package com.Shadows.authservice.controller;

import com.Shadows.authservice.model.User;
import com.Shadows.authservice.service.AuthService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Value("${gateway.url:http://localhost:8222}")
    private String gatewayUrl;

    // Web page endpoints
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }


    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model, RedirectAttributes redirectAttributes) {
        try {
            ResponseEntity<?> response = authService.registerUser(user);
            if (response.getStatusCode().is2xxSuccessful()) {
                redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
                return "redirect:/auth/login";
            } else {
                model.addAttribute("errorMessage", String.valueOf(response.getBody()));
                return "register";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String username, @RequestParam String password,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            ResponseEntity<?> response = authService.loginUser(user);
            if (response.getStatusCode().is2xxSuccessful()) {
                // Extract token and role from response
                Map<String, String> responseBody = (Map<String, String>) response.getBody();
                if (responseBody != null && responseBody.containsKey("token")) {
                    String token = responseBody.get("token");
                    String role = responseBody.get("role");
                    redirectAttributes.addFlashAttribute("successMessage", "Login successful!");
                    
                    // Redirect based on role
                    String redirectPath = "/order-service/frontpage"; // Default
                    if ("ADMIN".equals(role)) {
                        redirectPath = "/order-service/dashboard/admin";
                    } else if ("SHOP".equals(role)) {
                        redirectPath = "/order-service/dashboard/shop";
                    } else if ("CLIENT".equals(role)) {
                        redirectPath = "/order-service/dashboard/client";
                    }
                    
                    return "redirect:" + gatewayUrl + redirectPath + "?token=" + token;
                } else {
                    model.addAttribute("errorMessage", "Failed to retrieve token");
                    return "login";
                }
            } else {
                model.addAttribute("errorMessage", String.valueOf(response.getBody()));
                return "login";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Login failed: " + e.getMessage());
            return "login";
        }
    }

    // API endpoints (keeping for backward compatibility)
    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<?> registerUserApi(@RequestBody User user) {
        return authService.registerUser(user);
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> loginUserApi(@RequestBody User user) {
        return authService.loginUser(user);
    }
}