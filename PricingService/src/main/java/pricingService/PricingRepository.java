package pricingService;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PricingRepository extends JpaRepository<PricingModel, Long> {
    List<PricingModel> findTop20ByOrderByCalculatedAtDesc();
}
