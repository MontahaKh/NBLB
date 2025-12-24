package com.Shadows.orderservice.controller;

import com.Shadows.orderservice.model.Category;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.model.ProductStatus;
import com.Shadows.orderservice.Service.ProductServiceImp;
import com.Shadows.orderservice.util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.sql.Date;
import java.util.List;

@Controller
public class ProductController {
    @Autowired
    private ProductServiceImp productService; // Ajout de 'private' pour bonne pratique

    @Autowired
    private JwtUtil jwtUtil;

    @org.springframework.beans.factory.annotation.Value("${gateway.url}")
    private String gatewayUrl;

    @RequestMapping("/order-service/addProduct")
    public String addProduct(Model model, HttpSession session) {
        model.addAttribute("gatewayUrl", gatewayUrl);
        String token = (String) session.getAttribute("jwtToken");
        if (token != null && jwtUtil.validateToken(token)) {
            model.addAttribute("userRole", jwtUtil.extractRole(token));
            model.addAttribute("username", jwtUtil.extractUsername(token));
        }
        
        Product product = new Product();
        model.addAttribute("productform", product); // Méthode correcte avec import approprié
        model.addAttribute("categories", Category.values());
        return "new_product";
    }

    @RequestMapping("/order-service/edit/{id}")
    public String editProduct(@PathVariable("id") Long id, Model model, HttpSession session) {
        model.addAttribute("gatewayUrl", gatewayUrl);
        String token = (String) session.getAttribute("jwtToken");
        if (token != null && jwtUtil.validateToken(token)) {
            model.addAttribute("userRole", jwtUtil.extractRole(token));
            model.addAttribute("username", jwtUtil.extractUsername(token));
        }

        Product product = productService.getProductById(id).orElse(null);
        if (product == null) {
            return "redirect:/order-service/list_product";
        }

        model.addAttribute("productform", product);
        model.addAttribute("categories", Category.values());
        return "new_product";
    }

    @RequestMapping("/order-service/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        return "redirect:/order-service/list_product";
    }

    @RequestMapping(value = "/order-service/save", method = RequestMethod.POST)
    public String saveProduct(@ModelAttribute("productform") Product product, HttpSession session) { // Correction de "ProductForm" à "productform"
        String token = (String) session.getAttribute("jwtToken");
        String username = null;
        if (token != null && jwtUtil.validateToken(token)) {
            username = jwtUtil.extractUsername(token);
        }

        if (product.getId() != null) {
            // Update: preserve existing fields not in form
            Product existing = productService.getProductById(product.getId()).orElse(null);
            if (existing != null) {
                product.setCreatedAt(existing.getCreatedAt());
                product.setAddedBy(existing.getAddedBy());
                if (product.getStatus() == null) product.setStatus(existing.getStatus());
            }
        } else {
            // Create: set initial fields
            product.setCreatedAt(new Date(System.currentTimeMillis()));
            if (product.getStatus() == null) {
                product.setStatus(ProductStatus.AVAILABLE);
            }
            if (username != null) {
                product.setAddedBy(username);
            }
        }

        productService.createProduct(product);
        return "redirect:/order-service/list_product";
    }
    @RequestMapping("/order-service/list_product")
    public String listProducts(Model model, HttpSession session) {
        model.addAttribute("gatewayUrl", gatewayUrl);
        String token = (String) session.getAttribute("jwtToken");
        List<Product> listProducts;

        if (token != null && jwtUtil.validateToken(token)) {
            String role = jwtUtil.extractRole(token);
            String username = jwtUtil.extractUsername(token);
            
            model.addAttribute("userRole", role);
            model.addAttribute("username", username);
            
            if ("SHOP".equals(role)) {
                // Shop owners only see their own products
                listProducts = productService.getProductsByAddedBy(username);
            } else {
                // Admin or others might see all (or handle differently)
                // For now, default to all for non-shop users, or restrict as needed
                listProducts = productService.getAllProducts();
            }
        } else {
            // Not logged in or invalid token
            return "redirect:/auth/login";
        }

        model.addAttribute("listProducts", listProducts);
        return "list_product";
    }
}