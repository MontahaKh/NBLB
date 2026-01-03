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
        // Check if admin user already exists
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
}
