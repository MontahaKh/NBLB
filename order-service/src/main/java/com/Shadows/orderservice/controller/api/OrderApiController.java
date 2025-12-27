package com.Shadows.orderservice.controller.api;

import com.Shadows.orderservice.Service.OrderServiceImp;
import com.Shadows.orderservice.model.Order;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.repository.ProductRepository;
import com.Shadows.orderservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/order-service/api")
public class OrderApiController {

    @Autowired
    private OrderServiceImp orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/orders/me")
    public ResponseEntity<?> getMyOrders(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing/invalid token");
        }

        String username = jwtUtil.extractUsername(token);
        List<Order> orders = orderService.getOrdersByUsername(username);

        List<OrderSummary> summaries = new ArrayList<>();
        for (Order o : orders) {
            OrderSummary s = new OrderSummary();
            s.id = o.getId();
            s.status = o.getStatus();
            s.total = safeParseDouble(o.getPrice());
            s.orderDate = o.getDate() != null ? Instant.ofEpochMilli(o.getDate().getTime()).toString() : null;
            summaries.add(s);
        }

        return ResponseEntity.ok(summaries);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CheckoutRequest request
    ) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing/invalid token"));
        }

        if (request == null || request.items == null || request.items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty cart"));
        }

        String username = jwtUtil.extractUsername(token);

        Map<Long, Integer> requestedQty = new LinkedHashMap<>();
        for (CheckoutItem item : request.items) {
            if (item == null || item.productId == null) continue;
            int qty = item.quantity == null ? 1 : item.quantity;
            if (qty <= 0) continue;
            requestedQty.merge(item.productId, qty, Integer::sum);
        }

        if (requestedQty.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No valid items"));
        }

        List<Product> products = productRepository.findAllById(requestedQty.keySet());
        if (products.size() != requestedQty.size()) {
            Set<Long> foundIds = new HashSet<>();
            for (Product p : products) foundIds.add(p.getId());
            List<Long> missing = new ArrayList<>();
            for (Long id : requestedQty.keySet()) {
                if (!foundIds.contains(id)) missing.add(id);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Products not found", "missingProductIds", missing));
        }

        Map<Long, Product> byId = new HashMap<>();
        for (Product p : products) byId.put(p.getId(), p);

        BigDecimal total = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (Map.Entry<Long, Integer> e : requestedQty.entrySet()) {
            Long productId = e.getKey();
            int qty = e.getValue();
            Product p = byId.get(productId);
            if (p == null) continue;

            int stock = p.getQuantity() == null ? 0 : p.getQuantity();
            if (qty > stock) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "Insufficient stock",
                        "productId", productId,
                        "available", stock,
                        "requested", qty
                ));
            }

            BigDecimal price = BigDecimal.valueOf(p.getPrice() == null ? 0.0 : p.getPrice());
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
            totalQuantity += qty;
        }

        total = total.setScale(2, RoundingMode.HALF_UP);

        // Decrement stock (best-effort single-threaded approach)
        for (Map.Entry<Long, Integer> e : requestedQty.entrySet()) {
            Product p = byId.get(e.getKey());
            if (p == null) continue;
            int newQty = (p.getQuantity() == null ? 0 : p.getQuantity()) - e.getValue();
            p.setQuantity(newQty);
            if (newQty <= 0) {
                p.setQuantity(0);
                p.setStatus(com.Shadows.orderservice.model.ProductStatus.OUT_OF_STOCK);
            }
        }
        productRepository.saveAll(products);

        Order order = new Order();
        order.setUsername(username);
        order.setStatus("PENDING");
        order.setDate(new Date());
        order.setRef((int) (Math.random() * 100000));
        order.setQuantity(totalQuantity);
        order.setPrice(total.toPlainString());
        order.setProducts(new ArrayList<>(products));

        Order saved = orderService.createOrder(order);

        return ResponseEntity.ok(Map.of(
                "orderId", saved.getId(),
                "total", total.doubleValue()
        ));
    }

    @GetMapping("/seller/sales")
    public ResponseEntity<?> getSellerSales(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing/invalid token"));
        }

        String role = Optional.ofNullable(jwtUtil.extractRole(token)).orElse("");
        if (!"SHOP".equalsIgnoreCase(role) && !"SELLER".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }

        String seller = jwtUtil.extractUsername(token);
        List<Order> orders = orderService.getOrdersByProductOwner(seller);

        List<SaleLine> lines = new ArrayList<>();
        for (Order o : orders) {
            String status = Optional.ofNullable(o.getStatus()).orElse("");
            if (!("PAID".equalsIgnoreCase(status)
                    || "WAITING_DELIVERY".equalsIgnoreCase(status)
                    || "SHIPPED".equalsIgnoreCase(status))) {
                continue;
            }

            if (o.getProducts() == null) continue;
            for (Product p : o.getProducts()) {
                if (p == null) continue;
                if (!seller.equalsIgnoreCase(Optional.ofNullable(p.getAddedBy()).orElse(""))) continue;

                SaleLine line = new SaleLine();
                line.orderId = o.getId();
                line.orderStatus = o.getStatus();
                line.orderDate = o.getDate() != null ? Instant.ofEpochMilli(o.getDate().getTime()).toString() : null;
                line.buyerUsername = o.getUsername();
                line.productId = p.getId();
                line.productName = p.getName();
                line.unitPrice = p.getPrice();
                lines.add(line);
            }
        }

        return ResponseEntity.ok(lines);
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null) return null;
        String prefix = "Bearer ";
        if (!authorization.startsWith(prefix)) return null;
        return authorization.substring(prefix.length()).trim();
    }

    private static double safeParseDouble(String value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    public static class CheckoutRequest {
        public List<CheckoutItem> items;
        public double total;
    }

    public static class CheckoutItem {
        public Long productId;
        public Integer quantity;
    }

    public static class OrderSummary {
        public Long id;
        public String orderDate;
        public double total;
        public String status;
    }

    public static class SaleLine {
        public Long orderId;
        public String orderDate;
        public String orderStatus;
        public String buyerUsername;
        public Long productId;
        public String productName;
        public Double unitPrice;
    }
}
