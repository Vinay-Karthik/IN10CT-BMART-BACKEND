# B-MART E-Commerce Backend Service

Spring Boot REST API backend service for B-MART, an Amazon.in-style full-stack e-commerce platform specializing in bag collections (Backpacks, Handbags, Travel Bags, Wallets).

---

## Technical Features
- **Framework:** Spring Boot 3.2.5 (Java 17 / 21)
- **Database:** MySQL 8.0 (`ecommerce` database)
- **Security:** Spring Security, BCrypt Password Hashing, JJWT (JSON Web Token) Auth & Authorization Filters
- **Authentication:** Email OTP, SMS OTP (MSG91 / Twilio placeholders), Password Reset & Token Refresh
- **Catalog & Search:** Multi-attribute filtering (Category, Price Range, Brand, Rating) with Pagination & Sorting
- **Payments:** Razorpay Integration (Order generation, HMAC-SHA256 signature verification, Webhook handler)
- **Orders & Status Pipeline:** `PLACED` → `CONFIRMED` → `SHIPPED` → `OUT_FOR_DELIVERY` → `DELIVERED` / `CANCELLED`
- **Wishlist & Reviews:** Live rating & review count recalculation, Wishlist toggle
- **Notifications:** In-app notifications & Firebase Cloud Messaging (FCM) outline

---

## Setup & Running Instructions

### 1. Database Setup
Ensure MySQL Server 8.0 is running on `localhost:3306`.
Execute the schema and seed scripts:
```bash
mysql -u root -p ecommerce < src/main/resources/schema.sql
mysql -u root -p ecommerce < src/main/resources/seed.sql
```

### 2. Configuration
Review `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce
spring.datasource.username=root
spring.datasource.password=Anil@3616
```

### 3. Build & Run
```bash
mvn clean compile
mvn spring-boot:run
```
The REST API server will start on `http://localhost:8080`.
