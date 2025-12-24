package com.Shadows.orderservice.repository;

import com.Shadows.orderservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByAddedBy(String addedBy);
}


