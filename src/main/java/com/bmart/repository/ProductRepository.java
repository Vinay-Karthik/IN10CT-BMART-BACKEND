package com.bmart.repository;

import com.bmart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.categoryId = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) AND " +
           "(:minRating IS NULL OR p.rating >= :minRating) AND " +
           "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> filterProducts(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("brand") String brand,
            @Param("minRating") Double minRating,
            @Param("query") String query,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Product> findByCategoryCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Product> findBySellerSellerId(Long sellerId);

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Product> findBySellerSellerIdAndStatus(Long sellerId, String status);

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Product> findByStatus(String status);
}
