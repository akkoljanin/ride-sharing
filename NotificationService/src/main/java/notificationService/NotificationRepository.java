package notificationService;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationModel, Long> {
    List<NotificationModel> findByRiderEmailOrderByReceivedAtDesc(String riderEmail);
    List<NotificationModel> findByDriverEmailOrderByReceivedAtDesc(String driverEmail);
    List<NotificationModel> findTop50ByOrderByReceivedAtDesc();
}
