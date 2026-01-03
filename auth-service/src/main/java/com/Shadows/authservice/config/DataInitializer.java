package com.Shadows.authservice.config;

import com.Shadows.authservice.model.Role;
import com.Shadows.authservice.model.User;
import com.Shadows.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${spring.security.user.name}")
    private String adminUsername;

    @Value("${spring.security.user.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user
        createAdminUser();

        // Create seed sellers
        createSeedSellers();
    }

    private void createAdminUser() {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setEmail("admin@nblb.com");
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole(Role.ADMIN);

            userRepository.save(adminUser);
            System.out.println("✓ Default admin user created: " + adminUsername + " / " + adminPassword);
        } else {
            System.out.println("✓ Admin user already exists");
        }
    }

    private void createSeedSellers() {
        // Seed data for 4 sellers
        String[][] sellerData = {
                { "freshmart", "freshmart@nblb.com", "password123" },
                { "meatking", "meatking@nblb.com", "password123" },
                { "bakerydelights", "bakerydelights@nblb.com", "password123" },
                { "dairyplus", "dairyplus@nblb.com", "password123" }
        };

        for (String[] data : sellerData) {
            String username = data[0];
            String email = data[1];
            String password = data[2];

            if (userRepository.findByUsername(username).isEmpty()) {
                User seller = new User();
                seller.setUsername(username);
                seller.setEmail(email);
                seller.setPassword(passwordEncoder.encode(password));
                seller.setRole(Role.SHOP);

                userRepository.save(seller);
                System.out.println("✓ Seller created: " + username + " / " + password);
            } else {
                System.out.println("✓ Seller already exists: " + username);
            }
        }
    }
}
