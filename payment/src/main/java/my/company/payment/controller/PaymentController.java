package my.company.payment.controller;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import my.company.payment.model.Payment;
import my.company.payment.model.PaymentType;
import my.company.payment.repository.PaymentRepository;
import my.company.payment.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PaymentRepository repo;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gateway.url}")
    private String gatewayUrl;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;


    // -------------------------
    // 1️⃣ Affichage du formulaire
    // -------------------------
    @GetMapping("/form")
    public String showPaymentForm(@RequestParam Long orderId,
                                  @RequestParam Double amount,
                                  @RequestParam("token") String token,
                                  Model model) {


        if (!jwtUtil.validateToken(token)) {
            return "redirect:/auth/login"; // token invalide
        }

        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        model.addAttribute("gatewayUrl", gatewayUrl);
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setUsername(username); // facultatif mais utile

        model.addAttribute("payment", payment);
        model.addAttribute("token", token);
        model.addAttribute("username", username);
        model.addAttribute("userRole", role);


        // important pour les redirections
        return "payment-form";
    }


    // -------------------------
    // 2️⃣ Traitement du paiement
    // -------------------------
    @PostMapping("/process")
    public String processPayment(@ModelAttribute Payment payment,
                                 @RequestParam("token") String token,
                                 Model model) {

        if (!jwtUtil.validateToken(token)) {
            return "redirect:/auth/login";
        }

        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);


        // Paiement à la livraison
        if (payment.getType() == PaymentType.CASH_ON_DELIVERY) {
            payment.setStatus("PENDING_DELIVERY");
            payment.setDate(new Date());
            payment.setUsername(username);
            repo.save(payment);

            // Mise à jour de la commande
            updateOrderStatus(payment.getOrderId(), "WAITING_DELIVERY", token);

            model.addAttribute("message", "Paiement à la livraison validé !");
            model.addAttribute("payment", payment);
            model.addAttribute("ticketUrl", "/payment/ticket/" + payment.getId() + "?token=" + token);
            model.addAttribute("username", username);
            model.addAttribute("userRole", role);
            model.addAttribute("orderId", payment.getOrderId());
            model.addAttribute("token", token);



            return "payment-success";
        }

        // Paiement par carte (Stripe)
        if (payment.getType() == PaymentType.CARD_STRIPE) {
            return "redirect:/payment/stripe?orderId=" + payment.getOrderId()
                    + "&amount=" + payment.getAmount()
                    + "&token=" + token;
        }

        return "payment-form";
    }


    // -------------------------
    // 3️⃣ Stripe session
    // -------------------------
    @GetMapping("/stripe")
    public String createStripeSession(@RequestParam Long orderId,
                                      @RequestParam Double amount,
                                      @RequestParam("token") String token) throws Exception {

        if (!jwtUtil.validateToken(token)) {
            return "redirect:/auth/login";
        }

        Stripe.apiKey = stripeSecretKey;
        long amountCents = (long) (amount * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?orderId=" + orderId + "&amount=" + amount + "&token=" + token)
                .setCancelUrl(cancelUrl + "?token=" + token)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Paiement commande " + orderId)
                                        .build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);

        return "redirect:" + session.getUrl();
    }


    // -------------------------
    // 4️⃣ Stripe → Success
    // -------------------------
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam Long orderId,
                                 @RequestParam Double amount,
                                 @RequestParam("token") String token,
                                 Model model) {

        if (!jwtUtil.validateToken(token)) {
            return "redirect:/auth/login";
        }

        String username = jwtUtil.extractUsername(token);

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setStatus("PAID");
        payment.setType(PaymentType.CARD_STRIPE);
        payment.setDate(new Date());
        payment.setUsername(username);
        repo.save(payment);

        // Mise à jour commande
        updateOrderStatus(orderId, "PAID", token);

        model.addAttribute("payment", payment);
        model.addAttribute("token", token);
        model.addAttribute("ticketUrl",
                "/payment/ticket/" + payment.getId() + "?token=" + token);


        return "payment-success";
    }


    // -------------------------
    // 5️⃣ Stripe → Cancel
    // -------------------------
    @GetMapping("/cancel")
    public String cancelPayment(@RequestParam("token") String token, Model model) {
        model.addAttribute("message", "Paiement annulé.");
        model.addAttribute("token", token);
        return "cancel-payment";
    }


    // -------------------------
    // 6️⃣ Ticket
    // -------------------------
    @GetMapping("/ticket/{id}")
    public String viewTicket(@PathVariable Long id,
                             @RequestParam("token") String token,
                             Model model) {


            Payment payment = repo.findById(id).orElse(null);
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            if (payment == null) {
                model.addAttribute("message", "Ticket introuvable !");
                return "payment-ticket";
            }
            model.addAttribute("username", username);
            model.addAttribute("role", role);
            model.addAttribute("payment", payment);
            model.addAttribute("gatewayUrl", gatewayUrl);
            model.addAttribute("token", token);

        return "payment-ticket";
    }


    // --------------------------
    // 7️⃣ Méthode utilitaire
    // -------------------------
    private void updateOrderStatus(Long orderId, String status, String token) {
        try {
            String url = gatewayUrl + "/order-service/api/orders/" + orderId
                    + "/status?status=" + status
                    + "&token=" + token;
            restTemplate.postForObject(url, null, String.class);
        } catch (Exception e) {
            System.out.println("Order-service indisponible : " + e.getMessage());
        }
    }

}
