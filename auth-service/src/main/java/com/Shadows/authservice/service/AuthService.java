package com.Shadows.authservice.service;

import com.Shadows.authservice.model.Role;
import com.Shadows.authservice.model.User;
import com.Shadows.authservice.repository.UserRepository;
import com.Shadows.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public ResponseEntity<?> registerUser(User user) {
        try {
            // Check if user already exists
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            // Check if email already exists
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            // Set default role if not present
            if (user.getRole() == null) {
                user.setRole(Role.CLIENT);
            }

            // Encode password
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // Save user
            User savedUser = userRepository.save(user);

            return ResponseEntity.ok("User registered successfully with ID: " + savedUser.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Registration failed: " + e.getMessage());
        }
    }

    public ResponseEntity<?> loginUser(User user) {
        try {
            Optional<User> existingUser = userRepository.findByUsername(user.getUsername());

            if (existingUser.isPresent() && passwordEncoder.matches(user.getPassword(), existingUser.get().getPassword())) {
                String token = jwtUtil.generateToken(user.getUsername(), existingUser.get().getRole().name());
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("role", existingUser.get().getRole().name());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("Invalid username or password");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Login failed: " + e.getMessage());
        }
    }

    public User register(User user) {
        // Set default role if not present
        if (user.getRole() == null) {
            user.setRole(Role.CLIENT);
        }
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public String login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return jwtUtil.generateToken(username, user.get().getRole().name());
        }
        return null;
    }

    public boolean validateUser(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.isPresent() && passwordEncoder.matches(password, user.get().getPassword());
    }

    public boolean validateToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            return jwtUtil.validateToken(token, username);
        } catch (Exception e) {
            return false;
        }
    }
}