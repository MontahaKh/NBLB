package com.Shadows.authservice.controller;

import com.Shadows.authservice.model.Role;
import com.Shadows.authservice.model.User;
import com.Shadows.authservice.repository.UserRepository;
import com.Shadows.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get all sellers (users with SHOP role)
     * Required Authorization header with ADMIN token
     */
    @GetMapping("/users/sellers")
    public ResponseEntity<?> getAllSellers(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            List<Map<String, Object>> sellers = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.SHOP)
                    .map(user -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", user.getId());
                        map.put("username", user.getUsername());
                        map.put("email", user.getEmail());
                        map.put("role", user.getRole());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(sellers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching sellers: " + e.getMessage());
        }
    }

    /**
     * Get all users (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            List<Map<String, Object>> users = userRepository.findAll().stream()
                    .map(user -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", user.getId());
                        map.put("username", user.getUsername());
                        map.put("email", user.getEmail());
                        map.put("role", user.getRole());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching users: " + e.getMessage());
        }
    }

    /**
     * Get count of sellers
     */
    @GetMapping("/users/sellers/count")
    public ResponseEntity<?> getSellerCount(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            long count = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.SHOP)
                    .count();

            return ResponseEntity.ok(Collections.singletonMap("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error counting sellers: " + e.getMessage());
        }
    }

    /**
     * Check if user is admin
     */
    private boolean isAdmin(String authorization) {
        System.out.println("[UserController] Checking admin access. Authorization header: " +
                (authorization == null ? "NULL"
                        : (authorization.length() > 20 ? authorization.substring(0, 20) + "..." : authorization)));

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("[UserController] Auth header missing or invalid format");
            return false;
        }

        String token = authorization.substring(7);
        System.out.println(
                "[UserController] Extracted token: " + token.substring(0, Math.min(20, token.length())) + "...");

        boolean isValid = jwtUtil.validateToken(token);
        System.out.println("[UserController] Token valid: " + isValid);

        if (!isValid) {
            return false;
        }

        String role = jwtUtil.extractRole(token);
        System.out.println("[UserController] Extracted role: " + role);

        boolean isAdmin = "ADMIN".equals(role);
        System.out.println("[UserController] Is admin: " + isAdmin);
        return isAdmin;
    }

    /**
     * Create a new user (admin only)
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> userRequest) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            String username = userRequest.get("username");
            String email = userRequest.get("email");
            String password = userRequest.get("password");
            String roleStr = userRequest.get("role");

            // Validation
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }
            if (roleStr == null || roleStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Role is required");
            }

            // Check if user already exists
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            // Parse role
            Role role;
            try {
                role = Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid role. Must be: ADMIN, SHOP, CLIENT");
            }

            // Create new user
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(new BCryptPasswordEncoder().encode(password));
            newUser.setRole(role);

            User savedUser = userRepository.save(newUser);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedUser.getId());
            response.put("username", savedUser.getUsername());
            response.put("email", savedUser.getEmail());
            response.put("role", savedUser.getRole());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating user: " + e.getMessage());
        }
    }

    /**
     * Update an existing user (admin only)
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> userRequest) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            Optional<User> userOptional = userRepository.findById(id);
            if (!userOptional.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOptional.get();

            // Update fields if provided
            if (userRequest.containsKey("email") && userRequest.get("email") != null) {
                user.setEmail(userRequest.get("email"));
            }
            if (userRequest.containsKey("password") && userRequest.get("password") != null) {
                user.setPassword(new BCryptPasswordEncoder().encode(userRequest.get("password")));
            }
            if (userRequest.containsKey("role") && userRequest.get("role") != null) {
                try {
                    user.setRole(Role.valueOf(userRequest.get("role").toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Invalid role. Must be: ADMIN, SHOP, CLIENT");
                }
            }

            User updatedUser = userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getId());
            response.put("username", updatedUser.getUsername());
            response.put("email", updatedUser.getEmail());
            response.put("role", updatedUser.getRole());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user: " + e.getMessage());
        }
    }

    /**
     * Delete a user (admin only)
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            Optional<User> userOptional = userRepository.findById(id);
            if (!userOptional.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOptional.get();

            // Prevent deleting the admin user
            if ("admin".equalsIgnoreCase(user.getUsername())) {
                return ResponseEntity.badRequest().body("Cannot delete admin user");
            }

            userRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting user: " + e.getMessage());
        }
    }
}
