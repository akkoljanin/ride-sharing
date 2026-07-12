package api.services;

import api.dtos.DriverDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
public interface DriverService {
    @GetMapping("/drivers")
    ResponseEntity<?> getAllDrivers();

    @GetMapping("/drivers/{email}")
    ResponseEntity<?> getDriverByEmail(@PathVariable String email);

    @GetMapping("/drivers/available")
    ResponseEntity<?> getAvailableDrivers();

    @GetMapping("/drivers/available/count")
    ResponseEntity<?> countAvailableDrivers();

    @PostMapping("/drivers")
    ResponseEntity<?> registerDriver(@RequestBody DriverDto dto);

    @PutMapping("/drivers/{email}/availability")
    ResponseEntity<?> updateAvailability(@PathVariable String email,
                                         @RequestParam boolean available);

    @PutMapping("/drivers/{email}/location")
    ResponseEntity<?> updateLocation(@PathVariable String email,
                                     @RequestParam double lat,
                                     @RequestParam double lng);

    @DeleteMapping("/drivers/{email}")
    ResponseEntity<?> deleteDriver(@PathVariable String email);
}
