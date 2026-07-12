package notificationService;

import api.events.RideEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NotificationServiceApplicationTests {

    @Autowired
    private NotificationRepository repo;

    @Autowired
    private RideEventListener listener;

    @Test
    void contextLoads() {}

    @Test
    void shouldPersistNotificationOnEvent() {
        long before = repo.count();

        RideEvent event = new RideEvent(
                99L, "rider@test.rs", "driver@test.rs",
                "RIDE_COMPLETED", new BigDecimal("750.00"));
        listener.handleRideEvent(event);

        long after = repo.count();
        assertEquals(before + 1, after);

        var notifications = repo.findByRiderEmailOrderByReceivedAtDesc("rider@test.rs");
        assertFalse(notifications.isEmpty());
        assertEquals("RIDE_COMPLETED", notifications.get(0).getEventType());
    }

    @Test
    void shouldBuildCorrectMessageForEachEventType() {
        String[] types = {"RIDE_REQUESTED", "RIDE_ACCEPTED", "RIDE_COMPLETED", "RIDE_CANCELLED"};
        for (String type : types) {
            RideEvent ev = new RideEvent(1L, "r@t.rs", "d@t.rs", type, new BigDecimal("500"));
            listener.handleRideEvent(ev);
        }
        var all = repo.findByRiderEmailOrderByReceivedAtDesc("r@t.rs");
        assertEquals(4, all.size());
    }
}
