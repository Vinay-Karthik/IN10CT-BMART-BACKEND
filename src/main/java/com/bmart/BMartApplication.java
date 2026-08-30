package com.bmart;

import com.bmart.entity.User;
import com.bmart.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableCaching
public class BMartApplication {

    public static void main(String[] args) {
        SpringApplication.run(BMartApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String encodedPassword = passwordEncoder.encode("Admin@123");

            // Seed or update Admin Account with Admin@123 password
            var existingAdminOpt = userRepository.findByEmailOrUsername("admin@bmart.com", "admin");
            if (existingAdminOpt.isEmpty()) {
                User admin = User.builder()
                        .email("admin@bmart.com")
                        .username("admin")
                        .password(encodedPassword)
                        .role("ADMIN")
                        .status("ACTIVE")
                        .isVerified(true)
                        .fullName("B-MART System Admin")
                        .phoneNumber("9999999999")
                        .build();
                userRepository.save(admin);
            } else {
                User admin = existingAdminOpt.get();
                admin.setPassword(encodedPassword);
                userRepository.save(admin);
            }

            // Ensure all existing user accounts have an active status if missing
            userRepository.findAll().forEach(u -> {
                boolean changed = false;
                if (u.getStatus() == null || u.getStatus().isEmpty()) {
                    u.setStatus("ACTIVE");
                    changed = true;
                }
                if (changed) {
                    userRepository.save(u);
                }
            });
        };
    }
}
