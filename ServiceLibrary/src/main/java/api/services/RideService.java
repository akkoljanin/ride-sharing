package api.services;

import api.dtos.RideDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
public interface RideService {
    @PostMapping("/rides/request")
    ResponseEntity<?> requestRide(@RequestBody RideDto dto,
                                  @RequestHeader("X-User-Email") String riderEmail);

    @PutMapping("/rides/{id}/accept")
    ResponseEntity<?> acceptRide(@PathVariable Long id,
                                 @RequestHeader("X-User-Email") String driverEmail);

    @PutMapping("/rides/{id}/complete")
    ResponseEntity<?> completeRide(@PathVariable Long id,
                                   @RequestHeader("X-User-Email") String driverEmail);

    @PutMapping("/rides/{id}/cancel")
    ResponseEntity<?> cancelRide(@PathVariable Long id,
                                 @RequestHeader("X-User-Email") String userEmail);

    @GetMapping("/rides/{id}")
    ResponseEntity<?> getRideById(@PathVariable Long id);

    @GetMapping("/rides/rider/{email}")
    ResponseEntity<?> getRidesByRider(@PathVariable String email);

    @GetMapping("/rides/driver/{email}")
    ResponseEntity<?> getRidesByDriver(@PathVariable String email);

    @GetMapping("/rides")
    ResponseEntity<?> getAllRides();
}
