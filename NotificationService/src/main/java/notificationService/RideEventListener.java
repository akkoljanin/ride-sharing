package notificationService;

import api.events.RideEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RideEventListener {

    @Autowired
    private NotificationRepository repo;

    @RabbitListener(queues = "ride.notification.queue")
    public void handleRideEvent(RideEvent event) {
        System.out.printf("[NOTIFICATION] Event received: %s | ride=%d | rider=%s%n",
                event.getEventType(), event.getRideId(), event.getRiderEmail());

        String message = buildMessage(event);

        NotificationModel notification = new NotificationModel(
                event.getRideId(),
                event.getRiderEmail(),
                event.getDriverEmail(),
                event.getEventType(),
                event.getPrice(),
                message
        );
        repo.save(notification);

        // In production this would call an email/SMS/push service.
        // For now we log to simulate the notification.
        System.out.println("[NOTIFICATION] Sent: " + message);
    }

    private String buildMessage(RideEvent event) {
        return switch (event.getEventType()) {
            case "RIDE_REQUESTED" ->
                String.format("Ride #%d requested. Price: %.2f RSD. Looking for a driver...",
                        event.getRideId(),
                        event.getPrice() != null ? event.getPrice().doubleValue() : 0.0);
            case "RIDE_ACCEPTED" ->
                String.format("Great news! Driver %s accepted your ride #%d.",
                        event.getDriverEmail(), event.getRideId());
            case "RIDE_COMPLETED" ->
                String.format("Ride #%d completed. Total charge: %.2f RSD. Thank you!",
                        event.getRideId(),
                        event.getPrice() != null ? event.getPrice().doubleValue() : 0.0);
            case "RIDE_CANCELLED" ->
                String.format("Ride #%d has been cancelled.", event.getRideId());
            default ->
                String.format("Ride #%d status update: %s", event.getRideId(), event.getEventType());
        };
    }
}
