package com.hsf.e_comerce.payment.controller;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.common.annotation.CurrentUser;
import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.order.dto.response.OrderResponse;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.repository.OrderRepository;
import com.hsf.e_comerce.order.service.OrderService;
import com.hsf.e_comerce.payment.entity.Payment;
import com.hsf.e_comerce.payment.enums.PaymentMethod;
import com.hsf.e_comerce.payment.repository.PaymentRepository;
import com.hsf.e_comerce.payment.service.PaymentService;
import com.hsf.e_comerce.payment.service.VNPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PaymentMvcController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final VNPayService vnPayService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Trang chọn phương thức thanh toán
     */
    @GetMapping("/{orderId}")
    public String selectPayment(
            @PathVariable UUID orderId,
            @CurrentUser User user,
            Model model) {

        OrderResponse order = orderService.getOrderByIdAndUser(orderId, user);

        model.addAttribute("order", order);
        return "payments/select";
    }

    /**
     * Xác nhận COD
     */
    @PostMapping("/cod/confirm")
    public String confirmCOD(
            @RequestParam UUID orderId,
            @CurrentUser User user,
            RedirectAttributes redirectAttributes) {

        try {
            paymentService.confirmCOD(orderId, user);
            redirectAttributes.addFlashAttribute("success", "Đặt hàng COD thành công");
            return "redirect:/orders/" + orderId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/payments/" + orderId;
        }
    }

    /**
     * Redirect sang VNPay
     */
    @GetMapping("/vnpay/redirect")
    public String redirectVNPay(
            @RequestParam UUID orderId,
            @CurrentUser User user) {

        Order order = orderRepository
                .findByIdAndUser(orderId, user)
                .orElseThrow(() -> new CustomException("Order không hợp lệ"));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseGet(() -> paymentService.createPayment(order, PaymentMethod.VNPAY));

        String url = vnPayService.buildPaymentUrl(payment);
        return "redirect:" + url;
    }

    /**
     * Callback từ VNPay
     */
    @GetMapping("/vnpay/callback")
    public String vnpayCallback(
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {

        try {
            paymentService.handleVNPayCallback(params);
            redirectAttributes.addFlashAttribute("success", "Thanh toán VNPay thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Thanh toán thất bại");
        }

        String txnRef = params.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByTransactionId(txnRef).orElse(null);

        if (payment != null) {
            return "redirect:/orders/" + payment.getOrder().getId();
        }
        return "redirect:/orders";
    }

    @GetMapping("/{orderId}/select-method")
    public String paymentPage(
            @PathVariable UUID orderId,
            @CurrentUser User currentUser,
            Model model
    ) {
        OrderResponse order = orderService
                .getOrderForPayment(orderId, currentUser);

        model.addAttribute("order", order);
        return "payments/select";
    }

}
