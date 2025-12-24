package com.Shadows.orderservice.controller;

import com.Shadows.orderservice.Service.ProductServiceImp;
import com.Shadows.orderservice.util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/order-service/")
public class DashboardController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ProductServiceImp productService;

    @Value("${gateway.url}")
    private String gatewayUrl;

    @GetMapping("/frontpage")
    public String frontpage(@RequestParam(value = "token", required = false) String token,
                           HttpSession session, Model model) {
        model.addAttribute("gatewayUrl", gatewayUrl);
        if (token != null) {
            // Store token in session for API calls
            session.setAttribute("jwtToken", token);
            model.addAttribute("successMessage", "Login successful!");
        }
        
        // Check for existing session token to set role
        String currentToken = token != null ? token : (String) session.getAttribute("jwtToken");
        if (currentToken != null && jwtUtil.validateToken(currentToken)) {
            String role = jwtUtil.extractRole(currentToken);
            model.addAttribute("userRole", role);
            model.addAttribute("username", jwtUtil.extractUsername(currentToken));
        } else {
            model.addAttribute("userRole", "VISITOR");
        }
        
        // Fetch all products for visitor view
        model.addAttribute("products", productService.getAllProducts());
        
        return "frontpage";
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(@RequestParam(value = "token", required = false) String token,
                           HttpSession session, Model model) {
        model.addAttribute("gatewayUrl", gatewayUrl);
        String currentToken = token;
        if (currentToken == null) {
            currentToken = (String) session.getAttribute("jwtToken");
        }

        if (currentToken != null && jwtUtil.validateToken(currentToken)) {
            String role = jwtUtil.extractRole(currentToken);
            if ("ADMIN".equals(role)) {
                if (token != null) {
                    session.setAttribute("jwtToken", token);
                    model.addAttribute("successMessage", "Login successful!");
                }
                model.addAttribute("userRole", role);
                model.addAttribute("username", jwtUtil.extractUsername(currentToken));
                return "dashboard_admin";
            }
        }
        return "redirect:/order-service/frontpage";
    }

    @GetMapping("/dashboard/shop")
    public String shopDashboard(@RequestParam(value = "token", required = false) String token,
                           HttpSession session, Model model) {
        model.addAttribute("gatewayUrl", gatewayUrl);
        try {
            String currentToken = token;
            if (currentToken == null) {
                currentToken = (String) session.getAttribute("jwtToken");
            }

            if (currentToken != null && jwtUtil.validateToken(currentToken)) {
                String role = jwtUtil.extractRole(currentToken);
                if ("SHOP".equals(role)) {
                    if (token != null) {
                        session.setAttribute("jwtToken", token);
                        model.addAttribute("successMessage", "Login successful!");
                    }
                    model.addAttribute("userRole", role);
                    model.addAttribute("username", jwtUtil.extractUsername(currentToken));
                    return "dashboard_shop";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/order-service/frontpage?error=" + e.getMessage();
        }
        return "redirect:/order-service/frontpage";
    }

    @GetMapping("/dashboard/client")
    public String clientDashboard(@RequestParam(value = "token", required = false) String token,
                           HttpSession session, Model model) {
        model.addAttribute("gatewayUrl", gatewayUrl);
        model.addAttribute("token", token);
        try {
            String currentToken = token;
            if (currentToken == null) {
                currentToken = (String) session.getAttribute("jwtToken");
            }

            if (currentToken != null && jwtUtil.validateToken(currentToken)) {
                String role = jwtUtil.extractRole(currentToken);
                if ("CLIENT".equals(role)) {
                    if (token != null) {
                        session.setAttribute("jwtToken", token);
                        model.addAttribute("successMessage", "Login successful!");
                    }
                    model.addAttribute("userRole", role);
                    model.addAttribute("username", jwtUtil.extractUsername(currentToken));
                    // Fetch all products for client view
                    model.addAttribute("products", productService.getAllProducts());
                    return "dashboard_client";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/order-service/frontpage?error=" + e.getMessage();
        }
        return "redirect:/order-service/frontpage";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:"+gatewayUrl+"/order-service/frontpage";
    }
}