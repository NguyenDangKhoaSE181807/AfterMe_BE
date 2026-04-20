package com.example.reminder.service.security;

public interface SecretEncryptionService {

    String generateEncryptionKeyId();

    EncryptionResult encrypt(String plainTextSecret, String encryptionKeyId);

    String decrypt(
            String cipherText,
            String iv,
            String encryptionKeyId,
            String algorithm
    );
}
