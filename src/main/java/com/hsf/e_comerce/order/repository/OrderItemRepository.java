package com.hsf.e_comerce.order.repository;

import com.hsf.e_comerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("""
    SELECT oi
    FROM OrderItem oi
    JOIN FETCH oi.product p
    JOIN FETCH p.shop s
    WHERE oi.order.id = :orderId
    """)
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Thống kê số lượng bán theo sản phẩm của một shop (chỉ đơn DELIVERED).
     * Returns: productId (UUID), productName (String), totalQuantity (Long).
     */
    @Query("""
        SELECT oi.product.id, p.name, COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        JOIN oi.order o
        JOIN oi.product p
        WHERE o.shop.id = :shopId
          AND o.status = com.hsf.e_comerce.order.valueobject.OrderStatus.DELIVERED
        GROUP BY oi.product.id, p.name
        ORDER BY SUM(oi.quantity) DESC
        """)
    List<Object[]> getProductSalesByShop(@Param("shopId") UUID shopId);
}
