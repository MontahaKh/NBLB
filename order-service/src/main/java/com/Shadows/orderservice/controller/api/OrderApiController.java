package com.Shadows.orderservice.controller.api;

import com.Shadows.orderservice.Service.OrderServiceImp;
import com.Shadows.orderservice.model.Order;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.repository.ProductRepository;
import com.Shadows.orderservice.util.JwtUtil;
import com.Shadows.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order-service/api")
public class OrderApiController {

    @Autowired
    private OrderServiceImp orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/orders/me")
    public ResponseEntity<?> getMyOrders(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
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
            @RequestBody CheckoutRequest request) {
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
            if (item == null || item.productId == null)
                continue;
            int qty = item.quantity == null ? 1 : item.quantity;
            if (qty <= 0)
                continue;
            requestedQty.merge(item.productId, qty, Integer::sum);
        }

        if (requestedQty.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No valid items"));
        }

        List<Product> products = productRepository.findAllById(requestedQty.keySet());
        if (products.size() != requestedQty.size()) {
            Set<Long> foundIds = new HashSet<>();
            for (Product p : products)
                foundIds.add(p.getId());
            List<Long> missing = new ArrayList<>();
            for (Long id : requestedQty.keySet()) {
                if (!foundIds.contains(id))
                    missing.add(id);
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Products not found", "missingProductIds", missing));
        }

        Map<Long, Product> byId = new HashMap<>();
        for (Product p : products)
            byId.put(p.getId(), p);

        BigDecimal total = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (Map.Entry<Long, Integer> e : requestedQty.entrySet()) {
            Long productId = e.getKey();
            int qty = e.getValue();
            Product p = byId.get(productId);
            if (p == null)
                continue;

            int stock = p.getQuantity() == null ? 0 : p.getQuantity();
            if (qty > stock) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "Insufficient stock",
                        "productId", productId,
                        "available", stock,
                        "requested", qty));
            }

            BigDecimal price = BigDecimal.valueOf(p.getPrice() == null ? 0.0 : p.getPrice());
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
            totalQuantity += qty;
        }

        total = total.setScale(2, RoundingMode.HALF_UP);

        // Decrement stock (best-effort single-threaded approach)
        for (Map.Entry<Long, Integer> e : requestedQty.entrySet()) {
            Product p = byId.get(e.getKey());
            if (p == null)
                continue;
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
                "total", total.doubleValue()));
    }

    @GetMapping("/seller/sales")
    public ResponseEntity<?> getSellerSales(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
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

            if (o.getProducts() == null)
                continue;
            for (Product p : o.getProducts()) {
                if (p == null)
                    continue;
                if (!seller.equalsIgnoreCase(Optional.ofNullable(p.getAddedBy()).orElse("")))
                    continue;

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
        if (authorization == null)
            return null;
        String prefix = "Bearer ";
        if (!authorization.startsWith(prefix))
            return null;
        return authorization.substring(prefix.length()).trim();
    }

    /**
     * Get all products (public access for sellers and customers)
     */
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        List<Map<String, Object>> products = productRepository.findAll().stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    map.put("seller", p.getAddedBy());
                    map.put("addedBy", p.getAddedBy());
                    map.put("category", p.getCategory());
                    map.put("price", p.getPrice());
                    map.put("quantity", p.getQuantity());
                    map.put("stock", p.getQuantity());
                    map.put("status", p.getStatus());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(products);
    }

    /**
     * Get seller's own products
     */
    @GetMapping("/seller/products")
    public ResponseEntity<?> getSellerProducts(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing/invalid token"));
        }

        String seller = jwtUtil.extractUsername(token);
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> seller.equalsIgnoreCase(Optional.ofNullable(p.getAddedBy()).orElse("")))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = products.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    map.put("seller", p.getAddedBy());
                    map.put("addedBy", p.getAddedBy());
                    map.put("category", p.getCategory());
                    map.put("price", p.getPrice());
                    map.put("quantity", p.getQuantity());
                    map.put("stock", p.getQuantity());
                    map.put("status", p.getStatus());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Create product (seller)
     */
    @PostMapping("/seller/products")
    public ResponseEntity<?> createSellerProduct(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> request) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing/invalid token"));
        }

        String seller = jwtUtil.extractUsername(token);

        try {
            Product product = new Product();
            product.setName((String) request.get("name"));

            String categoryStr = (String) request.get("category");
            if (categoryStr != null && !categoryStr.isEmpty()) {
                product.setCategory(
                        com.Shadows.orderservice.model.Category.valueOf(categoryStr.toUpperCase().replace(" ", "_")));
            }

            product.setPrice(((Number) request.get("price")).doubleValue());
            product.setQuantity(((Number) request.get("quantity")).intValue());
            product.setAddedBy(seller);
            product.setStatus(com.Shadows.orderservice.model.ProductStatus.AVAILABLE);

            Product saved = productRepository.save(product);

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("name", saved.getName());
            response.put("category", saved.getCategory());
            response.put("price", saved.getPrice());
            response.put("quantity", saved.getQuantity());
            response.put("status", saved.getStatus());
            response.put("seller", saved.getAddedBy());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update seller product
     */
    @PutMapping("/seller/products/{id}")
    public ResponseEntity<?> updateSellerProduct(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing/invalid token"));
        }

        String seller = jwtUtil.extractUsername(token);
        Optional<Product> opt = productRepository.findById(id);

        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Product product = opt.get();
        if (!seller.equalsIgnoreCase(Optional.ofNullable(product.getAddedBy()).orElse(""))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not your product"));
        }

        try {
            if (request.containsKey("name")) {
                product.setName((String) request.get("name"));
            }
            if (request.containsKey("category")) {
                String categoryStr = (String) request.get("category");
                if (categoryStr != null && !categoryStr.isEmpty()) {
                    product.setCategory(com.Shadows.orderservice.model.Category
                            .valueOf(categoryStr.toUpperCase().replace(" ", "_")));
                }
            }
            if (request.containsKey("price")) {
                product.setPrice(((Number) request.get("price")).doubleValue());
            }
            if (request.containsKey("quantity")) {
                product.setQuantity(((Number) request.get("quantity")).intValue());
            }
            if (request.containsKey("status")) {
                String statusStr = (String) request.get("status");
                product.setStatus(com.Shadows.orderservice.model.ProductStatus.valueOf(statusStr));
            }

            Product updated = productRepository.save(product);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updated.getId());
            response.put("name", updated.getName());
            response.put("category", updated.getCategory());
            response.put("price", updated.getPrice());
            response.put("quantity", updated.getQuantity());
            response.put("status", updated.getStatus());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete seller product
     */
    @DeleteMapping("/seller/products/{id}")
    public ResponseEntity<?> deleteSellerProduct(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing/invalid token"));
        }

        String seller = jwtUtil.extractUsername(token);
        Optional<Product> opt = productRepository.findById(id);

        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Product product = opt.get();
        if (!seller.equalsIgnoreCase(Optional.ofNullable(product.getAddedBy()).orElse(""))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not your product"));
        }

        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted"));
    }

    /**
     * Update order status to SHIPPED (seller)
     */
    @PutMapping("/seller/orders/{id}/ship")
    public ResponseEntity<?> shipSellerOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing/invalid token"));
        }

        String seller = jwtUtil.extractUsername(token);
        Optional<Order> opt = orderRepository.findById(id);

        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Order order = opt.get();

        // Verify that this order contains products from this seller
        boolean hasSellerProduct = order.getProducts() != null &&
                order.getProducts().stream()
                        .anyMatch(p -> seller.equalsIgnoreCase(Optional.ofNullable(p.getAddedBy()).orElse("")));

        if (!hasSellerProduct) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "This order does not contain your products"));
        }

        // Only allow shipping if order is PAID or WAITING_DELIVERY
        String currentStatus = order.getStatus();
        if (!("PAID".equalsIgnoreCase(currentStatus) || "WAITING_DELIVERY".equalsIgnoreCase(currentStatus))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Order cannot be shipped. Current status: " + currentStatus));
        }

        order.setStatus("SHIPPED");
        Order updated = orderRepository.save(order);

        Map<String, Object> response = new HashMap<>();
        response.put("id", updated.getId());
        response.put("status", updated.getStatus());
        response.put("message", "Order marked as shipped");

        return ResponseEntity.ok(response);
    }

    private static double safeParseDouble(String value) {
        if (value == null)
            return 0.0;
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
