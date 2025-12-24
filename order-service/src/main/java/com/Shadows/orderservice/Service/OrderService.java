package com.Shadows.orderservice.Service;

import com.Shadows.orderservice.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    Order createOrder(Order order);
    List<Order> getOrders();
    Optional<Order> getOrderById(Long orderId);
    Order updateOrder(Order order);
    void deleteOrder(Long id);
    List<Order> getOrdersByUsername(String username);
    List<Order> getOrdersByProductOwner(String username);
    void updateOrderStatus(Long id, String status);
}
