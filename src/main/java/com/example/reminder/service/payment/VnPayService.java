package com.example.reminder.service.payment;

import com.example.reminder.entity.Plan;
import com.example.reminder.entity.Transaction;
import com.example.reminder.entity.UserSubscription;
import com.example.reminder.exception.BadRequestException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.reminder.config.VnPayProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class VnPayService {

    private static final Logger logger = LoggerFactory.getLogger(VnPayService.class);
    private final VnPayProperties properties;

    public String createPaymentUrl(Transaction transaction, UserSubscription subscription, Plan plan, String clientIp) {
        try {
            if (properties.getTmnCode() == null || properties.getTmnCode().isBlank()) {
                throw new BadRequestException("Missing VNPay terminal code (VNPAY_TMN_CODE)");
            }
            if (properties.getHashSecret() == null || properties.getHashSecret().isBlank()) {
                throw new BadRequestException("Missing VNPay hash secret (VNPAY_HASH_SECRET)");
            }
            if (properties.getReturnUrl() == null || properties.getReturnUrl().isBlank()) {
                throw new BadRequestException("Missing VNPay return URL (VNPAY_RETURN_URL)");
            }

            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", properties.getTmnCode());
            params.put("vnp_Amount", plan.getPrice().multiply(new java.math.BigDecimal(100)).toBigInteger().toString());
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", String.valueOf(transaction.getId()));
            params.put("vnp_OrderInfo", "Purchase subscription: " + subscription.getPlan().getName());
            params.put("vnp_OrderType", "other");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", properties.getReturnUrl());
            params.put("vnp_CreateDate", transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            if (clientIp != null) params.put("vnp_IpAddr", clientIp);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (e.getValue() == null || e.getValue().isEmpty()) continue;
                hashData.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII)).append('&');
                query.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII)).append('&');
            }

            // remove trailing &
            if (hashData.length() > 0) hashData.setLength(hashData.length() - 1);
            if (query.length() > 0) query.setLength(query.length() - 1);

            String secureHash = hmacSHA512(properties.getHashSecret(), hashData.toString());

                String baseUrl = properties.getUrl() != null && !properties.getUrl().isEmpty()
                    ? properties.getUrl()
                    : "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

                String paymentUrl = baseUrl + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;
            return paymentUrl;
        } catch (Exception ex) {
            logger.error("Error creating VNPay payment URL - TmnCode: {}, HasSecret: {}, ReturnUrl: {}", 
                properties.getTmnCode(), 
                properties.getHashSecret() != null ? "***" : "null", 
                properties.getReturnUrl(), 
                ex);
            throw new RuntimeException("Failed to create VnPay payment URL: " + ex.getMessage(), ex);
        }
    }

    public boolean isValidReturnSignature(Map<String, String> params) {
        String incomingHash = params.get("vnp_SecureHash");
        if (incomingHash == null || incomingHash.isBlank()) {
            return false;
        }

        Map<String, String> filtered = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (Objects.equals(entry.getKey(), "vnp_SecureHash") || Objects.equals(entry.getKey(), "vnp_SecureHashType")) {
                continue;
            }
            filtered.put(entry.getKey(), entry.getValue());
        }

        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> e : filtered.entrySet()) {
            data.append(e.getKey())
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                    .append('&');
        }

        if (data.length() > 0) {
            data.setLength(data.length() - 1);
        }

        try {
            String expectedHash = hmacSHA512(properties.getHashSecret(), data.toString());
            return expectedHash.equalsIgnoreCase(incomingHash);
        } catch (Exception ex) {
            return false;
        }
    }

    private String hmacSHA512(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac.init(secretKey);
        byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hash.append('0');
            hash.append(hex);
        }
        return hash.toString().toUpperCase();
    }
}
