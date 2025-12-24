package com.Shadows.orderservice.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.sql.Date;
import java.util.List;

@Data
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double price;
    private String description;
    private Integer quantity;
    private String imageUrl;
    @Enumerated(EnumType.STRING)
    private Category category;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    private String addedBy;
    private Date createdAt;
    private Date expieryDate;



    @ManyToMany(mappedBy = "products")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List< Order> orders;


}
