package com.Shadows.orderservice.Service;


import com.Shadows.orderservice.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    Product createProduct(Product product);

    List<Product> getAllProducts();

    Optional<Product> getProductById(Long id);

    Product updateProduct(Product product);

    void deleteProduct(Long id);

    List<Product> getProductsByAddedBy(String addedBy);
}
