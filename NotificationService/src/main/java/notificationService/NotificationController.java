package notificationService;

import api.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController implements NotificationService {

    @Autowired
    private NotificationRepository repo;

    @Override
    public ResponseEntity<?> getAllNotifications() {
        return ResponseEntity.ok(repo.findTop50ByOrderByReceivedAtDesc());
    }

    @Override
    public ResponseEntity<?> getNotificationsForRider(@PathVariable String email) {
        var list = repo.findByRiderEmailOrderByReceivedAtDesc(email);
        if (list.isEmpty())
            return ResponseEntity.ok("No notifications for rider: " + email);
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<?> getNotificationsForDriver(@PathVariable String email) {
        var list = repo.findByDriverEmailOrderByReceivedAtDesc(email);
        if (list.isEmpty())
            return ResponseEntity.ok("No notifications for driver: " + email);
        return ResponseEntity.ok(list);
    }
}
