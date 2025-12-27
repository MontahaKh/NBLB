package com.Shadows.orderservice.repository;

import com.Shadows.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUsername(String username);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.products p WHERE p.addedBy = :username")
    List<Order> findOrdersByProductOwner(@Param("username") String username);
}