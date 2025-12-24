package com.Shadows.orderservice.Service;

import com.Shadows.orderservice.model.Cart;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.repository.CartRepository;
import com.Shadows.orderservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartServiceImp implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Cart getCartByUsername(String username) {
        return cartRepository.findByUsername(username)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUsername(username);
                    return cartRepository.save(cart);
                });
    }

    @Override
    public Cart addToCart(String username, Long productId) {
        Cart cart = getCartByUsername(username);
        Optional<Product> product = productRepository.findById(productId);
        product.ifPresent(cart::addProduct);
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeFromCart(String username, Long productId) {
        Cart cart = getCartByUsername(username);
        Optional<Product> product = productRepository.findById(productId);
        product.ifPresent(cart::removeProduct);
        return cartRepository.save(cart);
    }

    @Override
    public void clearCart(String username) {
        Cart cart = getCartByUsername(username);
        cart.getProducts().clear();
        cartRepository.save(cart);
    }
}
