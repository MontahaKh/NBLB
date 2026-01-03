package com.Shadows.orderservice.model;

import jakarta.persistence.*;
import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Table(name = "orders")
@Entity
@Data

public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int ref;
    private String price;
    private int quantity;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date;

    private String username;
    private String status = "PENDING";

    @ManyToMany
    @JoinTable(name = "order_products", joinColumns = @JoinColumn(name = "order_id"), inverseJoinColumns = @JoinColumn(name = "product_id"))
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<Product> products = new ArrayList<>();

    /**
     * Get order date (alias for 'date' field for compatibility)
     */
    public Date getOrderDate() {
        return this.date;
    }

    /**
     * Get total amount as double
     */
    public double getTotal() {
        try {
            return Double.parseDouble(price != null ? price : "0.0");
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

}