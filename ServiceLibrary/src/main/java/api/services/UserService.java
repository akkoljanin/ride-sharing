package api.services;

import api.dtos.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
public interface UserService {
    @GetMapping("/users")
    ResponseEntity<?> getUsers();

    @GetMapping("/users/email")
    ResponseEntity<?> getUserByEmail(@RequestParam String email);

    @PostMapping("/users/register")
    ResponseEntity<?> registerUser(@RequestBody UserDto dto);

    @PostMapping("/users/admin")
    ResponseEntity<?> createAdmin(@RequestBody UserDto dto);

    @PutMapping("/users/{email}")
    ResponseEntity<?> updateUser(@PathVariable String email, @RequestBody UserDto dto,
                                 @RequestHeader("X-User-Role") String currentRole);

    @DeleteMapping("/users/{email}")
    ResponseEntity<?> deleteUser(@PathVariable String email);

    @GetMapping("/users/{email}/role")
    ResponseEntity<?> checkRole(@PathVariable String email);
}
