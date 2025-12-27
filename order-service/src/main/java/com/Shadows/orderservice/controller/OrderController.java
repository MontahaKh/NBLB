package com.Shadows.orderservice.controller;

import com.Shadows.orderservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import com.Shadows.orderservice.model.Order;
import com.Shadows.orderservice.Service.OrderServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;



@Controller
@RequestMapping("/order-service")
public class OrderController {

    @Autowired
    private OrderServiceImp orderService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${gateway.url}")
    private String gatewayUrl;

    @RequestMapping("/allorder")
    public String allOrders(Model model, @RequestParam("token") String token){
        model.addAttribute("gatewayUrl", gatewayUrl);
        model.addAttribute("token", token);
        List<Order> listOrders;

        if (token != null && jwtUtil.validateToken(token)) {
            String role = jwtUtil.extractRole(token);
            String username = jwtUtil.extractUsername(token);
            model.addAttribute("userRole", role);
            model.addAttribute("username", username);

            if ("CLIENT".equals(role)) {
                listOrders = orderService.getOrdersByUsername(username);
            } else if ("ADMIN".equals(role)) {
                listOrders = orderService.getOrders();
            } else {
                listOrders = orderService.getOrdersByProductOwner(username);
            }
        } else {
            listOrders = orderService.getOrders();
        }

        model.addAttribute("listOrders", listOrders);
        return "List_orders";
    }


    @RequestMapping("/send/{id}")
    public String sendOrder(@PathVariable Long id, @RequestParam("token") String token) {
        if (token != null && jwtUtil.validateToken(token)) {
            String role = jwtUtil.extractRole(token);
            if (!"CLIENT".equals(role)) {
                java.util.Optional<Order> orderOpt = orderService.getOrderById(id);
                if (orderOpt.isPresent() && "PAID".equals(orderOpt.get().getStatus())) {
                    orderService.updateOrderStatus(id, "SHIPPED");
                }
            }
        }
        return "redirect:/order-service/allorder?token=" + token;
    }

    @RequestMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable Long id, @RequestParam("token") String token,Model model) {

        if (token != null && jwtUtil.validateToken(token)) {
             orderService.updateOrderStatus(id, "CANCELLED");
             model.addAttribute("token", token);
        }
        return "redirect:"+gatewayUrl+"/order-service/allorder?token=" + token;
    }

    @RequestMapping(value = "/api/orders/{id}/status", method = {RequestMethod.POST, RequestMethod.GET})
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<String> updateOrderStatusApi(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring("Bearer ".length()).trim();
        }
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing/invalid token");
        }

        java.util.Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
        }

        Order order = orderOpt.get();
        String role = String.valueOf(jwtUtil.extractRole(token));
        String username = String.valueOf(jwtUtil.extractUsername(token));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isOwner = username != null && username.equalsIgnoreCase(order.getUsername());

        // Minimal authorization rules:
        // - owner can set PAID / WAITING_DELIVERY / CANCELLED
        // - admin can set any status
        if (!isAdmin) {
            String s = String.valueOf(status).toUpperCase(java.util.Locale.ROOT);
            boolean allowed = isOwner && (s.equals("PAID") || s.equals("WAITING_DELIVERY") || s.equals("CANCELLED"));
            if (!allowed) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
            }
        }

        orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok("Status updated");
    }
}