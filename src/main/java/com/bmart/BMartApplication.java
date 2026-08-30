package com.bmart;

import com.bmart.entity.Category;
import com.bmart.entity.Product;
import com.bmart.entity.User;
import com.bmart.repository.CategoryRepository;
import com.bmart.repository.ProductRepository;
import com.bmart.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootApplication
@EnableCaching
public class BMartApplication {

    public static void main(String[] args) {
        SpringApplication.run(BMartApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder,
            DataSource dataSource
    ) {
        return args -> {
            try {
                String encodedPassword = passwordEncoder.encode("Admin@123");

                // 1. Seed Admin Account
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

                // 2. Seed & Update Categories (IDs 1, 2, 3, 4) to match DB records exactly
                Category travel = categoryRepository.findById(1L)
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryId(1L).categoryName("Backpacks").description("Backpacks & Laptop Bags").build()));
                travel.setCategoryName("Backpacks");
                categoryRepository.save(travel);

                Category handbags = categoryRepository.findById(2L)
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryId(2L).categoryName("Handbags").description("Luxury Handbags").build()));
                handbags.setCategoryName("Handbags");
                categoryRepository.save(handbags);

                Category tech = categoryRepository.findById(3L)
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryId(3L).categoryName("Travel Bags").description("Travel Bags & Duffles").build()));
                tech.setCategoryName("Travel Bags");
                categoryRepository.save(tech);

                Category fashion = categoryRepository.findById(4L)
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryId(4L).categoryName("Wallets").description("Wallets & Accessories").build()));
                fashion.setCategoryName("Wallets");
                categoryRepository.save(fashion);

                // 3. Auto-import all 4 SQL product files (backpacks, handbags, travelbags, wallets)
                try {
                    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                    populator.setContinueOnError(true);
                    populator.addScript(new ClassPathResource("insert_backpacks.sql"));
                    populator.addScript(new ClassPathResource("insert_handbags.sql"));
                    populator.addScript(new ClassPathResource("insert_travelbags.sql"));
                    populator.addScript(new ClassPathResource("insert_wallets.sql"));
                    populator.execute(dataSource);
                    System.out.println(">>> B-MART Auto-Imported all SQL products successfully!");
                } catch (Exception popErr) {
                    System.err.println("WARN: Could not execute SQL scripts: " + popErr.getMessage());
                }
            } catch (Exception e) {
                System.err.println("WARN: Database seeding encountered non-fatal error: " + e.getMessage());
            }
        };
    }
}
