package rideService;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface RideRepository extends JpaRepository<RideModel, Long> {

    List<RideModel> findByRiderEmail(String riderEmail);

    List<RideModel> findByDriverEmail(String driverEmail);

    long countByStatus(String status);

    @Modifying
    @Transactional
    @Query("UPDATE RideModel r SET r.status = :status, r.driverEmail = :driverEmail WHERE r.id = :id")
    void assignDriver(Long id, String driverEmail, String status);

    @Modifying
    @Transactional
    @Query("UPDATE RideModel r SET r.status = :status, r.completedAt = :completedAt WHERE r.id = :id")
    void completeRide(Long id, String status, LocalDateTime completedAt);

    @Modifying
    @Transactional
    @Query("UPDATE RideModel r SET r.status = :status WHERE r.id = :id")
    void updateStatus(Long id, String status);
}
