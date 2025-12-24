package com.Shadows.orderservice.Flyweight;

import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.model.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderFactory {

    private static final Map<String, Order> orderCache = new HashMap<>();

    public static Order getOrder(Product product, int quantity) {
        String key = product.getId() + "-" + quantity;

        if (!orderCache.containsKey(key)) {
            Order newOrder = new Order();
            List<Product> products = new ArrayList<>();
            products.add(product);
            newOrder.setProducts(products);
            newOrder.setQuantity(quantity);
            orderCache.put(key, newOrder);
            System.out.println("✅ Nouvelle instance Order créée pour : " + key);
        } else {
            System.out.println("♻️ Réutilisation de l’instance existante pour : " + key);
        }

        return orderCache.get(key);
    }
}