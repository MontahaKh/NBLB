package com.Shadows.orderservice.controller;

import com.Shadows.orderservice.Service.CartService;
import com.Shadows.orderservice.Service.OrderServiceImp;
import com.Shadows.orderservice.model.Cart;
import com.Shadows.orderservice.model.Order;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@Controller
@RequestMapping("/order-service/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderServiceImp orderService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${gateway.url}")
    private String gatewayUrl;

    @GetMapping
    public String viewCart(Model model,  @RequestParam("token") String token) {
        model.addAttribute("gatewayUrl", gatewayUrl);
        model.addAttribute("token", token);
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            model.addAttribute("username", username);
            model.addAttribute("userRole", role);
            
            Cart cart = cartService.getCartByUsername(username);
            model.addAttribute("cart", cart);
            model.addAttribute("token",token);
            
            double total = cart.getProducts().stream()
                    .mapToDouble(Product::getPrice)
                    .sum();
            model.addAttribute("totalPrice", total);
            
            return "cart";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/add/{id}")
    public String addToCart(@PathVariable Long id, @RequestParam("token") String token)  {

        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            cartService.addToCart(username, id);

            return "redirect:" +gatewayUrl+"/order-service/cart?token="+token;
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Long id, HttpSession session) {
        String token = (String) session.getAttribute("jwtToken");
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            cartService.removeFromCart(username, id);
        }
        return "redirect:"+gatewayUrl+"/order-service/cart?token=" + token;
    }


    @GetMapping("/checkout")
    public String checkout(@RequestParam("token") String token) {

        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            Cart cart = cartService.getCartByUsername(username);
            
            if (!cart.getProducts().isEmpty()) {
                Order order = new Order();
                order.setDate(new Date());
                order.setRef((int) (Math.random() * 100000));
                
                double total = cart.getProducts().stream()
                        .mapToDouble(Product::getPrice)
                        .sum();
                order.setPrice(String.valueOf(total));
                order.setQuantity(cart.getProducts().size());
                order.setUsername(username);
                
                order.getProducts().addAll(cart.getProducts());
                
                orderService.createOrder(order);
                cartService.clearCart(username);
            }
            return "redirect:"  +gatewayUrl+ "/order-service/allorder?token=" + token;
        }
        return "redirect:/auth/login";
    }
}
