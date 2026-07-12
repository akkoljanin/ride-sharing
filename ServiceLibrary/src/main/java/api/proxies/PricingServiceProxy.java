package api.proxies;

import api.dtos.PriceRequestDto;
import api.dtos.PriceResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pricing-service")
public interface PricingServiceProxy {
    @PostMapping("/pricing/calculate")
    PriceResponseDto calculatePrice(@RequestBody PriceRequestDto request);
}
