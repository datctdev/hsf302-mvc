package com.hsf.e_comerce.order.entity;

import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = true)
    private ProductVariant variant; // Optional: null nếu product không có variant

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName; // Lưu tên sản phẩm tại thời điểm đặt hàng

    @Column(name = "variant_name", length = 100)
    private String variantName; // e.g., "Color"

    @Column(name = "variant_value", length = 100)
    private String variantValue; // e.g., "Red"

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice; // Giá tại thời điểm đặt hàng

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice; // quantity * unitPrice

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method to calculate total price
    public void calculateTotalPrice() {
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }
        if (quantity == null) {
            quantity = 1;
        }
        totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
