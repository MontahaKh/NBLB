package com.Shadows.orderservice.Service;

import com.Shadows.orderservice.model.Order;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.repository.OrderRepository;
import com.Shadows.orderservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImp implements ProductService{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    // Créer un nouveau produit
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    @Override
    // Récupérer tous les produits
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    @Override
    // Récupérer un produit par ID
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    @Override
    // Mettre à jour un produit
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
    @Override
    @Transactional
    // Supprimer un produit par ID
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            List<Order> orders = product.getOrders();
            if (orders != null) {
                for (Order order : orders) {
                    order.getProducts().remove(product);
                    orderRepository.save(order);
                }
            }
            productRepository.deleteById(id);
        }
    }

    @Override
    public List<Product> getProductsByAddedBy(String addedBy) {
        return productRepository.findByAddedBy(addedBy);
    }
}
