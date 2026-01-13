package com.hsf.e_comerce.product.repository;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductCategory;
import com.hsf.e_comerce.product.entity.ProductCategoryMapping;
import com.hsf.e_comerce.product.entity.ProductCategoryMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductCategoryMappingRepository extends JpaRepository<ProductCategoryMapping, ProductCategoryMappingId> {
    
    List<ProductCategoryMapping> findByProduct(Product product);
    
    void deleteByProduct(Product product);
    
    void deleteByProductAndCategory(Product product, ProductCategory category);
}
