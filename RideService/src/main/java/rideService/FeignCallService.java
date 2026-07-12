package rideService;

import api.dtos.PriceRequestDto;
import api.dtos.PriceResponseDto;
import api.proxies.DriverServiceProxy;
import api.proxies.PricingServiceProxy;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Separate Spring-managed component so that @CircuitBreaker AOP proxy works correctly.
 * Spring AOP cannot proxy calls within the same bean or on private methods —
 * by extracting Feign calls here, circuit breaker annotations work as expected.
 */
@Service
public class FeignCallService {

    private static final Logger log = LoggerFactory.getLogger(FeignCallService.class);

    @Autowired
    private DriverServiceProxy driverProxy;

    @Autowired
    private PricingServiceProxy pricingProxy;

    // ── Driver service call with circuit breaker ──────────────────────────
    @CircuitBreaker(name = "driverService", fallbackMethod = "countDriversFallback")
    public Integer countAvailableDrivers() {
        return driverProxy.countAvailableDrivers();
    }

    public Integer countDriversFallback(Throwable t) {
        log.warn("[CIRCUIT BREAKER] driverService OPEN — {}", t.getMessage());
        return -1; // sentinel: signals unavailability to caller
    }

    // ── Driver availability update with circuit breaker ───────────────────
    @CircuitBreaker(name = "driverService", fallbackMethod = "updateAvailabilityFallback")
    public void updateDriverAvailability(String email, boolean available) {
        driverProxy.updateAvailability(email, available);
    }

    public void updateAvailabilityFallback(String email, boolean available, Throwable t) {
        log.warn("[CIRCUIT BREAKER] driverService OPEN — cannot update availability for {}: {}",
                email, t.getMessage());
        // Non-critical: log and continue — ride state is already updated
    }

    // ── Pricing call with circuit breaker + flat-rate fallback ────────────
    @CircuitBreaker(name = "pricingService", fallbackMethod = "calculatePriceFallback")
    public PriceResponseDto calculatePrice(PriceRequestDto request) {
        return pricingProxy.calculatePrice(request);
    }

    public PriceResponseDto calculatePriceFallback(PriceRequestDto request, Throwable t) {
        log.warn("[CIRCUIT BREAKER] pricingService OPEN — applying flat rate 500 RSD. Reason: {}",
                t.getMessage());
        PriceResponseDto fallback = new PriceResponseDto();
        fallback.setTotalPrice(new BigDecimal("500.00"));
        fallback.setSurgeMultiplier(1.0);
        fallback.setDistanceKm(0);
        fallback.setEstimatedMinutes(15);
        fallback.setBaseFare(new BigDecimal("500.00"));
        fallback.setDistanceFare(BigDecimal.ZERO);
        fallback.setTimeFare(BigDecimal.ZERO);
        return fallback;
    }
}
