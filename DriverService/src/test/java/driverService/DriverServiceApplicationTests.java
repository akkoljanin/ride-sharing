package driverService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DriverServiceApplicationTests {

    @Autowired
    private DriverRepository repo;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldLoadInitialDrivers() {
        long count = repo.count();
        assertTrue(count >= 3, "Should have at least 3 seed drivers");
    }

    @Test
    void shouldFindAvailableDrivers() {
        long available = repo.countByAvailableTrue();
        assertTrue(available >= 1, "Should have at least one available driver");
    }

    @Test
    void shouldFindDriverByEmail() {
        var driver = repo.findByEmail("driver1@ridesharing.rs");
        assertTrue(driver.isPresent());
        assertEquals("Marko Petrović", driver.get().getName());
    }
}
