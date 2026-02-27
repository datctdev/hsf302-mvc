package com.hsf.e_comerce.platform.service.impl;

import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.entity.OrderItem;
import com.hsf.e_comerce.platform.dto.request.CommissionFilterRequest;
import com.hsf.e_comerce.platform.dto.response.CommissionDetailResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionItemResponse;
import com.hsf.e_comerce.platform.dto.response.CommissionResponse;
import com.hsf.e_comerce.platform.entity.Commission;
import com.hsf.e_comerce.platform.entity.CommissionItem;
import com.hsf.e_comerce.platform.repository.CommissionRepository;
import com.hsf.e_comerce.platform.service.CommissionService;
import com.hsf.e_comerce.shop.service.ShopService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CommissionRepository commissionRepository;
    private final CategoryCommissionServiceImpl categoryCommissionService;
    private final ShopService shopService;

    @Override
    @Transactional
    public void createCommission(Order order) {

        if (commissionRepository.existsByOrderId(order.getId())) {
            return;
        }

        BigDecimal totalCommission = BigDecimal.ZERO;

        Commission commission = Commission.builder()
                .orderId(order.getId())
                .sellerId(order.getShop().getUser().getId())
                .orderAmount(order.getSubtotal())
                .build();

        List<CommissionItem> commissionItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {

            BigDecimal rate = resolveCommissionRate(item);

            BigDecimal itemSubtotal =
                    item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal itemCommission =
                    itemSubtotal.multiply(rate)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            totalCommission = totalCommission.add(itemCommission);

            CommissionItem commissionItem = CommissionItem.builder()
                    .commission(commission)
                    .orderItemId(item.getId())
                    .productName(item.getProductName())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .commissionRate(rate)
                    .commissionAmount(itemCommission)
                    .build();

            commissionItems.add(commissionItem);
        }

        commission.setTotalCommission(totalCommission);
        commission.setItems(commissionItems);

        commissionRepository.save(commission);
    }

    private BigDecimal resolveCommissionRate(OrderItem item) {

        UUID categoryId = item.getProduct()
                .getCategory()
                .getId();

        return categoryCommissionService
                .getCommissionByCategory(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissionResponse> getCommissions(CommissionFilterRequest filter) {

        Specification<Commission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getSellerName() != null && !filter.getSellerName().isBlank()) {

                List<UUID> sellerIds = shopService
                        .findAllByNameContaining(filter.getSellerName());

                if (!sellerIds.isEmpty()) {
                    predicates.add(root.get("sellerId").in(sellerIds));
                } else {
                    predicates.add(cb.disjunction()); // không trả về gì
                }
            }

            if (filter.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        filter.getFrom().atStartOfDay()
                ));
            }

            if (filter.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"),
                        filter.getTo().atTime(23,59,59)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return commissionRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionDetailResponse getByOrderId(UUID orderId) {

        Commission commission = commissionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException("Commission không tồn tại."));

        List<CommissionItemResponse> items = commission.getItems()
                .stream()
                .map(i -> CommissionItemResponse.builder()
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .commissionRate(i.getCommissionRate())
                        .commissionAmount(i.getCommissionAmount())
                        .build())
                .toList();

        return CommissionDetailResponse.builder()
                .orderId(commission.getOrderId())
                .sellerName(shopService.findByUserId(commission.getSellerId()))
                .orderAmount(commission.getOrderAmount())
                .totalCommission(commission.getTotalCommission())
                .items(items)
                .createdAt(commission.getCreatedAt())
                .build();
    }

    private CommissionResponse mapToResponse(Commission c) {

        String sellerName = shopService.findByUserId(c.getSellerId());

        return CommissionResponse.builder()
                .orderId(c.getOrderId())
                .sellerId(c.getSellerId())
                .sellerName(sellerName)
                .orderAmount(c.getOrderAmount())
                .commissionAmount(c.getTotalCommission())
                .createdAt(c.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalCommissionBySeller(UUID sellerId) {
        return commissionRepository.getTotalCommissionBySeller(sellerId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalNetIncomeBySeller(UUID sellerId) {
        return commissionRepository.getTotalNetIncomeBySeller(sellerId);
    }
}
