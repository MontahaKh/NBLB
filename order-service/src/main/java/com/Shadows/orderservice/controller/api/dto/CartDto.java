package com.Shadows.orderservice.controller.api.dto;

import java.util.List;

public record CartDto(
        Long id,
        String username,
        List<ProductDto> products
) {
}
