package com.Shadows.orderservice.controller.api;

import com.Shadows.orderservice.Service.ProductServiceImp;
import com.Shadows.orderservice.controller.api.dto.ProductDto;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.model.ProductStatus;
import com.Shadows.orderservice.repository.ProductRepository;
import com.Shadows.orderservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order-service")
public class ProductApiController {

    @Autowired
    private ProductServiceImp productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/products")
    public List<ProductDto> getProducts() {
        return productService.getAllProducts().stream().map(ProductApiController::toDto).collect(Collectors.toList());
    }

    @GetMapping("/my-products")
    public ResponseEntity<?> getMyProducts(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing/invalid token");
        }

        String role = Optional.ofNullable(jwtUtil.extractRole(token)).orElse("");
        if (!isSellerRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        String username = jwtUtil.extractUsername(token);
        List<ProductDto> products = productRepository.findByAddedBy(username).stream()
            .map(ProductApiController::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CreateProductRequest request
    ) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing/invalid token");
        }

        String role = Optional.ofNullable(jwtUtil.extractRole(token)).orElse("");
        if (!isSellerRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        String username = jwtUtil.extractUsername(token);

        Product product = new Product();
        product.setName(request.name);
        product.setPrice(request.price);
        product.setDescription(request.description);
        product.setQuantity(request.quantity);
        product.setCategory(request.category);
        product.setImageUrl(request.imageUrl);
        product.setAddedBy(username);
        product.setCreatedAt(new Date(System.currentTimeMillis()));
        product.setStatus(request.status != null ? request.status : ProductStatus.AVAILABLE);

        if (request.expiryDate != null && !request.expiryDate.isBlank()) {
            try {
                LocalDate parsed = LocalDate.parse(request.expiryDate);
                product.setExpieryDate(Date.valueOf(parsed));
            } catch (DateTimeParseException ignored) {
                // keep null if invalid
            }
        }

        Product saved = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    private static ProductDto toDto(Product p) {
        return new ProductDto(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getDescription(),
                p.getQuantity(),
                p.getImageUrl(),
                p.getCategory(),
                p.getStatus(),
                p.getAddedBy(),
                p.getCreatedAt(),
                p.getExpieryDate()
        );
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null) return null;
        String prefix = "Bearer ";
        if (!authorization.startsWith(prefix)) return null;
        return authorization.substring(prefix.length()).trim();
    }

    private static boolean isSellerRole(String role) {
        return "SHOP".equalsIgnoreCase(role) || "SELLER".equalsIgnoreCase(role);
    }

    public static class CreateProductRequest {
        public String name;
        public Double price;
        public String description;
        public Integer quantity;
        public String expiryDate;
        public com.Shadows.orderservice.model.Category category;
        public String imageUrl;
        public ProductStatus status;
    }
}
