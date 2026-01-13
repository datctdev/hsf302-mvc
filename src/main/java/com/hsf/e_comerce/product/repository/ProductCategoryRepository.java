package com.hsf.e_comerce.product.repository;

import com.hsf.e_comerce.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    
    Optional<ProductCategory> findByName(String name);
    
    List<ProductCategory> findByParentIsNull(); // Root categories
    
    List<ProductCategory> findByParent(ProductCategory parent);
}
