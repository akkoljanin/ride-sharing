package api.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Service
public interface NotificationService {

    @GetMapping("/notifications")
    ResponseEntity<?> getAllNotifications();

    @GetMapping("/notifications/rider/{email}")
    ResponseEntity<?> getNotificationsForRider(@PathVariable String email);

    @GetMapping("/notifications/driver/{email}")
    ResponseEntity<?> getNotificationsForDriver(@PathVariable String email);
}
