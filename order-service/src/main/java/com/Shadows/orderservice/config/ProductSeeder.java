package com.Shadows.orderservice.config;

import com.Shadows.orderservice.model.Category;
import com.Shadows.orderservice.model.Product;
import com.Shadows.orderservice.model.ProductStatus;
import com.Shadows.orderservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;

@Component
public class ProductSeeder implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // Check if products already exist
        if (productRepository.count() == 0) {
            seedProducts();
        } else {
            System.out.println("✓ Products already exist");
        }
    }

    private void seedProducts() {
        // Define 4 sellers with 3 products each
        String[] sellers = { "freshmart", "meatking", "bakerydelights", "dairyplus" };

        // Seller 1: freshmart - FRESH_PRODUCE
        createProduct("Organic Tomatoes", 4.99, "Fresh organic tomatoes from local farms", 100,
                Category.FRESH_PRODUCE, "freshmart", "https://via.placeholder.com/300?text=Tomatoes");
        createProduct("Crisp Lettuce", 3.49, "Iceberg lettuce, fresh and crunchy", 150,
                Category.FRESH_PRODUCE, "freshmart", "https://via.placeholder.com/300?text=Lettuce");
        createProduct("Bell Peppers Mix", 5.99, "Red, yellow, and green bell peppers", 80,
                Category.FRESH_PRODUCE, "freshmart", "https://via.placeholder.com/300?text=Peppers");

        // Seller 2: meatking - MEAT_AND_SEAFOOD
        createProduct("Prime Beef Steak", 15.99, "Premium grass-fed beef steaks", 50,
                Category.MEAT_AND_SEAFOOD, "meatking", "https://via.placeholder.com/300?text=Beef");
        createProduct("Fresh Atlantic Salmon", 18.99, "Wild-caught fresh salmon fillets", 40,
                Category.MEAT_AND_SEAFOOD, "meatking", "https://via.placeholder.com/300?text=Salmon");
        createProduct("Chicken Breast", 8.99, "Boneless, skinless chicken breasts", 120,
                Category.MEAT_AND_SEAFOOD, "meatking", "https://via.placeholder.com/300?text=Chicken");

        // Seller 3: bakerydelights - BAKED_GOODS
        createProduct("Artisan Sourdough Bread", 6.49, "Fresh baked sourdough with a crispy crust", 30,
                Category.BAKED_GOODS, "bakerydelights", "https://via.placeholder.com/300?text=Sourdough");
        createProduct("Croissants Pack", 7.99, "Pack of 6 buttery croissants", 45,
                Category.BAKED_GOODS, "bakerydelights", "https://via.placeholder.com/300?text=Croissants");
        createProduct("Chocolate Chip Cookies", 4.49, "Homemade chocolate chip cookies (dozen)", 60,
                Category.BAKED_GOODS, "bakerydelights", "https://via.placeholder.com/300?text=Cookies");

        // Seller 4: dairyplus - DAIRY_PRODUCTS
        createProduct("Greek Yogurt", 5.99, "Plain Greek yogurt, 32oz container", 70,
                Category.DAIRY_PRODUCTS, "dairyplus", "https://via.placeholder.com/300?text=Yogurt");
        createProduct("Cheddar Cheese Block", 7.49, "Aged sharp cheddar cheese, 1lb block", 50,
                Category.DAIRY_PRODUCTS, "dairyplus", "https://via.placeholder.com/300?text=Cheese");
        createProduct("Organic Whole Milk", 4.49, "Organic whole milk, 1 gallon", 100,
                Category.DAIRY_PRODUCTS, "dairyplus", "https://via.placeholder.com/300?text=Milk");

        System.out.println("✓ 12 products created (3 per seller)");
    }

    private void createProduct(String name, Double price, String description, Integer quantity,
            Category category, String addedBy, String imageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setDescription(description);
        product.setQuantity(quantity);
        product.setCategory(category);
        product.setStatus(ProductStatus.AVAILABLE);
        product.setAddedBy(addedBy);
        product.setImageUrl(imageUrl);
        product.setCreatedAt(Date.valueOf(LocalDate.now()));
        product.setExpieryDate(Date.valueOf(LocalDate.now().plusDays(30)));

        productRepository.save(product);
    }
}
