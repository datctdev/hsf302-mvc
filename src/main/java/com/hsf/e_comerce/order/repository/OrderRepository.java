package com.hsf.e_comerce.order.repository;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @EntityGraph(attributePaths = {"user", "shop", "items", "items.product", "items.variant"})
    Optional<Order> findById(UUID id);
    
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT o FROM Order o WHERE o.shop.id = :shopId ORDER BY o.createdAt DESC")
    List<Order> findByShopId(@Param("shopId") UUID shopId);
    
    @Query("SELECT o FROM Order o WHERE o.shop.id = :shopId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByShopIdAndStatus(@Param("shopId") UUID shopId, @Param("status") OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") OrderStatus status);
    
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllOrders();
    
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByStatus(@Param("status") OrderStatus status);
    
    @EntityGraph(attributePaths = {"user", "shop", "items", "items.product", "items.variant"})
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithItems(@Param("userId") UUID userId);
    
    @EntityGraph(attributePaths = {"user", "shop", "items", "items.product", "items.variant"})
    @Query("SELECT o FROM Order o WHERE o.shop.id = :shopId ORDER BY o.createdAt DESC")
    List<Order> findByShopIdWithItems(@Param("shopId") UUID shopId);

    Optional<Order> findByIdAndUser(UUID orderId, User currentUser);

    @Query("""
    SELECT o
    FROM Order o
    LEFT JOIN FETCH o.items i
    LEFT JOIN FETCH i.product
    LEFT JOIN FETCH i.variant
    WHERE o.id = :orderId
      AND o.user = :user
""")
    Optional<Order> findByIdAndUserWithItems(
            @Param("orderId") UUID orderId,
            @Param("user") User user
    );

    List<Order> findByShopIdAndStatusNot(UUID shopId, OrderStatus orderStatus);

    Optional<Order> findByGhnOrderCode(String ghnOrderCode);
}
