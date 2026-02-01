package com.hsf.e_comerce.order.valueobject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ma trận chuyển trạng thái đơn hàng (seller/admin).
 * Chuỗi chuẩn: PENDING_PAYMENT → CONFIRMED → PROCESSING → SHIPPED → DELIVERED.
 * Hủy chỉ cho phép từ PENDING_PAYMENT hoặc CONFIRMED.
 */
public final class OrderStatusTransition {

    private static final Set<OrderStatus> TERMINAL = EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDED);

    /** Các trạng thái được phép chuyển từ trạng thái hiện tại (seller/admin cập nhật). */
    private static final java.util.Map<OrderStatus, Set<OrderStatus>> ALLOWED_NEXT;

    static {
        var m = new java.util.EnumMap<OrderStatus, Set<OrderStatus>>(OrderStatus.class);
        m.put(OrderStatus.PENDING, Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        m.put(OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        m.put(OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        m.put(OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPING));
        m.put(OrderStatus.SHIPPING, Set.of(OrderStatus.DELIVERED));
        m.put(OrderStatus.DELIVERED, Set.of());
        m.put(OrderStatus.CANCELLED, Set.of());
        m.put(OrderStatus.REFUNDED, Set.of());
        ALLOWED_NEXT = Collections.unmodifiableMap(m);
    }

    private OrderStatusTransition() {}

    /**
     * Trạng thái được phép chuyển tiếp từ {@code current} (dùng cho dropdown seller).
     * Không bao gồm trạng thái hiện tại; trạng thái kết thúc (DELIVERED, CANCELLED, REFUNDED) trả về rỗng.
     */
    public static List<OrderStatus> getAllowedNextStatuses(OrderStatus current) {
        if (current == null) return List.of();
        Set<OrderStatus> next = ALLOWED_NEXT.get(current);
        if (next == null) return List.of();
        return next.stream().sorted(Enum::compareTo).collect(Collectors.toList());
    }

    /** Kiểm tra chuyển từ current sang next có hợp lệ không. */
    public static boolean isAllowed(OrderStatus current, OrderStatus next) {
        if (current == null || next == null) return false;
        Set<OrderStatus> allowed = ALLOWED_NEXT.get(current);
        return allowed != null && allowed.contains(next);
    }

    public static boolean isTerminal(OrderStatus status) {
        return status != null && TERMINAL.contains(status);
    }
}
