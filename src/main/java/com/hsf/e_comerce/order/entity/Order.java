package com.hsf.e_comerce.order.entity;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.shop.entity.Shop;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber; // Format: ORD-YYYYMMDD-XXXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Buyer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "shipping_name", nullable = false, length = 255)
    private String shippingName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    private String shippingPhone;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_district", length = 100)
    private String shippingDistrict;

    @Column(name = "shipping_ward", length = 100)
    private String shippingWard;

    @Column(name = "shipping_district_id")
    private Integer shippingDistrictId;  // Mã quận/huyện GHN

    @Column(name = "shipping_ward_code", length = 20)
    private String shippingWardCode;  // Mã phường/xã GHN

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Ghi chú của khách hàng

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal; // Tổng tiền sản phẩm

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO; // Phí vận chuyển

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total; // Tổng tiền = subtotal + shippingFee

    @Column(name = "ghn_order_code", length = 100)
    private String ghnOrderCode; // Mã vận đơn GHN (nếu có)

    @Column(name = "platform_commission", precision = 12, scale = 2)
    private BigDecimal platformCommission = BigDecimal.ZERO; // Hoa hồng nền tảng (tính trên subtotal)

    @Column(name = "commission_rate") // Double: không dùng precision/scale (chỉ DECIMAL/BigDecimal mới dùng)
    private Double commissionRate; // % hoa hồng áp dụng tại thời điểm tạo đơn

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<OrderItem> items = new HashSet<>();

    @Column(name = "received_by_buyer", nullable = false)
    private boolean receivedByBuyer = false;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method to calculate total
    public void calculateTotal() {
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
        if (shippingFee == null) {
            shippingFee = BigDecimal.ZERO;
        }
        total = subtotal.add(shippingFee);
    }
}
