package pricingService;

import api.dtos.PriceRequestDto;
import api.dtos.PriceResponseDto;
import api.services.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@RestController
public class PricingServiceImpl implements PricingService {

    private static final BigDecimal BASE_FARE       = new BigDecimal("150.00");
    private static final BigDecimal RATE_PER_KM     = new BigDecimal("120.00");
    private static final BigDecimal RATE_PER_MINUTE = new BigDecimal("8.00");
    private static final double AVG_SPEED_KMH       = 35.0;

    @Autowired
    private PricingRepository repo;

    @Override
    public ResponseEntity<?> calculatePrice(@RequestBody PriceRequestDto request) {
        double distanceKm = haversineDistance(
                request.getPickupLat(), request.getPickupLng(),
                request.getDropoffLat(), request.getDropoffLng());

        int estimatedMinutes = (int) Math.ceil((distanceKm / AVG_SPEED_KMH) * 60);

        double surgeMultiplier = calculateSurge(
                request.getActiveDrivers(),
                request.getActiveRequests());

        BigDecimal surge = BigDecimal.valueOf(surgeMultiplier);
        BigDecimal distFare = RATE_PER_KM
                .multiply(BigDecimal.valueOf(distanceKm))
                .multiply(surge)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal timeFare = RATE_PER_MINUTE
                .multiply(BigDecimal.valueOf(estimatedMinutes))
                .multiply(surge)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = BASE_FARE.add(distFare).add(timeFare)
                .setScale(2, RoundingMode.HALF_UP);

        // persist for history
        PricingModel record = new PricingModel();
        record.setPickupLat(request.getPickupLat());
        record.setPickupLng(request.getPickupLng());
        record.setDropoffLat(request.getDropoffLat());
        record.setDropoffLng(request.getDropoffLng());
        record.setDistanceKm(Math.round(distanceKm * 100.0) / 100.0);
        record.setEstimatedMinutes(estimatedMinutes);
        record.setSurgeMultiplier(surgeMultiplier);
        record.setBaseFare(BASE_FARE);
        record.setDistanceFare(distFare);
        record.setTimeFare(timeFare);
        record.setTotalPrice(total);
        repo.save(record);

        PriceResponseDto response = new PriceResponseDto();
        response.setTotalPrice(total);
        response.setDistanceKm(Math.round(distanceKm * 100.0) / 100.0);
        response.setEstimatedMinutes(estimatedMinutes);
        response.setSurgeMultiplier(surgeMultiplier);
        response.setBaseFare(BASE_FARE);
        response.setDistanceFare(distFare);
        response.setTimeFare(timeFare);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> getPricingHistory() {
        return ResponseEntity.ok(repo.findTop20ByOrderByCalculatedAtDesc());
    }

    // Haversine formula – straight-line distance between two GPS coordinates
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Demand/supply ratio + time-of-day multiplier
    private double calculateSurge(int drivers, int requests) {
        double ratio = (drivers == 0) ? 4.0 : (double) requests / drivers;

        double surge;
        if (ratio > 3.0)      surge = 3.0;
        else if (ratio > 2.0) surge = 2.0 + (ratio - 2.0) * 0.5;
        else if (ratio > 1.5) surge = 1.5 + (ratio - 1.5);
        else if (ratio > 1.2) surge = 1.0 + (ratio - 1.2) * 1.5;
        else                  surge = 1.0;

        // time-of-day adjustment
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(7, 0)) && now.isBefore(LocalTime.of(9, 0)))   surge *= 1.3;
        else if (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(19, 0))) surge *= 1.4;
        else if (now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(4, 0)))  surge *= 1.2;

        return Math.min(Math.round(surge * 10.0) / 10.0, 4.0);
    }
}
