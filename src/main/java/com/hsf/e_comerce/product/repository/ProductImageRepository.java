package com.hsf.e_comerce.product.repository;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    
    List<ProductImage> findByProductOrderByDisplayOrderAsc(Product product);
    
    Optional<ProductImage> findByProductAndIsThumbnailTrue(Product product);
}
