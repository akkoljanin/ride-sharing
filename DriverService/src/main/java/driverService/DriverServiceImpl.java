package driverService;

import api.dtos.DriverDto;
import api.services.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import util.exception.DataIntegrityViolationException;
import util.exception.NoDataFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverRepository repo;

    @Override
    public ResponseEntity<?> getAllDrivers() {
        List<DriverModel> drivers = repo.findAll();
        if (drivers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No drivers registered.");
        }
        return ResponseEntity.ok(drivers.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<?> getDriverByEmail(@PathVariable String email) {
        Optional<DriverModel> driver = repo.findByEmail(email);
        if (driver.isPresent()) {
            return ResponseEntity.ok(toDto(driver.get()));
        }
        throw new NoDataFoundException("Driver not found: " + email);
    }

    @Override
    public ResponseEntity<?> getAvailableDrivers() {
        List<DriverModel> available = repo.findByAvailableTrue();
        if (available.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No available drivers.");
        }
        return ResponseEntity.ok(available.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<?> countAvailableDrivers() {
        long count = repo.countByAvailableTrue();
        return ResponseEntity.ok(count);
    }

    @Override
    public ResponseEntity<?> registerDriver(@RequestBody DriverDto dto) {
        if (repo.existsByEmail(dto.getEmail())) {
            throw new DataIntegrityViolationException("Driver already registered: " + dto.getEmail());
        }
        DriverModel saved = repo.save(toModel(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Override
    public ResponseEntity<?> updateAvailability(@PathVariable String email,
                                                @RequestParam boolean available) {
        if (!repo.existsByEmail(email)) {
            throw new NoDataFoundException("Driver not found: " + email);
        }
        repo.updateAvailability(email, available);
        return ResponseEntity.ok("Availability updated to " + available + " for driver: " + email);
    }

    @Override
    public ResponseEntity<?> updateLocation(@PathVariable String email,
                                            @RequestParam double lat,
                                            @RequestParam double lng) {
        if (!repo.existsByEmail(email)) {
            throw new NoDataFoundException("Driver not found: " + email);
        }
        repo.updateLocation(email, lat, lng);
        return ResponseEntity.ok("Location updated for driver: " + email);
    }

    @Override
    public ResponseEntity<?> deleteDriver(@PathVariable String email) {
        Optional<DriverModel> driver = repo.findByEmail(email);
        if (driver.isEmpty()) {
            throw new NoDataFoundException("Driver not found: " + email);
        }
        repo.delete(driver.get());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private DriverDto toDto(DriverModel m) {
        return new DriverDto(m.getEmail(), m.getName(), m.getVehiclePlate(),
                m.getVehicleModel(), m.isAvailable(), m.getLocationLat(), m.getLocationLng());
    }

    private DriverModel toModel(DriverDto d) {
        return new DriverModel(d.getEmail(), d.getName(), d.getVehiclePlate(),
                d.getVehicleModel(), d.isAvailable(), d.getLocationLat(), d.getLocationLng());
    }
}
