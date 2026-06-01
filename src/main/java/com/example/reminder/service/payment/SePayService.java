package com.example.reminder.service.payment;

import com.example.reminder.config.SePayProperties;
import com.example.reminder.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SePayService {

    private final SePayProperties properties;
    private final ObjectMapper objectMapper;

    public String buildTransferContent(Long transactionId) {
        if (transactionId == null) {
            throw new BadRequestException("Transaction id is required to build transfer content");
        }

        String prefix = normalizedPrefix();
        return prefix + transactionId;
    }

    public String buildQrUrl(BigDecimal amount, String transferContent) {
        validateQrConfig();

        String encodedAddInfo = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String encodedAccountName = URLEncoder.encode(properties.getAccountName(), StandardCharsets.UTF_8);
        String normalizedAmount = normalizeAmount(amount);

        return "https://img.vietqr.io/image/"
                + properties.getBankCode()
                + "-"
                + properties.getAccountNumber()
                + "-compact2.png?amount="
                + normalizedAmount
                + "&addInfo="
                + encodedAddInfo
                + "&accountName="
                + encodedAccountName;
    }

    public boolean isValidWebhookSignature(String payload, String incomingSignature, String timestamp) {
        if (incomingSignature == null || incomingSignature.isBlank()) {
            return false;
        }

        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new BadRequestException("Missing SePay webhook secret (SEPAY_WEBHOOK_SECRET)");
        }

        String body = payload == null ? "" : payload;
        String timestampValue = timestamp == null ? "" : timestamp.trim();
        List<String> candidates = List.of(
                body,
                timestampValue.isBlank() ? body : timestampValue + "." + body,
                timestampValue.isBlank() ? body : timestampValue + body
        );
        String normalizedIncoming = incomingSignature.trim();
        int markerIndex = normalizedIncoming.indexOf('=');
        if (markerIndex >= 0 && markerIndex < normalizedIncoming.length() - 1) {
            normalizedIncoming = normalizedIncoming.substring(markerIndex + 1);
        }

        for (String candidate : candidates) {
            String expectedSignature = hmacSha256Hex(candidate, properties.getWebhookSecret());
            if (expectedSignature.equalsIgnoreCase(normalizedIncoming)) {
                return true;
            }
        }

        return false;
    }

    public Optional<String> extractTransactionReference(String payload) {
        JsonNode root = parsePayload(payload);
        List<JsonNode> candidates = new ArrayList<>();
        candidates.add(root);
        if (root.has("data") && root.get("data").isObject()) {
            candidates.add(root.get("data"));
        }

        for (JsonNode node : candidates) {
            Optional<String> direct = pickFirst(node,
                    "transactionRef",
                    "transaction_ref",
                    "reference",
                    "referenceCode",
                    "transferContent",
                    "content",
                    "description",
                    "desc");
            if (direct.isPresent()) {
                Optional<String> resolved = resolveTransferRef(direct.get());
                if (resolved.isPresent()) {
                    return resolved;
                }
            }

            if (node.has("transactionId") && node.get("transactionId").canConvertToLong()) {
                return Optional.of(buildTransferContent(node.get("transactionId").asLong()));
            }
        }

        return Optional.empty();
    }

    public Optional<BigDecimal> extractPaidAmount(String payload) {
        JsonNode root = parsePayload(payload);
        List<JsonNode> candidates = new ArrayList<>();
        candidates.add(root);
        if (root.has("data") && root.get("data").isObject()) {
            candidates.add(root.get("data"));
        }

        for (JsonNode node : candidates) {
            for (String field : List.of("amount", "transferAmount", "transfer_amount", "value")) {
                if (!node.has(field)) {
                    continue;
                }

                JsonNode value = node.get(field);
                if (value == null || value.isNull()) {
                    continue;
                }

                if (value.isNumber()) {
                    return Optional.of(value.decimalValue());
                }

                if (value.isTextual()) {
                    String text = value.asText().replace(",", "").trim();
                    if (!text.isBlank()) {
                        try {
                            return Optional.of(new BigDecimal(text));
                        } catch (NumberFormatException ignored) {
                            // Ignore invalid amount and continue searching fallback fields.
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private void validateQrConfig() {
        if (properties.getBankCode() == null || properties.getBankCode().isBlank()) {
            throw new BadRequestException("Missing SePay bank code (SEPAY_BANK_CODE)");
        }
        if (properties.getAccountNumber() == null || properties.getAccountNumber().isBlank()) {
            throw new BadRequestException("Missing SePay account number (SEPAY_ACCOUNT_NUMBER)");
        }
        if (properties.getAccountName() == null || properties.getAccountName().isBlank()) {
            throw new BadRequestException("Missing SePay account name (SEPAY_ACCOUNT_NAME)");
        }
    }

    private String normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Amount is required to generate SePay QR");
        }

        return amount.stripTrailingZeros().toPlainString();
    }

    private String normalizedPrefix() {
        String prefix = properties.getTransferPrefix();
        if (prefix == null || prefix.isBlank()) {
            return "AFM";
        }

        return prefix.trim().toUpperCase(Locale.ROOT);
    }

    private Optional<String> resolveTransferRef(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return Optional.empty();
        }

        String prefix = Pattern.quote(normalizedPrefix());
        Pattern refPattern = Pattern.compile(prefix + "\\d+", Pattern.CASE_INSENSITIVE);

        Matcher exact = refPattern.matcher(candidate.trim());
        if (exact.matches()) {
            return Optional.of(candidate.trim().toUpperCase(Locale.ROOT));
        }

        Matcher found = refPattern.matcher(candidate);
        if (found.find()) {
            return Optional.of(found.group().toUpperCase(Locale.ROOT));
        }

        return Optional.empty();
    }

    private Optional<String> pickFirst(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field)) {
                continue;
            }
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return Optional.of(value.asText().trim());
            }
        }

        return Optional.empty();
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload == null ? "{}" : payload);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid SePay webhook payload");
        }
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);

            byte[] digest = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String part = Integer.toHexString(0xff & b);
                if (part.length() == 1) {
                    hex.append('0');
                }
                hex.append(part);
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to validate SePay webhook signature");
        }
    }
}
