package com.Shadows.recommendationservice.model;

import java.util.List;

public class OrderSummaryDto {
    private Long id;
    private String orderDate;
    private double total;
    private String status;
    private List<String> productNames;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getProductNames() { return productNames; }
    public void setProductNames(List<String> productNames) { this.productNames = productNames; }
}
