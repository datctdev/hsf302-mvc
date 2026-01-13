package com.hsf.e_comerce.shop.repository;

import com.hsf.e_comerce.shop.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {
    
    Optional<Shop> findByUserId(UUID userId);
    
    boolean existsByUserId(UUID userId);
    
    boolean existsByName(String name);
}
