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
            String encodedPassword = passwordEncoder.encode("Anil@3616");

            // Seed / Update Admin Account
            User admin = userRepository.findByEmail("admin@bmart.com")
                .orElseGet(() -> User.builder().email("admin@bmart.com").username("admin").build());
            admin.setPassword(encodedPassword);
            admin.setRole("ROLE_ADMIN");
            admin.setStatus("ACTIVE");
            admin.setVerified(true);
            admin.setFullName("B-MART System Admin");
            admin.setPhoneNumber("9999999999");
            userRepository.save(admin);

            // Seed / Update Customer Account
            User customer = userRepository.findByEmail("anilworks321@gmail.com")
                .orElseGet(() -> User.builder().email("anilworks321@gmail.com").username("anil123").build());
            customer.setPassword(encodedPassword);
            customer.setRole("ROLE_USER");
            customer.setStatus("ACTIVE");
            customer.setVerified(true);
            customer.setFullName("Anil Hosalli");
            customer.setPhoneNumber("8431811670");
            userRepository.save(customer);
        };
    }
}
