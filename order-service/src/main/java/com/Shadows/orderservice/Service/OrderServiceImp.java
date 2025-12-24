package com.Shadows.orderservice.Service;

import com.Shadows.orderservice.model.Order;
import com.Shadows.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
public class OrderServiceImp implements OrderService{
    @Autowired
    private OrderRepository orderRepository;
    @Override
    public Order createOrder(Order order) {
        return  orderRepository.save(order);
    }

    @Override
    public List<Order> getOrders() {
        return orderRepository.findAll();

    }

    @Override
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);

    }

    @Override
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id) ;

    }

    @Override
    public List<Order> getOrdersByUsername(String username) {
        return orderRepository.findByUsername(username);
    }

    @Override
    public List<Order> getOrdersByProductOwner(String username) {
        return orderRepository.findOrdersByProductOwner(username);
    }

    @Override
    public void updateOrderStatus(Long id, String status) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isPresent()) {
            Order o = order.get();
            o.setStatus(status);
            orderRepository.save(o);
        }
    }
}