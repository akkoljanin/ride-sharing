@echo off
REM ============================================================
REM  Ride-Sharing – Kubernetes Deploy Script (Windows)
REM  Prerequisites:
REM    - Docker Desktop with Kubernetes enabled
REM    - kubectl configured (kubectl version should work)
REM    - Istio installed: istioctl install --set profile=demo -y
REM  Usage: deploy-k8s.bat
REM ============================================================

echo.
echo ========================================
echo  Ride-Sharing K8s Deploy
echo ========================================
echo.

REM Step 1 – Check kubectl
kubectl version --client >nul 2>&1
if errorlevel 1 (
    echo ERROR: kubectl not found. Enable Kubernetes in Docker Desktop.
    pause & exit /b 1
)
echo [1/7] kubectl OK

REM Step 2 – Build all Docker images
echo [2/7] Building Docker images...
call build.bat
if errorlevel 1 (
    echo ERROR: Build failed.
    pause & exit /b 1
)

REM Step 3 – Enable Istio injection for namespace
echo [3/7] Applying namespace + ConfigMap...
kubectl apply -f k8s\00-namespace-configmap.yaml

REM Step 4 – Deploy infrastructure
echo [4/7] Deploying RabbitMQ and NamingServer...
kubectl apply -f k8s\01-rabbitmq.yaml
kubectl apply -f k8s\02-naming-server.yaml

echo Waiting for NamingServer to be ready...
kubectl wait --for=condition=ready pod -l app=naming-server -n ride-sharing --timeout=120s

REM Step 5 – Deploy business microservices
echo [5/7] Deploying microservices...
kubectl apply -f k8s\03-users-service.yaml
kubectl apply -f k8s\04-driver-service.yaml
kubectl apply -f k8s\05-pricing-service.yaml
kubectl apply -f k8s\06-ride-service.yaml
kubectl apply -f k8s\07-notification-and-gateway.yaml
kubectl apply -f k8s\08-hpa.yaml

REM Step 6 – Apply Istio policies
echo [6/7] Applying Istio service mesh policies...
kubectl apply -f k8s\istio\01-gateway-virtualservices.yaml
kubectl apply -f k8s\istio\02-destination-rules.yaml
kubectl apply -f k8s\istio\03-mtls-authz.yaml

REM Step 7 – Deploy EFK stack
echo [7/7] Deploying EFK (Elasticsearch + Fluentd + Kibana)...
kubectl apply -f k8s\efk\01-elasticsearch.yaml
kubectl apply -f k8s\efk\02-fluentd.yaml
kubectl apply -f k8s\efk\03-kibana.yaml

echo.
echo ========================================
echo  Deploy complete!
echo ========================================
echo.
echo  Check status:     kubectl get pods -n ride-sharing
echo  API Gateway:      http://localhost:30765
echo  Kibana (logs):    http://localhost:30601
echo  Eureka:           kubectl port-forward svc/naming-server 8761:8761 -n ride-sharing
echo  Istio dashboard:  istioctl dashboard kiali
echo.
pause
