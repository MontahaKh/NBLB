package com.Shadows.orderservice.controller.api.dto;

import com.Shadows.orderservice.model.Category;
import com.Shadows.orderservice.model.ProductStatus;

import java.sql.Date;

public record ProductDto(
        Long id,
        String name,
        Double price,
        String description,
        Integer quantity,
        String imageUrl,
        Category category,
        ProductStatus status,
        String addedBy,
        Date createdAt,
        Date expieryDate
) {
}
