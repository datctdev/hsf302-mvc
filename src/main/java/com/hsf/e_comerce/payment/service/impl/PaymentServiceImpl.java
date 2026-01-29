package com.hsf.e_comerce.payment.service.impl;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.cart.entity.Cart;
import com.hsf.e_comerce.cart.entity.CartItem;
import com.hsf.e_comerce.cart.repository.CartItemRepository;
import com.hsf.e_comerce.cart.repository.CartRepository;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.entity.OrderItem;
import com.hsf.e_comerce.order.repository.OrderRepository;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.payment.entity.Payment;
import com.hsf.e_comerce.payment.enums.PaymentMethod;
import com.hsf.e_comerce.payment.enums.PaymentStatus;
import com.hsf.e_comerce.payment.repository.PaymentRepository;
import com.hsf.e_comerce.payment.service.PaymentService;
import com.hsf.e_comerce.payment.service.VNPayService;
import com.hsf.e_comerce.product.entity.ProductVariant;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final VNPayService vnPayService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    public Payment createPayment(Order order, PaymentMethod method) {

        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new CustomException("Order đã có payment.");
        }

        PaymentStatus status =
                method == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.INIT;

        Payment payment = Payment.builder()
                .order(order)
                .method(method)
                .amount(order.getTotal())
                .status(status)
                .transactionId(UUID.randomUUID().toString())
                .build();

        // Update order status
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        return paymentRepository.save(payment);
    }

    @Override
    public void confirmCOD(UUID orderId, User user) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Order không tồn tại"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("Không có quyền");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseGet(() -> createPayment(order, PaymentMethod.COD));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new CustomException("Payment không hợp lệ");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.CONFIRMED);

        paymentRepository.save(payment);
        orderRepository.save(order);

        this.handlePaymentSuccess(order.getId());
    }


    @Override
    public void handleVNPayCallback(Map<String, String> params) {

        if (!vnPayService.verifyChecksum(params)) {
            throw new CustomException("Sai checksum VNPay");
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String gatewayTxnId = params.get("vnp_TransactionNo");

        Payment payment = paymentRepository.findByTransactionId(txnRef)
                .orElseThrow(() -> new CustomException("Payment không tồn tại"));

        payment.setGatewayTransactionId(gatewayTxnId);
        payment.setGatewayResponse(params.toString());

        Order order = payment.getOrder();

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.CONFIRMED);

            this.handlePaymentSuccess(order.getId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);

            if (order.isStockDeducted()) {
                for (OrderItem item : order.getItems()) {
                    item.getVariant().setStockQuantity(
                            item.getVariant().getStockQuantity() + item.getQuantity()
                    );
                }
                order.setStockDeducted(false);
            }

            order.setStatus(OrderStatus.CANCELLED);
        }


        paymentRepository.save(payment);
        orderRepository.save(order);
    }

    @Override
    public Payment getOrCreatePaymentForVNPay(UUID orderId, User user) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new CustomException("Order không hợp lệ"));
        return paymentRepository.findByOrder(order)
                .orElseGet(() -> createPayment(order, PaymentMethod.VNPAY));
    }

    @Override
    public Optional<UUID> getOrderIdByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(p -> p.getOrder().getId());
    }

    @Transactional
    public void handlePaymentSuccess(UUID orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow();

        if (order.isStockDeducted()) return; // chống double call

        for (OrderItem item : order.getItems()) {
            ProductVariant v = item.getVariant();
            if (v.getStockQuantity() < item.getQuantity()) {
                throw new CustomException("Không đủ tồn kho");
            }
            v.setStockQuantity(v.getStockQuantity() - item.getQuantity());
        }

        order.setStockDeducted(true);
        orderRepository.save(order);

        Cart cart = cartRepository.findByUserIdWithItems(order.getUser().getId())
                .orElseThrow();

        List<CartItem> itemsToRemove = cart.getItems().stream()
                .filter(i -> i.getProduct().getShop().getId().equals(order.getShop().getId()))
                .toList();

        System.out.println("Order shopId = " + order.getShop().getId());
        System.out.println("Cart items count = " + cart.getItems().size());

        cart.getItems().forEach(i -> {
            System.out.println(
                    "CartItem id=" + i.getId() +
                            ", product=" + (i.getProduct() != null) +
                            ", shop=" + (i.getProduct() != null && i.getProduct().getShop() != null)
            );
        });

        for (CartItem item : itemsToRemove) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        }
        cartRepository.save(cart);
    }

}
