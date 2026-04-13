package com.example.reminder.service.security;

public record EncryptionResult(
        String cipherText,
        String iv,
        String algorithm
) {
}
