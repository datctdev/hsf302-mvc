package com.hsf.e_comerce.product.repository;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.valueobject.ProductStatus;
import com.hsf.e_comerce.shop.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    List<Product> findByShop(Shop shop);
    
    List<Product> findByShopAndDeletedFalse(Shop shop);
    
    List<Product> findByShopAndStatus(Shop shop, ProductStatus status);
    
    List<Product> findByShopAndStatusAndDeletedFalse(Shop shop, ProductStatus status);
    
    Optional<Product> findBySku(String sku);
    
    Optional<Product> findBySkuAndDeletedFalse(String sku);
    
    boolean existsBySku(String sku);
    
    boolean existsBySkuAndDeletedFalse(String sku);
    
    Optional<Product> findByIdAndDeletedFalse(UUID id);
    
    // Public query methods - filter deleted products
    @Query("SELECT p FROM Product p WHERE p.status = 'PUBLISHED' " +
           "AND p.shop.status = 'ACTIVE' AND p.shop.user.isActive = true " +
           "AND p.deleted = false")
    Page<Product> findPublishedProducts(Pageable pageable);
    
    /**
     * Full-text search (PostgreSQL): tìm trong name, description, sku.
     * plainto_tsquery tách từ khóa theo khoảng trắng và match tài liệu chứa TẤT CẢ các từ (AND).
     * Config 'simple' phù hợp tiếng Việt (không stem).
     */
    @Query(value = "SELECT p.* FROM products p " +
           "INNER JOIN shops s ON p.shop_id = s.id " +
           "INNER JOIN users u ON s.user_id = u.id " +
           "WHERE p.status = 'PUBLISHED' AND s.status = 'ACTIVE' AND u.is_active = true AND p.deleted = false " +
           "AND to_tsvector('simple', coalesce(p.name,'') || ' ' || coalesce(p.description,'') || ' ' || coalesce(p.sku,'')) " +
           "@@ plainto_tsquery('simple', :keyword) " +
           "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(p.id) FROM products p " +
           "INNER JOIN shops s ON p.shop_id = s.id " +
           "INNER JOIN users u ON s.user_id = u.id " +
           "WHERE p.status = 'PUBLISHED' AND s.status = 'ACTIVE' AND u.is_active = true AND p.deleted = false " +
           "AND to_tsvector('simple', coalesce(p.name,'') || ' ' || coalesce(p.description,'') || ' ' || coalesce(p.sku,'')) " +
           "@@ plainto_tsquery('simple', :keyword)",
           nativeQuery = true)
    Page<Product> searchPublishedProducts(@Param("keyword") String keyword, Pageable pageable);
    
    /* Thứ tự điều kiện :search: dùng :search trong CONCAT trước để Hibernate suy ra type string (tránh lỗi lower(bytea) trên PostgreSQL). */
    @Query("SELECT DISTINCT p FROM Product p " +
           "WHERE p.status = 'PUBLISHED' " +
           "AND p.shop.status = 'ACTIVE' AND p.shop.user.isActive = true " +
           "AND p.deleted = false " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:shopId IS NULL OR p.shop.id = :shopId) " +
           "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice) " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR :search IS NULL OR :search = '')")
    Page<Product> findPublishedProductsWithFilters(
        @Param("categoryId") UUID categoryId,
        @Param("shopId") UUID shopId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("search") String search,
        Pageable pageable
    );
    
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.status = 'PUBLISHED' " +
           "AND p.shop.status = 'ACTIVE' AND p.shop.user.isActive = true " +
           "AND p.deleted = false")
    Optional<Product> findPublishedById(@Param("id") UUID id);
    
    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.status = 'PUBLISHED' " +
           "AND p.shop.status = 'ACTIVE' AND p.shop.user.isActive = true " +
           "AND p.deleted = false")
    Page<Product> findPublishedProductsByShop(@Param("shopId") UUID shopId, Pageable pageable);

    /** Admin: list all products (including deleted) with optional shop/status filter. */
    @Query("SELECT p FROM Product p WHERE (:shopId IS NULL OR p.shop.id = :shopId) " +
           "AND (:status IS NULL OR p.status = :status)")
    @EntityGraph(attributePaths = {"shop"})
    Page<Product> findAllForAdmin(@Param("shopId") UUID shopId, @Param("status") ProductStatus status, Pageable pageable);
}
