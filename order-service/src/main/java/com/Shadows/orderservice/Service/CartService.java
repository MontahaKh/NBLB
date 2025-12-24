package com.Shadows.orderservice.Service;

import com.Shadows.orderservice.model.Cart;

public interface CartService {
    Cart getCartByUsername(String username);
    Cart addToCart(String username, Long productId);
    Cart removeFromCart(String username, Long productId);
    void clearCart(String username);
}
