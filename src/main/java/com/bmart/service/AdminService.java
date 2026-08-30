package com.bmart.service;

import com.bmart.dto.AdminProductRequest;
import com.bmart.dto.AdminUserUpdateRequest;
import com.bmart.entity.*;
import com.bmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Year;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    // --- PRODUCT MANAGEMENT ---
    @Transactional
    public Product addProduct(String adminEmail, AdminProductRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Product description is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        if (request.getEffectiveStock() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        // Validate Category from predefined lookup list
        Category category = resolveCategory(request);
        if (category == null) {
            throw new IllegalArgumentException("Invalid category: Category does not exist in predefined category list");
        }

        // Check Duplicate Product (same name + category)
        boolean isDuplicate = productRepository.existsByNameIgnoreCaseAndCategoryCategoryId(request.getName().trim(), category.getCategoryId());
        if (isDuplicate) {
            throw new IllegalStateException("Duplicate product rejected: Product with name '" + request.getName().trim() + "' already exists in category '" + category.getCategoryName() + "'");
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stock(request.getEffectiveStock())
                .category(category)
                .brand(request.getBrand() != null ? request.getBrand().trim() : "B-MART")
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : "https://images.unsplash.com/photo-1544816155-12df9643f363?w=500")
                .tags(request.getTags() != null ? request.getTags() : "FEATURED")
                .status("APPROVED")
                .rating(4.5)
                .reviewCount(0)
                .build();

        Product saved = productRepository.save(product);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("ADD_PRODUCT")
                .targetType("PRODUCT")
                .targetId(String.valueOf(saved.getProductId()))
                .reason("Admin added new product: " + saved.getName())
                .build());

        return saved;
    }

    private Category resolveCategory(AdminProductRequest request) {
        if (request.getCategoryId() != null) {
            return categoryRepository.findById(request.getCategoryId()).orElse(null);
        }
        if (request.getCategoryName() != null && !request.getCategoryName().trim().isEmpty()) {
            return categoryRepository.findByCategoryNameIgnoreCase(request.getCategoryName().trim())
                    .orElseGet(() -> categoryRepository.findBySlug(request.getCategoryName().trim().toLowerCase()).orElse(null));
        }
        if (request.getCategory() != null) {
            String catVal = String.valueOf(request.getCategory()).trim();
            try {
                Long catId = Long.parseLong(catVal);
                return categoryRepository.findById(catId).orElse(null);
            } catch (NumberFormatException e) {
                return categoryRepository.findByCategoryNameIgnoreCase(catVal)
                        .orElseGet(() -> categoryRepository.findBySlug(catVal.toLowerCase()).orElse(null));
            }
        }
        return null;
    }

    @Transactional
    public void deleteProduct(String adminEmail, Long productId, boolean confirm) {
        if (!confirm) {
            throw new IllegalArgumentException("Deletion confirmation required before deleting product. Pass confirm=true parameter.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with ID: " + productId));

        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (Exception e) {
            // Product referenced in existing order history or cart/wishlist. Mark status as DELETED for safe soft deletion.
            product.setStatus("DELETED");
            productRepository.save(product);
        }

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("DELETE_PRODUCT")
                .targetType("PRODUCT")
                .targetId(String.valueOf(productId))
                .reason("Product deleted/archived by admin after confirmation")
                .build());
    }

    // --- USER MANAGEMENT ---
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User updateUser(String adminEmail, Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            throw new IllegalArgumentException("Valid email address is required");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            user.setRole(request.getRole().trim().toUpperCase());
        }

        if (request.getFullName() != null) user.setFullName(request.getFullName().trim());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber().trim());
        if (request.getStatus() != null) user.setStatus(request.getStatus().trim().toUpperCase());

        User saved = userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("MODIFY_USER")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason("Admin updated user details for " + saved.getUsername())
                .build());

        return saved;
    }

    @Transactional
    public User suspendUser(String adminEmail, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        user.setStatus("SUSPENDED");
        User saved = userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("SUSPEND_USER")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason(reason != null ? reason : "Suspended by admin")
                .build());

        return saved;
    }

    @Transactional
    public User banUser(String adminEmail, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        user.setStatus("BANNED");
        User saved = userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("BAN_USER")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason(reason != null ? reason : "Banned by admin for policy violation")
                .build());

        return saved;
    }

    @Transactional
    public User activateUser(String adminEmail, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        user.setStatus("ACTIVE");
        User saved = userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("ACTIVATE_USER")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason("Account restored to active status by admin")
                .build());

        return saved;
    }

    @Transactional
    public User changeUserRole(String adminEmail, Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        user.setRole(newRole.trim().toUpperCase());
        User saved = userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("CHANGE_USER_ROLE")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason("Promoted/Changed role to " + newRole)
                .build());

        return saved;
    }

    // --- PRODUCT MODERATION & CATEGORIES ---
    public List<Product> getPendingProducts() {
        return productRepository.findByStatus("PENDING");
    }

    @Transactional
    public Product approveProduct(String adminEmail, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with ID: " + productId));

        product.setStatus("APPROVED");
        Product saved = productRepository.save(product);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("APPROVE_PRODUCT")
                .targetType("PRODUCT")
                .targetId(String.valueOf(productId))
                .reason("Approved product listing")
                .build());

        return saved;
    }

    @Transactional
    public Product banProduct(String adminEmail, Long productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with ID: " + productId));

        product.setStatus("BANNED");
        Product saved = productRepository.save(product);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("BAN_PRODUCT")
                .targetType("PRODUCT")
                .targetId(String.valueOf(productId))
                .reason(reason != null ? reason : "Banned for policy violation")
                .build());

        return saved;
    }

    @Transactional
    public void deleteUser(String adminEmail, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));
        userRepository.delete(user);
        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("DELETE_USER")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason("User permanently deleted by admin")
                .build());
    }

    @Transactional
    public Product createProduct(String adminEmail, com.bmart.dto.SellerProductRequestDTO dto) {
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
        }
        if (category == null) {
            category = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(Category.builder()
                    .categoryName("General")
                    .slug("general")
                    .description("General merchandise category")
                    .build())
            );
        }

        String rawImgUrl = dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty() ? dto.getImageUrl().trim() : "";
        String resolvedImgUrl = extractImageUrlFromWebpage(rawImgUrl, dto.getName());

        Product product = Product.builder()
                .name(dto.getName() != null && !dto.getName().trim().isEmpty() ? dto.getName().trim() : "New Product")
                .description(dto.getDescription() != null && !dto.getDescription().trim().isEmpty() ? dto.getDescription().trim() : "High quality item on B-MART marketplace.")
                .price(dto.getPrice() != null ? dto.getPrice() : BigDecimal.valueOf(499))
                .discountPrice(dto.getDiscountPrice() != null ? dto.getDiscountPrice() : dto.getPrice())
                .brand(dto.getBrand() != null && !dto.getBrand().trim().isEmpty() ? dto.getBrand().trim() : "B-MART")
                .tags(dto.getTags() != null && !dto.getTags().trim().isEmpty() ? dto.getTags().trim() : "Featured")
                .stock(dto.getStock() != null ? dto.getStock() : 50)
                .category(category)
                .imageUrl(resolvedImgUrl)
                .status("APPROVED")
                .rating(4.8)
                .reviewCount(10)
                .build();

        Product saved = productRepository.save(product);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("CREATE_PRODUCT")
                .targetType("PRODUCT")
                .targetId(String.valueOf(saved.getProductId()))
                .reason("Created product directly by admin")
                .build());

        return saved;
    }

    @Transactional
    public Product updateProductStock(String adminEmail, Long productId, Integer stock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Product not found with ID: " + productId));

        product.setStock(stock != null && stock >= 0 ? stock : 0);
        Product saved = productRepository.save(product);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("UPDATE_PRODUCT_STOCK")
                .targetType("PRODUCT")
                .targetId(String.valueOf(productId))
                .reason("Admin updated stock to " + stock)
                .build());

        return saved;
    }

    private String extractImageUrlFromWebpage(String rawUrl, String productName) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return getCategoryFallbackImage(productName);
        }
        String cleanUrl = rawUrl.trim();
        String lower = cleanUrl.toLowerCase();

        // 1. If direct image asset URL
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
            lower.endsWith(".webp") || lower.endsWith(".avif") || lower.endsWith(".gif") ||
            lower.contains("unsplash.com") || lower.contains("imgur.com") || lower.contains("cloudinary.com")) {
            return cleanUrl;
        }

        // 2. OpenGraph / Meta Tag Web Image Scraper
        try {
            java.net.URL url = new java.net.URL(cleanUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(3500);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

            int status = conn.getResponseCode();
            if (status >= 200 && status < 400) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder html = new StringBuilder();
                String line;
                int maxChars = 200000;
                while ((line = reader.readLine()) != null && html.length() < maxChars) {
                    html.append(line).append("\n");
                }
                reader.close();

                String htmlStr = html.toString();

                java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("<meta\\s+(?:property|name)=[\"'](?:og:image|twitter:image)[\"']\\s+content=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher m1 = p1.matcher(htmlStr);
                if (m1.find()) {
                    return resolveRelativeUrl(cleanUrl, m1.group(1).trim());
                }

                java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("<meta\\s+content=[\"']([^\"']+)[\"']\\s+(?:property|name)=[\"'](?:og:image|twitter:image)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher m2 = p2.matcher(htmlStr);
                if (m2.find()) {
                    return resolveRelativeUrl(cleanUrl, m2.group(1).trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Could not scrape image from webpage URL: " + cleanUrl + " -> " + e.getMessage());
        }

        // 3. Fallback matching product title
        return getCategoryFallbackImage(productName);
    }

    private String resolveRelativeUrl(String baseUrl, String imgUrl) {
        if (imgUrl.startsWith("http://") || imgUrl.startsWith("https://")) {
            return imgUrl;
        }
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL resolved = new java.net.URL(base, imgUrl);
            return resolved.toString();
        } catch (Exception e) {
            return imgUrl;
        }
    }

    private String getCategoryFallbackImage(String title) {
        String t = title != null ? title.toLowerCase() : "";
        if (t.contains("backpack") || t.contains("rucksack") || t.contains("bag") || t.contains("tourist") || t.contains("travel")) {
            return "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&q=80";
        }
        if (t.contains("shoe") || t.contains("sneaker") || t.contains("nike") || t.contains("footwear")) {
            return "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&q=80";
        }
        if (t.contains("headphone") || t.contains("earphone") || t.contains("audio") || t.contains("sound")) {
            return "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&q=80";
        }
        if (t.contains("watch") || t.contains("smartwatch")) {
            return "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&q=80";
        }
        if (t.contains("shirt") || t.contains("tshirt") || t.contains("apparel") || t.contains("cloth")) {
            return "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600&q=80";
        }
        return "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=600&q=80";
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
    public Category createCategory(Category category) {
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            category.setSlug(category.getCategoryName().toLowerCase().replaceAll("[^a-z0-9]", "-"));
        }
        return categoryRepository.save(category);
    }

    // --- ORDERS & FINANCIALS ---
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Transactional
    public Order processRefund(String adminEmail, String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with ID: " + orderId));

        order.setStatus("REFUNDED");
        Order saved = orderRepository.save(order);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("REFUND_ORDER")
                .targetType("ORDER")
                .targetId(orderId)
                .reason(reason != null ? reason : "Platform level refund approved")
                .build());

        return saved;
    }

    public List<PayoutRequest> getPendingPayouts() {
        return payoutRequestRepository.findByStatus("PENDING");
    }

    @Transactional
    public PayoutRequest approvePayout(String adminEmail, Long payoutId, String note) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutId)
                .orElseThrow(() -> new NoSuchElementException("Payout request not found with ID: " + payoutId));

        payout.setStatus("APPROVED");
        payout.setAdminNote(note != null ? note : "Payout approved and transferred");
        payout.setProcessedAt(LocalDateTime.now());

        PayoutRequest saved = payoutRequestRepository.save(payout);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("APPROVE_PAYOUT")
                .targetType("PAYOUT")
                .targetId(String.valueOf(payoutId))
                .reason("Approved withdrawal of ₹" + payout.getAmount() + " for seller " + payout.getSeller().getStoreName())
                .build());

        return saved;
    }

    @Transactional
    public Seller setSellerCommission(String adminEmail, Long sellerId, BigDecimal commissionRate) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NoSuchElementException("Seller not found with ID: " + sellerId));

        seller.setCommissionRate(commissionRate);
        Seller saved = sellerRepository.save(seller);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("SET_COMMISSION")
                .targetType("SELLER")
                .targetId(String.valueOf(sellerId))
                .reason("Updated commission rate to " + commissionRate + "%")
                .build());

        return saved;
    }

    // --- BUSINESS ANALYTICS ---
    public Map<String, Object> getDailyAnalytics(String dateStr) {
        LocalDate date;
        try {
            date = (dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr.trim()) : LocalDate.now();
        } catch (Exception e) {
            date = LocalDate.now();
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        List<Order> validOrders = orders.stream()
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getStatus()) && !"FAILED".equalsIgnoreCase(o.getStatus()))
                .toList();

        BigDecimal totalRevenue = validOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());
        result.put("totalOrders", validOrders.size());
        result.put("totalRevenue", totalRevenue);
        result.put("transactions", validOrders);

        if (validOrders.isEmpty()) {
            result.put("message", "No revenue data available for this period");
        } else {
            result.put("message", "Daily revenue summary retrieved");
        }
        return result;
    }

    public Map<String, Object> getMonthlyAnalytics(Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int reqMonth = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();
        int reqYear = (year != null && year > 2000) ? year : now.getYear();

        YearMonth ym = YearMonth.of(reqYear, reqMonth);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        List<Order> validOrders = orders.stream()
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getStatus()) && !"FAILED".equalsIgnoreCase(o.getStatus()))
                .toList();

        BigDecimal totalRevenue = validOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", reqYear + "-" + String.format("%02d", reqMonth));
        result.put("totalOrders", validOrders.size());
        result.put("totalRevenue", totalRevenue);
        result.put("dailyTrendSummary", calculateDailyTrends(validOrders, ym.lengthOfMonth()));

        if (validOrders.isEmpty()) {
            result.put("message", "No revenue data available for this period");
        } else {
            result.put("message", "Monthly revenue & trend summary retrieved");
        }
        return result;
    }

    private List<Map<String, Object>> calculateDailyTrends(List<Order> orders, int daysInMonth) {
        Map<Integer, BigDecimal> dailyRev = new HashMap<>();
        Map<Integer, Integer> dailyCount = new HashMap<>();

        for (Order o : orders) {
            if (o.getCreatedAt() != null) {
                int day = o.getCreatedAt().getDayOfMonth();
                BigDecimal amt = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                dailyRev.put(day, dailyRev.getOrDefault(day, BigDecimal.ZERO).add(amt));
                dailyCount.put(day, dailyCount.getOrDefault(day, 0) + 1);
            }
        }

        List<Map<String, Object>> trends = new ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) {
            Map<String, Object> dayStat = new LinkedHashMap<>();
            dayStat.put("day", i);
            dayStat.put("orders", dailyCount.getOrDefault(i, 0));
            dayStat.put("revenue", dailyRev.getOrDefault(i, BigDecimal.ZERO));
            trends.add(dayStat);
        }
        return trends;
    }

    public Map<String, Object> getYearlyAnalytics(Integer year) {
        int reqYear = (year != null && year > 2000) ? year : LocalDate.now().getYear();

        LocalDateTime currStart = Year.of(reqYear).atDay(1).atStartOfDay();
        LocalDateTime currEnd = Year.of(reqYear).atMonth(12).atEndOfMonth().atTime(LocalTime.MAX);

        List<Order> currOrders = orderRepository.findByCreatedAtBetween(currStart, currEnd).stream()
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getStatus()) && !"FAILED".equalsIgnoreCase(o.getStatus()))
                .toList();

        BigDecimal currRevenue = currOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime prevStart = Year.of(reqYear - 1).atDay(1).atStartOfDay();
        LocalDateTime prevEnd = Year.of(reqYear - 1).atMonth(12).atEndOfMonth().atTime(LocalTime.MAX);

        List<Order> prevOrders = orderRepository.findByCreatedAtBetween(prevStart, prevEnd).stream()
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getStatus()) && !"FAILED".equalsIgnoreCase(o.getStatus()))
                .toList();

        BigDecimal prevRevenue = prevOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double growthPercentage = 0.0;
        if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthPercentage = currRevenue.subtract(prevRevenue)
                    .divide(prevRevenue, 4, java.math.RoundingMode.HALF_UP)
                    .doubleValue() * 100;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", reqYear);
        result.put("totalOrders", currOrders.size());
        result.put("totalRevenue", currRevenue);
        result.put("previousYearRevenue", prevRevenue);
        result.put("yoyGrowthPercentage", growthPercentage);

        if (currOrders.isEmpty()) {
            result.put("message", "No revenue data available for this period");
        } else {
            result.put("message", "Yearly revenue & YoY comparison retrieved");
        }
        return result;
    }

    public Map<String, Object> getOverallAnalytics() {
        List<Order> allOrders = orderRepository.findAll().stream()
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getStatus()) && !"FAILED".equalsIgnoreCase(o.getStatus()))
                .toList();

        BigDecimal totalRevenue = allOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgOrderValue = 0.0;
        if (!allOrders.isEmpty()) {
            avgOrderValue = totalRevenue.doubleValue() / allOrders.size();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", userRepository.count());
        result.put("totalSellers", sellerRepository.count());
        result.put("totalProducts", productRepository.count());
        result.put("totalOrders", allOrders.size());
        result.put("totalRevenue", totalRevenue);
        result.put("averageOrderValue", avgOrderValue);

        if (allOrders.isEmpty()) {
            result.put("message", "No revenue data available for this period");
        } else {
            result.put("message", "Overall lifetime platform analytics retrieved");
        }
        return result;
    }

    public Map<String, Object> getSiteAnalytics() {
        return getOverallAnalytics();
    }
}
