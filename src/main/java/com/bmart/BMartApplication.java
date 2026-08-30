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
            PasswordEncoder passwordEncoder
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

                // 2. Seed Default Categories if empty
                Category handbags = categoryRepository.findByCategoryName("Handbags & Purses")
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryName("Handbags & Purses").description("Luxury Handbags & Purses").build()));
                Category travel = categoryRepository.findByCategoryName("Backpacks & Travel")
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryName("Backpacks & Travel").description("Travel Bags & Backpacks").build()));
                Category tech = categoryRepository.findByCategoryName("Tech & Laptop Bags")
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryName("Tech & Laptop Bags").description("Laptop Bags & Tech Cases").build()));
                Category fashion = categoryRepository.findByCategoryName("Fashion Accessories")
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryName("Fashion Accessories").description("Belts & Accessories").build()));

                // 3. Seed Catalog Products if empty
                if (productRepository.count() == 0) {
                    List<Product> seedProducts = List.of(
                            Product.builder()
                                    .name("Dior Medium Lady D-Lite Bag")
                                    .description("Cannage embroidery luxury handbag crafted in premium fabric with pale gold-finish metal charms.")
                                    .price(new BigDecimal("4500.00"))
                                    .stock(25)
                                    .imageUrl("https://images.unsplash.com/photo-1584917865442-de89df76afd3?q=80&w=800&auto=format&fit=crop")
                                    .brand("Christian Dior")
                                    .category(handbags)
                                    .status("APPROVED")
                                    .build(),
                            Product.builder()
                                    .name("Gucci GG Marmont Matelassé Shoulder Bag")
                                    .description("Iconic matelassé chevron leather shoulder bag featuring Double G emblem hardware.")
                                    .price(new BigDecimal("3800.00"))
                                    .stock(18)
                                    .imageUrl("https://images.unsplash.com/photo-1591561954557-26941169b49e?q=80&w=800&auto=format&fit=crop")
                                    .brand("Gucci")
                                    .category(handbags)
                                    .status("APPROVED")
                                    .build(),
                            Product.builder()
                                    .name("Louis Vuitton Neverfull MM Tote")
                                    .description("Timeless Monogram canvas tote with natural cowhide leather trim and side laces.")
                                    .price(new BigDecimal("5200.00"))
                                    .stock(15)
                                    .imageUrl("https://images.unsplash.com/photo-1548036328-c9fa89d128fa?q=80&w=800&auto=format&fit=crop")
                                    .brand("Louis Vuitton")
                                    .category(handbags)
                                    .status("APPROVED")
                                    .build(),
                            Product.builder()
                                    .name("Prada Re-Edition Nylon Shoulder Bag")
                                    .description("Sleek black Re-Nylon shoulder bag with enamel triangle logo and chain handle.")
                                    .price(new BigDecimal("2900.00"))
                                    .stock(30)
                                    .imageUrl("https://images.unsplash.com/photo-1566150905458-1bf1fc113f0d?q=80&w=800&auto=format&fit=crop")
                                    .brand("Prada")
                                    .category(handbags)
                                    .status("APPROVED")
                                    .build(),
                            Product.builder()
                                    .name("Executive Leather Tech Backpack")
                                    .description("Water-resistant full-grain leather backpack with padded 16-inch laptop compartment.")
                                    .price(new BigDecimal("3200.00"))
                                    .stock(20)
                                    .imageUrl("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?q=80&w=800&auto=format&fit=crop")
                                    .brand("B-MART Executive")
                                    .category(tech)
                                    .status("APPROVED")
                                    .build(),
                            Product.builder()
                                    .name("Classic Weekender Travel Duffle")
                                    .description("Spacious canvas and leather weekender duffle bag with detachable shoulder strap.")
                                    .price(new BigDecimal("4100.00"))
                                    .stock(12)
                                    .imageUrl("https://images.unsplash.com/photo-1547949003-9792a18a2601?q=80&w=800&auto=format&fit=crop")
                                    .brand("B-MART Travel")
                                    .category(travel)
                                    .status("APPROVED")
                                    .build()
                    );
                    productRepository.saveAll(seedProducts);
                    System.out.println(">>> B-MART Seeded " + seedProducts.size() + " products successfully!");
                }
            } catch (Exception e) {
                System.err.println("WARN: Database seeding encountered non-fatal error: " + e.getMessage());
            }
        };
    }
}
