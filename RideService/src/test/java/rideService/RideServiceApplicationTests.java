package rideService;

import api.dtos.PriceResponseDto;
import api.dtos.RideDto;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@SpringBootTest
class RideServiceApplicationTests {

    @Autowired
    private RideRepository repo;

    @Autowired
    private RideServiceImpl rideService;

    // Mock the CB-protected feign call service
    @MockBean
    private FeignCallService feignCallService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {}

    @Test
    void shouldCreateRideWhenDriversAvailable() {
        PriceResponseDto mockPrice = new PriceResponseDto();
        mockPrice.setTotalPrice(new BigDecimal("850.00"));
        mockPrice.setSurgeMultiplier(1.0);
        mockPrice.setDistanceKm(5.2);
        mockPrice.setEstimatedMinutes(12);
        mockPrice.setBaseFare(new BigDecimal("150.00"));
        mockPrice.setDistanceFare(new BigDecimal("624.00"));
        mockPrice.setTimeFare(new BigDecimal("96.00"));

        when(feignCallService.countAvailableDrivers()).thenReturn(5);
        when(feignCallService.calculatePrice(any())).thenReturn(mockPrice);

        RideDto req = new RideDto();
        req.setPickupLat(45.2518); req.setPickupLng(19.8369);
        req.setDropoffLat(45.2671); req.setDropoffLng(19.8335);

        ResponseEntity<?> response = rideService.requestRide(req, "rider@test.rs");
        assertEquals(201, response.getStatusCode().value());

        RideDto created = (RideDto) response.getBody();
        assertNotNull(created);
        assertEquals("REQUESTED", created.getStatus());
        assertEquals("rider@test.rs", created.getRiderEmail());
        assertEquals(new BigDecimal("850.00"), created.getPrice());
    }

    @Test
    void shouldReturnServiceUnavailableWhenCircuitOpen() {
        // Sentinel -1 simulates circuit breaker open (fallback returns -1)
        when(feignCallService.countAvailableDrivers()).thenReturn(-1);

        RideDto req = new RideDto();
        req.setPickupLat(45.2518); req.setPickupLng(19.8369);
        req.setDropoffLat(45.2671); req.setDropoffLng(19.8335);

        ResponseEntity<?> response = rideService.requestRide(req, "rider@test.rs");
        assertEquals(503, response.getStatusCode().value());
    }

    @Test
    void shouldThrowWhenNoDriversAvailable() {
        when(feignCallService.countAvailableDrivers()).thenReturn(0);

        RideDto req = new RideDto();
        req.setPickupLat(45.2518); req.setPickupLng(19.8369);
        req.setDropoffLat(45.2671); req.setDropoffLng(19.8335);

        assertThrows(util.exception.DriverNotAvailableException.class,
                () -> rideService.requestRide(req, "rider@test.rs"));
    }

    @Test
    void shouldRejectInvalidStatusTransitionOnCancel() {
        RideModel completed = new RideModel();
        completed.setRiderEmail("r@test.rs");
        completed.setPickupLat(45.0); completed.setPickupLng(19.0);
        completed.setDropoffLat(45.1); completed.setDropoffLng(19.1);
        completed.setStatus("COMPLETED");
        RideModel saved = repo.save(completed);

        assertThrows(util.exception.InvalidStatusTransitionException.class,
                () -> rideService.cancelRide(saved.getId(), "r@test.rs"));
    }

    @Test
    void shouldUseFlatRateWhenPricingCircuitOpen() {
        // Normal driver count but pricing CB returns fallback (500 RSD)
        PriceResponseDto flatRate = new PriceResponseDto();
        flatRate.setTotalPrice(new BigDecimal("500.00"));
        flatRate.setSurgeMultiplier(1.0);
        flatRate.setDistanceKm(0);
        flatRate.setEstimatedMinutes(15);
        flatRate.setBaseFare(new BigDecimal("500.00"));
        flatRate.setDistanceFare(BigDecimal.ZERO);
        flatRate.setTimeFare(BigDecimal.ZERO);

        when(feignCallService.countAvailableDrivers()).thenReturn(3);
        when(feignCallService.calculatePrice(any())).thenReturn(flatRate);

        RideDto req = new RideDto();
        req.setPickupLat(45.2518); req.setPickupLng(19.8369);
        req.setDropoffLat(45.2671); req.setDropoffLng(19.8335);

        ResponseEntity<?> response = rideService.requestRide(req, "rider2@test.rs");
        assertEquals(201, response.getStatusCode().value());

        RideDto created = (RideDto) response.getBody();
        assertNotNull(created);
        assertEquals(new BigDecimal("500.00"), created.getPrice());
    }
}
