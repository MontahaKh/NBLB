package com.Shadows.recommendationservice.client;

import com.Shadows.recommendationservice.config.FeignConfig;
import com.Shadows.recommendationservice.model.OrderSummaryDto;
import com.Shadows.recommendationservice.model.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "order-service", url = "${order-service.url:http://localhost:8091}", configuration = FeignConfig.class)
public interface OrderServiceClient {

    @GetMapping("/order-service/api/orders/me")
    List<OrderSummaryDto> getMyOrders();

    @GetMapping("/order-service/products")
    List<ProductDto> getProducts();
}
