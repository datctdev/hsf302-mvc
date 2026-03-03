package com.hsf.e_comerce.product.repository;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    
    List<ProductImage> findByProductOrderByDisplayOrderAsc(Product product);
    
    Optional<ProductImage> findByProductAndIsThumbnailTrue(Product product);
    
    /** One image per product (first by display order) for the given product IDs. */
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id IN :productIds ORDER BY pi.product.id, pi.displayOrder ASC")
    List<ProductImage> findByProduct_IdInOrderByProductIdAndDisplayOrder(@Param("productIds") Set<UUID> productIds);
}
