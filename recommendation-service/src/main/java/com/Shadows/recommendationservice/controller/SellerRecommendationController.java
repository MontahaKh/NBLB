package com.Shadows.recommendationservice.controller;

import com.Shadows.recommendationservice.model.ProductDto;
import com.Shadows.recommendationservice.service.GeminiService;
import com.Shadows.recommendationservice.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seller-specific recommendation endpoints
 * Requires SELLER or ADMIN role
 */
@RestController
@RequestMapping("/api/seller/recommendations")
public class SellerRecommendationController {

    private final GeminiService geminiService;
    private final JwtUtil jwtUtil;

    public SellerRecommendationController(GeminiService geminiService, JwtUtil jwtUtil) {
        this.geminiService = geminiService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Get top-sold items for the seller dashboard
     * Returns most popular products without AI processing
     */
    @GetMapping("/top-sold")
    public ResponseEntity<?> getTopSoldItems(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        // Security Check
        String token = extractAndValidateToken(authorization);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Please log in.");
        }

        // Check role is SELLER or ADMIN
        if (!isSellerOrAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: Only sellers and admins can access this.");
        }
        
        // Pass authorization header to service for seller-specific queries
        List<ProductDto> topSold = geminiService.getTopSoldItems(limit, authorization);
        System.out.println("✓ Top sold items returned: " + (topSold != null ? topSold.size() : 0) + " items");
        return ResponseEntity.ok(topSold);
    }

    /**
     * Get AI-powered suggestions for new products to add
     * Based on top-sold items, suggests complementary products
     */
    @PostMapping("/suggest-products")
    public ResponseEntity<?> suggestNewProducts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        
        // Security Check
        String token = extractAndValidateToken(authorization);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Please log in.");
        }

        if (!isSellerOrAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: Only sellers and admins can access this.");
        }

        // Extract top-sold items and catalog from payload
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topSoldMaps = (List<Map<String, Object>>) payload.get("topSoldItems");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> catalogMaps = (List<Map<String, Object>>) payload.get("currentCatalog");

        if (topSoldMaps == null || topSoldMaps.isEmpty()) {
            return ResponseEntity.badRequest().body("topSoldItems is required and cannot be empty");
        }

        // Convert maps to ProductDto objects
        List<ProductDto> topSold = topSoldMaps.stream()
                .map(this::mapToProductDto)
                .toList();

        List<ProductDto> catalog = catalogMaps != null 
                ? catalogMaps.stream().map(this::mapToProductDto).toList()
                : List.of();

        // Get suggestions
        List<String> suggestions = geminiService.suggestNewProducts(topSold, catalog);

        // Return both suggestions and metadata
        Map<String, Object> response = new HashMap<>();
        response.put("suggestions", suggestions);
        response.put("basedOn", topSold.stream().map(ProductDto::getName).toList());
        response.put("count", suggestions.size());

        return ResponseEntity.ok(response);
    }

    // ========== HELPER METHODS ==========

    private String extractAndValidateToken(String authorization) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }

        return token;
    }

    private boolean isSellerOrAdmin(String token) {
        String role = jwtUtil.extractRole(token);
        return role != null && (role.equals("SELLER") || role.equals("SHOP") || role.equals("ADMIN"));
    }

    private ProductDto mapToProductDto(Map<String, Object> map) {
        ProductDto dto = new ProductDto();
        if (map.get("id") != null) {
            dto.setId(((Number) map.get("id")).longValue());
        }
        if (map.get("name") != null) {
            dto.setName((String) map.get("name"));
        }
        if (map.get("category") != null) {
            dto.setCategory((String) map.get("category"));
        }
        if (map.get("price") != null) {
            dto.setPrice(((Number) map.get("price")).doubleValue());
        }
        return dto;
    }
}
