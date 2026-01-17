package com.hsf.e_comerce.cart.repository;

import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    
    Optional<CartItem> findByCartAndProductAndVariant(Cart cart, Product product, ProductVariant variant);
    
    Optional<CartItem> findByCartAndProductAndVariantIsNull(Cart cart, Product product);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId AND " +
           "(:variantId IS NULL AND ci.variant IS NULL OR ci.variant.id = :variantId)")
    Optional<CartItem> findByCartAndProductAndVariantId(
        @Param("cartId") UUID cartId,
        @Param("productId") UUID productId,
        @Param("variantId") UUID variantId
    );
    
    void deleteByCartId(UUID cartId);
    
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci " +
           "INNER JOIN ci.cart c WHERE c.user.id = :userId")
    Integer countTotalItemsByUserId(@Param("userId") UUID userId);
}
