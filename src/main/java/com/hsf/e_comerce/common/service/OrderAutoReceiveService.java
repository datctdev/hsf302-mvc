package com.hsf.e_comerce.common.service;

import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.repository.OrderRepository;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAutoReceiveService {

    private final OrderRepository orderRepository;

    @Transactional
    @Scheduled(cron = "0 0 * * * *") // mỗi giờ
    public void autoMarkReceivedOrders() {

        LocalDateTime threshold = LocalDateTime.now().minusDays(3);

        List<Order> orders = orderRepository
                .findByStatusAndReceivedByBuyerFalseAndDeliveredAtBefore(
                        OrderStatus.DELIVERED,
                        threshold
                );

        for (Order order : orders) {
            order.setReceivedByBuyer(true);
            order.setReceivedAt(LocalDateTime.now());

            log.info("Auto received order {} after 3 days", order.getOrderNumber());
        }

        orderRepository.saveAll(orders);
    }
}

