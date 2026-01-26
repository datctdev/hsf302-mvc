package com.hsf.e_comerce.payment.service.impl;

import com.hsf.e_comerce.payment.entity.Payment;
import com.hsf.e_comerce.payment.service.VNPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.payUrl}")
    private String payUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    @Override
    public String buildPaymentUrl(Payment payment) {

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_TxnRef", payment.getTransactionId());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrder().getOrderNumber());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_Locale", "vn");

        String hashData = buildQueryString(params);
        String secureHash = hmacSHA512(hashSecret, hashData);

        return payUrl + "?" + hashData + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    public boolean verifyChecksum(Map<String, String> params) {

        Map<String, String> copy = new HashMap<>(params);

        String receivedHash = copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        String hashData = buildQueryString(new TreeMap<>(copy));
        String calculatedHash = hmacSHA512(hashSecret, hashData);

        return calculatedHash.equals(receivedHash);
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes());
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo checksum VNPay");
        }
    }
}
