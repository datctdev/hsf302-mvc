package com.hsf.e_comerce.order.dto.response;

import com.hsf.e_comerce.order.valueobject.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Minimal order info for chatbot tracking (guest or buyer).
 * Guest: only orderNumber, status, optional deliveredAt.
 * Buyer (owner): can include more fields if needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse {
    private String orderNumber;
    private OrderStatus status;
    private String statusDisplay;
    private LocalDateTime deliveredAt;
    private boolean found;
    private String message;
}
