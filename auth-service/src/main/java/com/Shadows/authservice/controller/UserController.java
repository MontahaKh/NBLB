package com.Shadows.authservice.controller;

import com.Shadows.authservice.model.Role;
import com.Shadows.authservice.model.User;
import com.Shadows.authservice.repository.UserRepository;
import com.Shadows.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
