package notificationService;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rideId;

    @Column(nullable = false)
    private String riderEmail;

    @Column
    private String driverEmail;

    @Column(nullable = false)
    private String eventType;

    @Column
    private BigDecimal price;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    public NotificationModel() {}

    public NotificationModel(Long rideId, String riderEmail, String driverEmail,
                              String eventType, BigDecimal price, String message) {
        this.rideId = rideId;
        this.riderEmail = riderEmail;
        this.driverEmail = driverEmail;
        this.eventType = eventType;
        this.price = price;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }
    public String getRiderEmail() { return riderEmail; }
    public void setRiderEmail(String riderEmail) { this.riderEmail = riderEmail; }
    public String getDriverEmail() { return driverEmail; }
    public void setDriverEmail(String driverEmail) { this.driverEmail = driverEmail; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
}
