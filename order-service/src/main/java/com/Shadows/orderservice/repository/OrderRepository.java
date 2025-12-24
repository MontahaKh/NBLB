package com.Shadows.orderservice.repository;

import jakarta.persistence.metamodel.SingularAttribute;
import com.Shadows.orderservice.model.Order;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUsername(String username);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.products p WHERE p.addedBy = :username")
    List<Order> findOrdersByProductOwner(@Param("username") String username);

    List<Order> id(long id);

    Optional<Order> findById(SingularAttribute<AbstractPersistable, Serializable> id);

    void deleteById(SingularAttribute<AbstractPersistable, Serializable> id);
}