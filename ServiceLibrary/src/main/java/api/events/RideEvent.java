package api.events;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RideEvent implements Serializable {
    private Long rideId;
    private String riderEmail;
    private String driverEmail;
    private String eventType;
    private BigDecimal price;
    private LocalDateTime timestamp;

    public RideEvent() {}
    public RideEvent(Long rideId, String riderEmail, String driverEmail,
                     String eventType, BigDecimal price) {
        this.rideId = rideId;
        this.riderEmail = riderEmail;
        this.driverEmail = driverEmail;
        this.eventType = eventType;
        this.price = price;
        this.timestamp = LocalDateTime.now();
    }
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
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
