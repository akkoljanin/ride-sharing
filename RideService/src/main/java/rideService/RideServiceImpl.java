package rideService;

import api.dtos.PriceRequestDto;
import api.dtos.PriceResponseDto;
import api.dtos.RideDto;
import api.events.RideEvent;
import api.services.RideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import util.exception.DriverNotAvailableException;
import util.exception.InvalidStatusTransitionException;
import util.exception.RideNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class RideServiceImpl implements RideService {

    private static final Logger log = LoggerFactory.getLogger(RideServiceImpl.class);

    @Autowired private RideRepository repo;
    @Autowired private FeignCallService feignCallService;  // CB-protected Feign calls
    @Autowired private RabbitTemplate rabbitTemplate;

    // ── REQUEST RIDE ──────────────────────────────────────────────────────
    @Override
    public ResponseEntity<?> requestRide(@RequestBody RideDto dto,
                                         @RequestHeader("X-User-Email") String riderEmail) {

        // Sync call to driver-service (with CB — returns -1 if circuit open)
        Integer available = feignCallService.countAvailableDrivers();
        if (available == null || available <= 0) {
            if (available != null && available == -1) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Driver service is temporarily unavailable. Please try again in a few minutes.");
            }
            throw new DriverNotAvailableException(
                    "No drivers available at this time. Please try again shortly.");
        }

        // Sync call to pricing-service (with CB — returns flat rate if circuit open)
        PriceRequestDto priceReq = new PriceRequestDto(
                dto.getPickupLat(), dto.getPickupLng(),
                dto.getDropoffLat(), dto.getDropoffLng(),
                available, (int) repo.countByStatus("REQUESTED") + 1);
        PriceResponseDto priceResp = feignCallService.calculatePrice(priceReq);

        // Persist ride
        RideModel ride = new RideModel();
        ride.setRiderEmail(riderEmail);
        ride.setPickupLat(dto.getPickupLat());
        ride.setPickupLng(dto.getPickupLng());
        ride.setDropoffLat(dto.getDropoffLat());
        ride.setDropoffLng(dto.getDropoffLng());
        ride.setStatus("REQUESTED");
        ride.setPrice(priceResp.getTotalPrice());
        ride.setSurgeMultiplier(priceResp.getSurgeMultiplier());
        RideModel saved = repo.save(ride);

        // Async event → notification-service
        publishEvent(saved.getId(), riderEmail, null, "RIDE_REQUESTED", saved.getPrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    // ── ACCEPT RIDE ───────────────────────────────────────────────────────
    @Override
    public ResponseEntity<?> acceptRide(@PathVariable Long id,
                                        @RequestHeader("X-User-Email") String driverEmail) {
        RideModel ride = findOrThrow(id);
        if (!"REQUESTED".equals(ride.getStatus()))
            throw new InvalidStatusTransitionException(
                    "Cannot accept ride in status: " + ride.getStatus());

        repo.assignDriver(id, driverEmail, "ACCEPTED");
        feignCallService.updateDriverAvailability(driverEmail, false);
        publishEvent(id, ride.getRiderEmail(), driverEmail, "RIDE_ACCEPTED", ride.getPrice());
        return ResponseEntity.ok("Ride " + id + " accepted by " + driverEmail);
    }

    // ── COMPLETE RIDE ─────────────────────────────────────────────────────
    @Override
    public ResponseEntity<?> completeRide(@PathVariable Long id,
                                          @RequestHeader("X-User-Email") String driverEmail) {
        RideModel ride = findOrThrow(id);
        if (!"ACCEPTED".equals(ride.getStatus()) && !"IN_PROGRESS".equals(ride.getStatus()))
            throw new InvalidStatusTransitionException(
                    "Cannot complete ride in status: " + ride.getStatus());

        repo.completeRide(id, "COMPLETED", LocalDateTime.now());
        feignCallService.updateDriverAvailability(driverEmail, true);
        publishEvent(id, ride.getRiderEmail(), driverEmail, "RIDE_COMPLETED", ride.getPrice());
        return ResponseEntity.ok("Ride " + id + " completed.");
    }

    // ── CANCEL RIDE ───────────────────────────────────────────────────────
    @Override
    public ResponseEntity<?> cancelRide(@PathVariable Long id,
                                        @RequestHeader("X-User-Email") String userEmail) {
        RideModel ride = findOrThrow(id);
        if ("COMPLETED".equals(ride.getStatus()) || "CANCELLED".equals(ride.getStatus()))
            throw new InvalidStatusTransitionException(
                    "Cannot cancel ride in status: " + ride.getStatus());

        repo.updateStatus(id, "CANCELLED");
        if (ride.getDriverEmail() != null)
            feignCallService.updateDriverAvailability(ride.getDriverEmail(), true);

        publishEvent(id, ride.getRiderEmail(), ride.getDriverEmail(),
                "RIDE_CANCELLED", ride.getPrice());
        return ResponseEntity.ok("Ride " + id + " cancelled.");
    }

    // ── QUERIES ───────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<?> getRideById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(findOrThrow(id)));
    }

    @Override
    public ResponseEntity<?> getRidesByRider(@PathVariable String email) {
        List<RideModel> rides = repo.findByRiderEmail(email);
        if (rides.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No rides for rider: " + email);
        return ResponseEntity.ok(rides.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<?> getRidesByDriver(@PathVariable String email) {
        List<RideModel> rides = repo.findByDriverEmail(email);
        if (rides.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No rides for driver: " + email);
        return ResponseEntity.ok(rides.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<?> getAllRides() {
        List<RideModel> all = repo.findAll();
        if (all.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No rides found.");
        return ResponseEntity.ok(all.stream().map(this::toDto).collect(Collectors.toList()));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private void publishEvent(Long rideId, String rider, String driver,
                               String type, BigDecimal price) {
        try {
            RideEvent event = new RideEvent(rideId, rider, driver, type, price);
            String routingKey = "ride." + type.split("_")[1].toLowerCase();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
            log.info("[RabbitMQ] Published {} for ride {}", type, rideId);
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish {}: {}", type, e.getMessage());
        }
    }

    private RideModel findOrThrow(Long id) {
        Optional<RideModel> opt = repo.findById(id);
        if (opt.isEmpty()) throw new RideNotFoundException("Ride not found: " + id);
        return opt.get();
    }

    private RideDto toDto(RideModel m) {
        RideDto d = new RideDto();
        d.setId(m.getId());
        d.setRiderEmail(m.getRiderEmail());
        d.setDriverEmail(m.getDriverEmail());
        d.setPickupLat(m.getPickupLat()); d.setPickupLng(m.getPickupLng());
        d.setDropoffLat(m.getDropoffLat()); d.setDropoffLng(m.getDropoffLng());
        d.setStatus(m.getStatus());
        d.setPrice(m.getPrice());
        d.setSurgeMultiplier(m.getSurgeMultiplier());
        d.setRequestedAt(m.getRequestedAt());
        d.setCompletedAt(m.getCompletedAt());
        return d;
    }
}
