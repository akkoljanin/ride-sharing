package api.proxies;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "driver-service")
public interface DriverServiceProxy {
    @GetMapping("/drivers/available/count")
    Integer countAvailableDrivers();

    @PutMapping("/drivers/{email}/availability")
    void updateAvailability(@PathVariable String email, @RequestParam boolean available);
}
