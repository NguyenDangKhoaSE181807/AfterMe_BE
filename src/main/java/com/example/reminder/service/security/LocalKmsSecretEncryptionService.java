package com.example.reminder.service.security;

import com.example.reminder.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalKmsSecretEncryptionService implements SecretEncryptionService {

    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] masterKeyBytes;

    public LocalKmsSecretEncryptionService(
            @Value("${app.security.kms.master-key-base64:MzIxQkUtREVWT1AtTUFTVEVSLUtFWQ==}") String masterKeyBase64
    ) {
        this.masterKeyBytes = Base64.getDecoder().decode(masterKeyBase64);
    }

    @Override
    public String generateEncryptionKeyId() {
        return "kms-local-" + UUID.randomUUID();
    }

    @Override
    public EncryptionResult encrypt(String plainTextSecret, String encryptionKeyId) {
        if (plainTextSecret == null || plainTextSecret.isBlank()) {
            throw new BadRequestException("Secret must not be blank");
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            SecretKeySpec key = deriveDataKey(encryptionKeyId);
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] encrypted = cipher.doFinal(plainTextSecret.getBytes(StandardCharsets.UTF_8));
            return new EncryptionResult(
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(iv),
                    AES_GCM_ALGORITHM
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encrypt secret", ex);
        }
    }

    private SecretKeySpec deriveDataKey(String encryptionKeyId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(masterKeyBytes);
            digest.update(encryptionKeyId.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes = Arrays.copyOf(digest.digest(), 16);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to derive encryption key", ex);
        }
    }
}
