package com.hsf.e_comerce.order.repository;

import com.hsf.e_comerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("""
    SELECT oi FROM OrderItem oi
    JOIN FETCH oi.product p
    JOIN FETCH p.shop
    WHERE oi.order.id = :orderId
    """)
    List<OrderItem> findByOrderId(UUID orderId);

}
