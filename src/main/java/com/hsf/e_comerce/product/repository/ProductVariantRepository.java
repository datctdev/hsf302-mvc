package com.hsf.e_comerce.product.repository;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    
    List<ProductVariant> findByProduct(Product product);
    
    Optional<ProductVariant> findBySku(String sku);
    
    boolean existsBySku(String sku);
}
