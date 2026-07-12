package usersService;

import api.dtos.UserDto;
import api.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import util.exception.AccessDeniedException;
import util.exception.DataIntegrityViolationException;
import util.exception.NoDataFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository repo;

    @Override
    public ResponseEntity<?> getUsers() {
        List<UserModel> models = repo.findAll();
        if (models.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No users found.");
        }
        List<UserDto> dtos = new ArrayList<>();
        for (UserModel m : models) dtos.add(toDto(m));
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        Optional<UserModel> model = repo.findByEmail(email);
        if (model.isPresent()) {
            return ResponseEntity.ok(toDto(model.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + email);
    }

    @Override
    public ResponseEntity<?> registerUser(@RequestBody UserDto dto) {
        String role = dto.getRole() == null ? "RIDER" : dto.getRole().toUpperCase();
        if (!role.equals("RIDER") && !role.equals("DRIVER")) {
            throw new DataIntegrityViolationException("Role must be RIDER or DRIVER for self-registration.");
        }
        if (repo.existsByEmail(dto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered.");
        }
        dto.setRole(role);
        UserModel saved = repo.save(toModel(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Override
    public ResponseEntity<?> createAdmin(@RequestBody UserDto dto) {
        if (!"ADMIN".equals(dto.getRole().toUpperCase())) {
            throw new DataIntegrityViolationException("Only ADMIN role allowed via this endpoint.");
        }
        if (repo.existsByEmail(dto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered.");
        }
        dto.setRole("ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(toModel(dto)));
    }

    @Override
    public ResponseEntity<?> updateUser(@PathVariable String email, @RequestBody UserDto dto,
                                        @RequestHeader("X-User-Role") String currentRole) {
        Optional<UserModel> existing = repo.findByEmail(email);
        if (existing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        if ("ROLE_RIDER".equals(currentRole) && !email.equals(dto.getEmail())) {
            throw new AccessDeniedException("RIDER can only update their own account.");
        }
        repo.updateUser(dto.getEmail(), dto.getPassword(), dto.getRole().toUpperCase(), email);
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        Optional<UserModel> user = repo.findByEmail(email);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        repo.delete(user.get());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<?> checkRole(@PathVariable String email) {
        Optional<UserModel> user = repo.findByEmail(email);
        if (user.isPresent()) return ResponseEntity.ok(user.get().getRole());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private UserDto toDto(UserModel m) {
        return new UserDto(m.getEmail(), m.getPassword(), m.getRole(), m.getPhone());
    }

    private UserModel toModel(UserDto d) {
        return new UserModel(d.getEmail(), d.getPassword(), d.getRole(), d.getPhone());
    }
}
