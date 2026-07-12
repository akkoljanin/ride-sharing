package api.services;

import api.dtos.PriceRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
public interface PricingService {
    @PostMapping("/pricing/calculate")
    ResponseEntity<?> calculatePrice(@RequestBody PriceRequestDto request);

    @GetMapping("/pricing/history")
    ResponseEntity<?> getPricingHistory();
}
