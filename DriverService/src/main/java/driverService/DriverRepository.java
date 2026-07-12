package driverService;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<DriverModel, Long> {

    Optional<DriverModel> findByEmail(String email);

    boolean existsByEmail(String email);

    List<DriverModel> findByAvailableTrue();

    long countByAvailableTrue();

    @Modifying
    @Transactional
    @Query("UPDATE DriverModel d SET d.available = :available WHERE d.email = :email")
    void updateAvailability(String email, boolean available);

    @Modifying
    @Transactional
    @Query("UPDATE DriverModel d SET d.locationLat = :lat, d.locationLng = :lng WHERE d.email = :email")
    void updateLocation(String email, double lat, double lng);
}
