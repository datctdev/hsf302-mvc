package com.hsf.e_comerce.product.repository;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.valueobject.ProductStatus;
import com.hsf.e_comerce.shop.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    @Query("SELECT p FROM Product p WHERE p.status = 'PUBLISHED' " +
           "AND p.shop.status = 'ACTIVE' AND p.shop.user.isActive = true " +
           "AND p.deleted = false " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchPublishedProducts(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN ProductCategoryMapping m ON m.product.id = p.id " +
           "WHERE p.status = 'PUBLISHED' " +
           "AND p.shop.status = 'ACTIVE' AND p.shop.user.isActive = true " +
           "AND p.deleted = false " +
           "AND (:categoryId IS NULL OR m.category.id = :categoryId) " +
           "AND (:shopId IS NULL OR p.shop.id = :shopId) " +
           "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
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
}
