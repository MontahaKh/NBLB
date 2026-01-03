package com.Shadows.orderservice.controller.api;

import com.Shadows.orderservice.model.Order;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.repository.OrderRepository;
import com.Shadows.orderservice.repository.ProductRepository;
import com.Shadows.orderservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order-service/api/admin")
public class AdminApiController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${gateway.url:http://localhost:8222}")
    private String gatewayUrl;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Get admin dashboard statistics
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            long totalProducts = productRepository.count();
            long totalOrders = orderRepository.count();

            // Calculate total revenue from orders
            double totalRevenue = orderRepository.findAll().stream()
                    .mapToDouble(order -> order.getTotal())
                    .sum();

            // Get seller count from auth-service
            long totalSellers = getSellerCountFromAuthService(authorization);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", getTotalUsersFromAuthService(authorization));
            stats.put("totalSellers", totalSellers);
            stats.put("totalOrders", totalOrders);
            stats.put("totalRevenue", String.format("$%.2f", totalRevenue));
            stats.put("totalProducts", totalProducts);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching stats: " + e.getMessage());
        }
    }

    /**
     * Get all products for admin
     */
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            List<Map<String, Object>> products = productRepository.findAll().stream()
                    .map(p -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", p.getId());
                        map.put("name", p.getName());
                        map.put("seller", p.getAddedBy());
                        map.put("category", p.getCategory());
                        map.put("price", p.getPrice());
                        map.put("stock", p.getQuantity());
                        map.put("status", p.getStatus());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching products: " + e.getMessage());
        }
    }

    /**
     * Get all orders for admin
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            List<Map<String, Object>> orders = orderRepository.findAll().stream()
                    .map(o -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", o.getId());
                        map.put("customer", o.getUsername());
                        map.put("date", o.getOrderDate());
                        map.put("total", String.format("$%.2f", o.getTotal()));
                        map.put("status", o.getStatus());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching orders: " + e.getMessage());
        }
    }

    /**
     * Get all sellers (users with SHOP role) from auth-service
     */
    @GetMapping("/sellers")
    public ResponseEntity<?> getAllSellers(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            // Call auth-service to get sellers with real emails
            String authServiceUrl = gatewayUrl + "/auth/api/users/sellers";

            try {
                ResponseEntity<List> response = restTemplate.getForEntity(
                        authServiceUrl,
                        List.class,
                        new org.springframework.http.HttpEntity<>(
                                new org.springframework.http.HttpHeaders() {
                                    {
                                        set("Authorization", authorization);
                                    }
                                }));

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // Add product count for each seller
                    List<Map<String, Object>> sellersWithProducts = new ArrayList<>();
                    for (Object seller : response.getBody()) {
                        if (seller instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> sellerMap = (Map<String, Object>) seller;
                            String username = (String) sellerMap.get("username");
                            long productCount = productRepository.findByAddedBy(username).size();
                            sellerMap.put("productCount", productCount);
                            sellersWithProducts.add(sellerMap);
                        }
                    }
                    return ResponseEntity.ok(sellersWithProducts);
                }
            } catch (Exception e) {
                // Fallback if auth-service is unavailable
                System.err.println("Auth-service unavailable, using fallback: " + e.getMessage());
            }

            // Fallback: Get unique sellers from products
            List<Map<String, Object>> sellers = productRepository.findAll().stream()
                    .map(Product::getAddedBy)
                    .distinct()
                    .map(seller -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("username", seller);
                        map.put("email", seller + "@nblb.com");
                        long productCount = productRepository.findByAddedBy(seller).size();
                        map.put("productCount", productCount);
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
     * Check if user is admin
     */
    private boolean isAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return false;
        }

        String role = jwtUtil.extractRole(token);
        return "ADMIN".equals(role);
    }

    /**
     * Get total seller count from auth-service
     */
    private long getSellerCountFromAuthService(String authorization) {
        try {
            String authServiceUrl = gatewayUrl + "/auth/api/users/sellers/count";
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    authServiceUrl,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object count = response.getBody().get("count");
                if (count instanceof Number) {
                    return ((Number) count).longValue();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not fetch seller count from auth-service: " + e.getMessage());
        }

        // Fallback
        return getSellerCount();
    }

    /**
     * Get total user count from auth-service
     */
    private long getTotalUsersFromAuthService(String authorization) {
        try {
            String authServiceUrl = gatewayUrl + "/auth/api/users";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", authorization);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    authServiceUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().size();
            }
        } catch (Exception e) {
            System.err.println("Could not fetch user count from auth-service: " + e.getMessage());
        }

        // Fallback
        return productRepository.count() + orderRepository.count() + 4;
    }

    /**
     * Get count of sellers (fallback)
     */
    private long getSellerCount() {
        return productRepository.findAll().stream()
                .map(Product::getAddedBy)
                .distinct()
                .count();
    }
}
