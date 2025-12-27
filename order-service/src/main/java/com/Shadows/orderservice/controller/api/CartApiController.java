package com.Shadows.orderservice.controller.api;

import com.Shadows.orderservice.Service.CartServiceImp;
import com.Shadows.orderservice.controller.api.dto.CartDto;
import com.Shadows.orderservice.controller.api.dto.ProductDto;
import com.Shadows.orderservice.model.Cart;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order-service")
public class CartApiController {

    @Autowired
    private CartServiceImp cartService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/cart")
    public ResponseEntity<?> addToCart(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AddToCartRequest request
    ) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing/invalid token");
        }

        if (request == null || request.productId == null) {
            return ResponseEntity.badRequest().body("Missing productId");
        }

        String username = jwtUtil.extractUsername(token);
        Cart cart = cartService.addToCart(username, request.productId);
        return ResponseEntity.ok(toDto(cart));
    }

    private static CartDto toDto(Cart cart) {
        List<ProductDto> products = cart.getProducts() == null
                ? List.of()
                : cart.getProducts().stream().map(CartApiController::toDto).collect(Collectors.toList());

        return new CartDto(cart.getId(), cart.getUsername(), products);
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

    public static class AddToCartRequest {
        public Long productId;
        public Integer quantity;
    }
}
