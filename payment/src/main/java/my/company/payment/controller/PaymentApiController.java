package my.company.payment.controller;

import my.company.payment.model.Payment;
import my.company.payment.model.PaymentType;
import my.company.payment.repository.PaymentRepository;
import my.company.payment.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/payment/api")
public class PaymentApiController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PaymentRepository repo;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gateway.url}")
    private String gatewayUrl;

    @PostMapping("/process")
    public ResponseEntity<?> process(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestBody ProcessPaymentRequest request) {

        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing/invalid token"));
        }

        if (request == null || request.orderId == null || request.amount == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing orderId/amount"));
        }

        String username = jwtUtil.extractUsername(token);

        PaymentType type = normalizeType(request.method);
        Payment payment = new Payment();
        payment.setOrderId(request.orderId);
        payment.setAmount(request.amount);
        payment.setUsername(username);
        payment.setDate(new Date());
        payment.setType(type);

        if (type == PaymentType.CASH_ON_DELIVERY) {
            payment.setStatus("PENDING_DELIVERY");
            repo.save(payment);
            updateOrderStatus(request.orderId, "WAITING_DELIVERY", token);
        } else {
            // For the static frontend integration we mark it as paid.
            payment.setStatus("PAID");
            repo.save(payment);
            updateOrderStatus(request.orderId, "PAID", token);
        }

        return ResponseEntity.ok(Map.of(
                "paymentId", payment.getId(),
                "status", payment.getStatus()
        ));
    }

    private void updateOrderStatus(Long orderId, String status, String token) {
        try {
            String url = gatewayUrl + "/order-service/api/orders/" + orderId + "/status?status=" + status;
            HttpHeaders headers = new HttpHeaders();
            if (token != null && !token.isBlank()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception ignored) {
            // best-effort; order-service might be down
        }
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null) return null;
        String prefix = "Bearer ";
        if (!authorization.startsWith(prefix)) return null;
        return authorization.substring(prefix.length()).trim();
    }

    private static PaymentType normalizeType(String method) {
        if (method == null) return PaymentType.CARD_STRIPE;
        String m = method.trim().toUpperCase(Locale.ROOT);
        if (m.equals("CASH") || m.equals("CASH_ON_DELIVERY")) return PaymentType.CASH_ON_DELIVERY;
        if (m.equals("CARD") || m.equals("CARD_STRIPE") || m.equals("STRIPE")) return PaymentType.CARD_STRIPE;
        return PaymentType.CARD_STRIPE;
    }

    public static class ProcessPaymentRequest {
        public Long orderId;
        public Double amount;
        public String method;
    }
}
