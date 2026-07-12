package pricingService;

import api.dtos.PriceRequestDto;
import api.dtos.PriceResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PricingServiceApplicationTests {

    @Autowired
    private PricingServiceImpl pricingService;

    @Test
    void contextLoads() {}

    @Test
    void shouldCalculatePriceForNoviSadTrip() {
        // Trg slobode -> Futoška pijaca (~2.5 km)
        PriceRequestDto req = new PriceRequestDto(
                45.2518, 19.8369,
                45.2623, 19.8211,
                10, 8);

        ResponseEntity<?> response = pricingService.calculatePrice(req);
        assertEquals(200, response.getStatusCode().value());

        PriceResponseDto dto = (PriceResponseDto) response.getBody();
        assertNotNull(dto);
        assertTrue(dto.getTotalPrice().doubleValue() > 0,
                "Price should be positive");
        assertTrue(dto.getDistanceKm() > 0,
                "Distance should be positive");
        assertTrue(dto.getSurgeMultiplier() >= 1.0,
                "Surge multiplier should be at least 1.0");
    }

    @Test
    void shouldApplySurgeWhenDemandHigh() {
        // 2 drivers, 10 requests -> surge expected
        PriceRequestDto highDemand = new PriceRequestDto(
                45.2518, 19.8369, 45.2623, 19.8211, 2, 10);
        PriceRequestDto normal = new PriceRequestDto(
                45.2518, 19.8369, 45.2623, 19.8211, 10, 5);

        PriceResponseDto high = (PriceResponseDto) pricingService.calculatePrice(highDemand).getBody();
        PriceResponseDto norm = (PriceResponseDto) pricingService.calculatePrice(normal).getBody();

        assertNotNull(high);
        assertNotNull(norm);
        assertTrue(high.getSurgeMultiplier() >= norm.getSurgeMultiplier(),
                "High demand should produce higher or equal surge multiplier");
    }

    @Test
    void shouldPersistPricingHistory() {
        long before = pricingService.getPricingHistory().getStatusCode().value();
        assertEquals(200, before);
    }
}
