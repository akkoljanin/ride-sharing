# Ride-Sharing Mikroservisni Sistem

<div >

[![CI/CD](https://github.com/akkoljanin/ride-sharing/actions/workflows/ci-cd.yml/badge.svg?branch=main)](https://github.com/akkoljanin/ride-sharing/actions/workflows/ci-cd.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-messaging-orange)
![Docker](https://img.shields.io/badge/Docker-compose-blue)
![Kubernetes](https://img.shields.io/badge/Kubernetes-manifests-326ce5)
![Istio](https://img.shields.io/badge/Istio-service%20mesh-466bb0)

</div>

## 1. Opis poslovne logike

Sistem simulira platformu za dijeljenje vožnji tipa Uber, namijenjenu gradu Novi Sad. Putnici (RIDER) zahtijevaju vožnje putem REST API-ja, vozači (DRIVER) ih prihvataju i izvršavaju, a sve aktivnosti se prate kroz event-driven arhitekturu.

| Akter | Uloga |
|-------|-------|
| **RIDER** | Putnik koji zahtijeva vožnju, prati status i prima notifikacije |
| **DRIVER** | Vozač koji prihvata zahtjeve, izvršava i završava vožnje |
| **ADMIN** | Administrator sa punim pristupom svim podacima |

### Tok vožnje 

Kada RIDER pošalje zahtjev za vožnju (GPS koordinate polazišta i odredišta), `ride-service` preuzima ulogu orkestratora poslovnog procesa i sinhrono komunicira sa dva druga servisa prije nego što sačuva vožnju. Prvo, putem deklarativnog REST poziva (OpenFeign) upućenog ka `driver-service`, provjerava se broj trenutno dostupnih vozača. Potom se, na isti način, poziva `pricing-service` radi izračunavanja cijene vožnje, primjenom Haversine formule za udaljenost i modela surge pricing-a za dinamičko prilagođavanje cijene trenutnoj potražnji. Tek nakon što su oba odgovora pristigla, vožnja se čuva u bazi podataka sa statusom `REQUESTED`, a `ride-service` asinhrono objavljuje događaj na RabbitMQ, koji `notification-service` preuzima i na osnovu njega obavještava putnika.

Prihvatanjem vožnje, status prelazi u `ACCEPTED`, a vozač postaje nedostupan, završetkom vožnje status prelazi u `COMPLETED`, a vozač ponovo postaje dostupan, dok otkazivanje vožnju prevodi u status `CANCELLED` i vozača, ukoliko je već bio dodijeljen, oslobađa. Svaka od ovih tranzicija emituje odgovarajući događaj (`RIDE_ACCEPTED`, `RIDE_COMPLETED`, `RIDE_CANCELLED`) koji se, na isti asinhroni način, prosljeđuje `notification-service`-u.

### Dijagram stanja vožnje

> Vožnja može preći u `CANCELLED` iz stanja `REQUESTED` ili `ACCEPTED`. Završena vožnja (`COMPLETED`) ne može biti otkazana - sistem vraća `400 Bad Request`.

<img width="361" height="421" alt="tranzicije drawio" src="https://github.com/user-attachments/assets/ade367b9-39c8-4703-a447-0480c4cc99dc" />


### Surge pricing formula

```
cijena = (150 + 120 × km + 8 × minuta) × surge_multiplikator
```

| Ratio (zahtjevi / vozači) | Surge multiplikator |
|--------------------------|---------------------|
| < 1.2 | 1.0× (normalno) |
| 1.2 - 2.0 | 1.0× - 2.0× |
| 2.0 - 3.0 | 2.0× - 2.5× |
| > 3.0 | 3.0× (maksimum 4.0×) |

**Doba dana:** Jutro 7-9h × 1.3 · Veče 17-19h × 1.4 · Noć × 1.2  
**Distanca:** Računa se Haversine formulom na osnovu GPS koordinata.

## 2. Dijagram mikroservisnog sistema

Sa aspekta odgovornosti, sistem se može posmatrati kroz tri logička sloja:

- **Ulazni sloj.** `api-gateway` (port 8765) predstavlja jedinu tačku ulaska (*single entry point*) za sve klijentske zahtjeve, čime se interna topologija sistema sakriva od klijenta. Gateway vrši autentifikaciju korisnika putem HTTP Basic Auth mehanizma (Spring Security), a nakon uspješne autentifikacije obavlja propagaciju identiteta kroz HTTP zaglavlja `X-User-Email` i `X-User-Role` ka downstream servisima. Na taj način se odgovornost za autentifikaciju centralizuje na jednom mjestu, dok poslovni servisi vrše isključivo autorizaciju na osnovu proslijeđene role, bez potrebe da sami implementiraju logiku provjere kredencijala.

- **Poslovni sloj.** Poslovnu logiku sistema realizuje pet autonomnih mikroservisa - `users-service` (8770), `driver-service` (8771), `pricing-service` (8772), `ride-service` (8773) i `notification-service` (8774), od kojih svaki enkapsulira jedan jasno odvojen poslovni podskup (upravljanje korisnicima, upravljanje vozačima, izračunavanje cijene, upravljanje životnim ciklusom vožnje, odnosno obavještavanje korisnika). `ride-service` sinhrono komunicira sa `driver-service` i `pricing-service` servisima putem deklarativnog REST klijenta (OpenFeign), obavijenog Circuit Breaker mehanizmom (Resilience4j) radi otpornosti na kvarove, dok istovremeno asinhrono, putem message brokera RabbitMQ (topic exchange `ride.events`, red `ride.notification.queue`), obavještava `notification-service` o događajima u toku vožnje. 

- **Infrastrukturni i monitoring sloj.** `naming-server` (Eureka, port 8761) implementira *Service Discovery*, gde se svaki servis  pri pokretanju dinamički registruje kod njega, čime se izbjegava hardkodovanje mrežnih adresa i omogućava horizontalno skaliranje (dodavanje novih instanci servisa bez izmjene konfiguracije ostalih servisa). Par `Prometheus` (9090) i `Grafana` (3000) čini sloj za posmatranje sistema (*observability*): Prometheus periodično prikuplja (*scrape*) metrike sa `/actuator/prometheus` endpointa svakog servisa (broj zahtjeva, stopa grešaka, stanje Circuit Breaker-a, potrošnja memorije), dok ih Grafana vizualizuje kroz dashboard-e i koristi za generisanje alarma prilikom anomalija.

Dijagram u nastavku vizuelno prikazuje opisane veze između komponenti: puna linija označava direktan sinhroni poziv ili slanje asinhrone poruke, dok isprekidana linija označava registraciju servisa na Eureku ili slanje metrika ka Prometheus-u.

<img width="908" height="661" alt="dijagram1 drawio" src="https://github.com/user-attachments/assets/92e25ed5-b38a-49c8-b779-1cb1633d9cf3" />


## 3. Pipeline - Build / Test / Deploy

### Preduslovi

| Alat | Verzija | Provjera |
|------|---------|----------|
| Java JDK | **21** (Temurin) | `java -version` |
| Docker Desktop | 24+ | `docker --version` |
| Docker Compose | v2+ | `docker compose version` |
| Maven Wrapper | uključen u projektu | `mvnw.cmd -v` |

### 3.1 Pipeline - Razvoj (lokalno)

**Korak 1: Kloniranje**
```bat
git clone https://github.com/<username>/ride-sharing.git
cd ride-sharing
```

**Korak 2: Build svih servisa**
```bat
build.bat
```

### 3.2 Pipeline - Testiranje

**Svi testovi odjednom:**
```bat
cd C:\ride-sharing
test.bat
```
**Što testovi pokrivaju:**

| Servis | Tip | Šta se testira |
|--------|-----|----------------|
| `UsersService` | Integracioni (H2) | Seed korisnici učitani, findByEmail, role |
| `DriverService` | Integracioni (H2) | Seed vozači, countAvailable, findByEmail |
| `PricingService` | Integracioni (H2) | Kalkulacija cijene, surge logika, persist |
| `NotificationService` | Integracioni (H2) | RabbitMQ listener, notifikacija u bazi |
| `RideService` | Unit (@MockBean) | Kreiranje vožnje, CB fallback, status tranzicije |

### 3.3 Pipeline - Produkcija (Docker)

**Pokretanje cijelog sistema:**
```bat
cd C:\ride-sharing
docker-compose up
```
**Provjera statusa:**
```bat
docker-compose ps
```
**Logovi:**
```bat
REM logovi jednog servisa
docker-compose logs -f ride-service
docker-compose logs -f notification-service

REM Provjeri jesu li RabbitMQ eventi stigli
docker-compose logs notification-service | findstr "NOTIFICATION"
```
## 4. Komunikacioni paterni

| Tip | Od - Ka | Kada | Zašto |
|-----|---------|------|-------|
| **REST Sync (Feign + CB)** | ride - driver | Zahtjev za vožnju | Rider čeka broj vozača odmah |
| **REST Sync (Feign + CB)** | ride - pricing | Zahtjev za vožnju | Rider čeka cijenu odmah |
| **REST Sync (Feign + CB)** | ride - driver | Accept/Complete/Cancel | Ažuriranje dostupnosti vozača |
| **Async (RabbitMQ)** | ride - notification | Kreiranje, prihvatanje, završetak, otkazivanje | Notifikacija ne blokira poslovnu logiku |

**Zašto pricing sinhrono?** Rider mora dobiti cijenu prije potvrde vožnje.  
**Zašto notification asinhrono?** Slanje notifikacije ne smije blokirati kreiranje vožnje. Ako notification-service padne, poruke čekaju u RabbitMQ redu.

## 5. Mikroservisi i portovi

| Servis | Port | Tip | Opis |
|--------|------|-----|------|
| `naming-server` | 8761 | Infrastruktura | Eureka Service Discovery |
| `api-gateway` | 8765 | Infrastruktura | Spring Cloud Gateway + Spring Security |
| `users-service` | 8770 | **Poslovna logika** | Registracija, autentifikacija, role |
| `driver-service` | 8771 | **Poslovna logika** | Vozači, dostupnost, GPS lokacija |
| `pricing-service` | 8772 | **Poslovna logika** | Surge pricing, Haversine, kalkulacijska istorija |
| `ride-service` | 8773 | **Poslovna logika** | Životni ciklus vožnje, Feign publisher, CB |
| `notification-service` | 8774 | **Poslovna logika** | RabbitMQ consumer, log eventa |
| `rabbitmq` | 5672 / 15672 | Infrastruktura | Message broker |
| `prometheus` | 9090 | Monitoring | Scraping metrika svakih 15s |
| `grafana` | 3000 | Monitoring | Dashboardi i alarmi |

**Dijeljene biblioteke (ne deployuju se kao servisi):**

| Modul | Opis |
|-------|------|
| `ServiceLibrary` | DTOs, Feign proxy interfejsi, RideEvent |
| `Util` | GlobalExceptionHandler, custom exception klase |

## 6. Testovi

### Pokretanje
```bat
cd C:\ride-sharing
test.bat
```
### Opis testova

**Integracioni testovi** startuju cijeli Spring kontekst sa H2 bazom i testiraju stvarno ponašanje:

| Servis | Test | Šta se provjerava |
|--------|------|-------------------|
| `DriverService` | `shouldLoadInitialDrivers` | 3 seed vozača učitana |
| `DriverService` | `shouldFindAvailableDrivers` | countAvailableTrue >= 1 |
| `DriverService` | `shouldFindDriverByEmail` | findByEmail vraća ispravnog vozača |
| `PricingService` | `shouldCalculatePriceForNoviSadTrip` | Trg slobode - Futoška, cijena > 0 |
| `PricingService` | `shouldApplySurgeWhenDemandHigh` | 2 vozača / 10 zahtjeva - viši surge |
| `PricingService` | `shouldPersistPricingHistory` | Kalkulacija sačuvana u bazi |

**Unit testovi** koriste `@MockBean` - Feign i RabbitMQ su zamijenjeni mockovima:

| Test | Scenario | Očekivano |
|------|----------|-----------|
| `shouldCreateRideWhenDriversAvailable` | Normalan tok | `201 Created`, status REQUESTED |
| `shouldReturnServiceUnavailableWhenCircuitOpen` | CB otvoren  | `503 Service Unavailable` |
| `shouldThrowWhenNoDriversAvailable` | 0 vozača | `DriverNotAvailableException` |
| `shouldRejectInvalidStatusTransitionOnCancel` | Otkazivanje COMPLETED vožnje | `InvalidStatusTransitionException` |
| `shouldUseFlatRateWhenPricingCircuitOpen` | Pricing CB otvoren | `201 Created`, cijena = 500 RSD |

## 7. Monitoring - Prometheus i Grafana

### Pristup

| Alat | URL | Kredencijali |
|------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin123 |
| Prometheus | http://localhost:9090 | bez logina |
| RabbitMQ UI | http://localhost:15672 | guest / guest |
| Eureka | http://localhost:8761 | bez logina |

### Podešavanje mail alarma
Za kreiranje passworda otvoriti link: https://myaccount.google.com/apppasswords i ukucati Grafana.
1. Otvori `monitoring/grafana/grafana.ini` i potrebno je uneti mejl i generisanu lozinku:
```ini
[smtp]
enabled = true
host = smtp.gmail.com:587
user = tvoj-email@gmail.com -UNETI EMAIL
password = tvoj-app-password -UNETI GENERISANI PASSWORD
from_address = tvoj-email@gmail.com
from_name = Ride-Sharing Monitoring

[unified_alerting]
enabled = true
```

2. Otvori `monitoring/grafana/provisioning/alerting/contact-points.yml` i zamijeni adresu:
```yaml
addresses: tvoj-email@gmail.com
```

3. Restartuj Grafanu:
```bat
docker-compose restart grafana
```

**Alarm se aktivira kada:**
- Circuit Breaker pređe u `OPEN` stanje
- 5xx error rate pređe 10% u 2 minute
- Bilo koji servis prestane odgovarati Prometheusu

<img width="1133" height="846" alt="Screenshot (782)" src="https://github.com/user-attachments/assets/e0191cd5-8d82-4b5d-9d84-580fd26572b6" />

## 8. Circuit Breaker - Resilience4j

### Stanja i prijelazi

```
CLOSED (normalan rad)
   │  greška >= 50% od 5 poziva
   ▼
OPEN (zahtjevi ne prolaze → fallback odgovor)
   │  nakon 10 sekundi
   ▼
HALF_OPEN (test: 2 probna poziva)
   │  oba uspiju     │  bar jedan ne uspije
   ▼                 ▼
CLOSED            OPEN
```

### CB instance u projektu

| Instanca | Štiti poziv | Fallback ponašanje |
|----------|------------|-------------------|
| `driverService` | `countAvailableDrivers()` | Vraća `-1` - HTTP 503 |
| `driverService` | `updateAvailability()` | Log warning, nastavi |
| `pricingService` | `calculatePrice()` | Flat rate **500 RSD** |

## 9. Kubernetes + Istio + EFK

### Preduslovi za K8s deployment

```bat
REM Provjeri kubectl
kubectl version --client

REM Provjeri da je K8s uključen u Docker Desktop
REM Settings → Kubernetes → Enable Kubernetes → Apply & Restart

REM Instaliraj Istio 
istioctl install --set profile=demo -y
```

### Deploy svega

```bat
cd C:\ride-sharing
deploy-k8s.bat
```

Skripta automatski:
1. Kreira `ride-sharing` namespace sa Istio injection labelom
2. Primjenjuje ConfigMap sa svim environment varijablama
3. Deploy RabbitMQ (StatefulSet) i NamingServer, čeka healthcheck
4. Deploy svih 5 business mikroservisa + APIGateway
5. Primjenjuje HorizontalPodAutoscaler (ride-service: 1-5 replika, trigger: CPU 70%)
6. Primjenjuje Istio Gateway, VirtualServices, DestinationRules
7. Primjenjuje mTLS (STRICT) i AuthorizationPolicy
8. Deploy EFK stack (Elasticsearch StatefulSet, Fluentd DaemonSet, Kibana)

### Pristup servisima na K8s

| Servis | Pristup |
|--------|---------|
| API Gateway | `http://localhost:30765` |
| Kibana (EFK logovi) | `http://localhost:30601` |
| Eureka | `kubectl port-forward svc/naming-server 8761:8761 -n ride-sharing` |
| Istio Kiali | `istioctl dashboard kiali` |
| Grafana na K8s | `kubectl port-forward svc/grafana 3000:3000 -n ride-sharing` |

### Testiranje Istio mTLS

```bat
REM Provjeri da je mTLS aktivan
kubectl get peerauthentication -n ride-sharing

REM Provjeri VirtualServices
kubectl get virtualservices -n ride-sharing

REM Provjeri DestinationRules
kubectl get destinationrules -n ride-sharing
```

### Testiranje EFK logova u Kibani

1. Otvori `http://localhost:30601`
2. **Management - Stack Management - Index Patterns**
3. Kreiraj index pattern: `ride-sharing-*`
4. Idi na **Discover**
5. Filtriraj logove:
   - Po servisu: `service_name: ride-service`
   - Po nivou: `level: ERROR`
   - Po poruci: `message: *RabbitMQ*`

### Testiranje HPA (autoskaliranja)

```bat
REM Prati HPA u realnom vremenu
kubectl get hpa ride-service-hpa -n ride-sharing -w

REM Simuliraj opterećenje 
kubectl run -i --tty load-generator --rm --image=busybox --restart=Never -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://ride-service:8773/actuator/health; done"

REM Obriši load generator
kubectl delete pod load-generator -n ride-sharing
```

### Deinstalacija K8s deploymaneta

```bat
kubectl delete namespace ride-sharing
```

## 10. CI/CD – GitHub Actions

<img width="1459" height="457" alt="Screenshot (801)" src="https://github.com/user-attachments/assets/005f9378-668d-4e69-b8e3-258ed385c3e4" />

### Podešavanje GitHub Secrets

```
GitHub repo - Settings - Secrets and variables - Actions - New repository secret
```

| Secret | Vrijednost |
|--------|-----------|
| `DOCKER_USERNAME` | Docker Hub korisničko ime |
| `DOCKER_PASSWORD` | Docker Hub access token |
| `KUBE_CONFIG` | base64 sadržaj `~/.kube/config` |

**Generisanje KUBE_CONFIG:**
```bat
REM Windows PowerShell
[Convert]::ToBase64String([System.IO.File]::ReadAllBytes("$env:USERPROFILE\.kube\config"))
```

## 11. Testiranje u Postmanu 

## Postman kolekcija

Kolekcija sa svih 20 API testova nalazi se u fajlu `postman.postman-collection.json`
u korenu projekta. 

### TEST 1 - Registracija RIDER korisnika

```http
POST {{baseUrl}}/users/register
Content-Type: application/json

{
  "email": "ana@test.rs",
  "password": "pass123",
  "role": "RIDER",
  "phone": "0641111111"
}
```
**Očekivano:** `201 Created`

### TEST 2 - Registracija DRIVER korisnika

```http
POST {{baseUrl}}/users/register
Content-Type: application/json

{
  "email": "petar@test.rs",
  "password": "pass123",
  "role": "DRIVER",
  "phone": "0652222222"
}
```
**Očekivano:** `201 Created`

### TEST 3 - Registracija vozača u driver-service

```http
POST {{baseUrl}}/drivers
Authorization: Basic petar@test.rs:pass123
Content-Type: application/json

{
  "email": "petar@test.rs",
  "name": "Petar Nikolic",
  "vehiclePlate": "NS-999-XX",
  "vehicleModel": "Golf 7",
  "available": true,
  "locationLat": 45.2671,
  "locationLng": 19.8335
}
```
**Očekivano:** `201 Created`

### TEST 4 - Prikaži sve korisnike (ADMIN)

```http
GET {{baseUrl}}/users
Authorization: Basic admin@ridesharing.rs:admin123
```
**Očekivano:** `200 OK` – lista svih korisnika sa seedovanim podacima

### TEST 5 - Provjeri dostupne vozače

```http
GET {{baseUrl}}/drivers/available
Authorization: Basic ana@test.rs:pass123
```
**Očekivano:** `200 OK` – lista vozača (Petar + seed vozači driver1 i driver2)

### TEST 6 - Kalkulacija cijene (direktno)

```http
POST {{baseUrl}}/pricing/calculate
Authorization: Basic ana@test.rs:pass123
Content-Type: application/json

{
  "pickupLat": 45.2518,
  "pickupLng": 19.8369,
  "dropoffLat": 45.2671,
  "dropoffLng": 19.8335,
  "activeDrivers": 5,
  "activeRequests": 3
}
```
**Očekivano:** `200 OK` - JSON sa `totalPrice`, `distanceKm`, `surgeMultiplier`, `estimatedMinutes`

### TEST 7 - Zahtjev za vožnju 

```http
POST {{baseUrl}}/rides/request
Authorization: Basic ana@test.rs:pass123
Content-Type: application/json

{
  "pickupLat": 45.2518,
  "pickupLng": 19.8369,
  "dropoffLat": 45.2671,
  "dropoffLng": 19.8335
}
```
**Očekivano:** `201 Created`

```json
{
  "id": 1,
  "riderEmail": "ana@test.rs",
  "status": "REQUESTED",
  "price": 850.00,
  "surgeMultiplier": 1.0,
  "requestedAt": "2026-07-08T..."
}
```

**Provera Docker logova:**
Kada ride-service sačuva novu vožnju u bazu, ne čeka da neko obradi obavještenje, već samo objavi događaj RIDE_REQUESTED na RabbitMQ i odmah vrati odgovor klijentu, a notification-service sluša taj event nezavisno (preko @RabbitListener), preuzima ga kad stigne, čuva notifikaciju u svoju bazu i prosleđuje je korisniku. 
```
docker-compose logs -f ride-service notification-service

```
<img width="1464" height="294" alt="dockerlog" src="https://github.com/user-attachments/assets/9e846f7d-8a52-4994-b346-cd4132388891" />

### TEST 8 - Prihvati vožnju (DRIVER)

```http
PUT {{baseUrl}}/rides/1/accept
Authorization: Basic petar@test.rs:pass123
```
**Očekivano:** `200 OK` – "Ride 1 accepted by petar@test.rs"

### TEST 9 - Provjeri da je vozač nedostupan

```http
GET {{baseUrl}}/drivers/petar@test.rs
Authorization: Basic ana@test.rs:pass123
```
**Očekivano:** `200 OK` – polje `"available": false`

---

### TEST 10 - Završi vožnju (DRIVER)

```http
PUT {{baseUrl}}/rides/1/complete
Authorization: Basic petar@test.rs:pass123
```
**Očekivano:** `200 OK` – "Ride 1 completed."

### TEST 11 - Provjeri da je vozač ponovo dostupan

```http
GET {{baseUrl}}/drivers/available
Authorization: Basic ana@test.rs:pass123
```
**Očekivano:** Petar ponovo u listi dostupnih

### TEST 12 - Provjeri notifikacije ridera

```http
GET {{baseUrl}}/notifications/rider/ana@test.rs
Authorization: Basic ana@test.rs:pass123
```
**Očekivano:** `200 OK` – lista sa 3 notifikacije:
1. `RIDE_REQUESTED`
2. `RIDE_ACCEPTED`
3. `RIDE_COMPLETED`

### TEST 13 - Pricing istorija

```http
GET {{baseUrl}}/pricing/history
Authorization: Basic ana@test.rs:pass123
```
**Očekivano:** `200 OK` – zadnjih 20 kalkulacija

### TEST 14 - Prikaži sve vožnje ridera

```http
GET {{baseUrl}}/rides/rider/ana@test.rs
Authorization: Basic ana@test.rs:pass123
```
**Očekivano:** `200 OK` – lista vožnji

### TEST 15 - Test nevaljane tranzicije (ERROR scenario)

```http
PUT {{baseUrl}}/rides/1/cancel
Authorization: Basic ana@test.rs:pass123
```
**Očekivano:** `400 Bad Request`
```json
{
  "message": "Cannot cancel ride in status: COMPLETED",
  "status": 400
}
```

### TEST 16 - Kreiranje nove vožnje 

```http
POST {{baseUrl}}/rides/request
Authorization: Basic ana@test.rs:pass123
Content-Type: application/json

{
  "pickupLat": 45.2518,
  "pickupLng": 19.8369,
  "dropoffLat": 45.2671,
  "dropoffLng": 19.8335
}
```
### TEST 17 - Otkazivanje vožnje

```http
PUT {{baseUrl}}/rides/2/cancel
Authorization: Basic ana@test.rs:pass123
```
 **Očekivano:** `200 OK` - "Ride 2 cancelled."
 
### TEST 18 - Circuit Breaker test

**Korak 1:** U Docker Desktop zaustavi `driver-service`

**Korak 2:** Pošalji zahtjev za vožnju (TEST 7):
```http
POST {{baseUrl}}/rides/request
Authorization: Basic ana@test.rs:pass123
Content-Type: application/json

{
  "pickupLat": 45.2518,
  "pickupLng": 19.8369,
  "dropoffLat": 45.2671,
  "dropoffLng": 19.8335
}
```
 **Očekivano:** `503 Service Unav<img width="1920" height="1080" alt="Screenshot (782)" src="https://github.com/user-attachments/assets/fc0a98f2-b89c-4817-85e6-07c24c5e45b7" />
ailable` - "Driver service is temporarily unavailable. Please try again in a few minutes."

**Korak 3:** Vrati `driver-service` u Docker Desktop-u.

### TEST 19 - Neovlašteni pristup (401)

```http
GET {{baseUrl}}/users
```
(bez Authorization headera)

**Očekivano:** `401 Unauthorized`

### TEST 20 - Admin-only endpoint bez ADMIN role (403)

```http
GET {{baseUrl}}/users
Authorization: Basic ana@test.rs:pass123
```
**Očekivano:** `403 Forbidden` (ana je RIDER, ne ADMIN)

## 12. Seed podaci

Odmah dostupni nakon `docker-compose up`, bez registracije:

### Korisnici (users-service)

| Email | Lozinka | Uloga |
|-------|---------|-------|
| `admin@ridesharing.rs` | `admin123` | ADMIN |
| `marko@example.com` | `pass123` | RIDER |
| `jovana@example.com` | `pass123` | RIDER |
| `milan@example.com` | `pass123` | DRIVER |

### Vozači (driver-service)

| Email | Ime | Vozilo | Registracija | Dostupan |
|-------|-----|--------|-------------|----------|
| `driver1@ridesharing.rs` | Marko Petrović | VW Golf 7 | NS-123-AB | Da |
| `driver2@ridesharing.rs` | Ana Jovanović | Škoda Octavia | NS-456-CD |  Da |
| `driver3@ridesharing.rs` | Nikola Đorđević | Toyota Corolla | BG-789-EF |  Ne |
