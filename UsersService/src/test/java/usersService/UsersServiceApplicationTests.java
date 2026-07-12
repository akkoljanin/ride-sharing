package usersService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UsersServiceApplicationTests {

    @Autowired
    private UserRepository repo;

    @Test
    void contextLoads() {}

    @Test
    void shouldFindAdminUser() {
        var user = repo.findByEmail("admin@ridesharing.rs");
        assertTrue(user.isPresent());
        assertEquals("ADMIN", user.get().getRole());
    }

    @Test
    void shouldFindRiderUser() {
        var user = repo.findByEmail("marko@example.com");
        assertTrue(user.isPresent());
        assertEquals("RIDER", user.get().getRole());
    }

    @Test
    void shouldReturnTrueForExistingEmail() {
        assertTrue(repo.existsByEmail("jovana@example.com"));
    }

    @Test
    void shouldReturnFalseForNonExistingEmail() {
        assertFalse(repo.existsByEmail("nonexistent@example.com"));
    }
}
