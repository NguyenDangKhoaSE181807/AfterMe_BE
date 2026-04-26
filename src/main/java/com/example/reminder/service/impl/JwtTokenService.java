package com.example.reminder.service.impl;

import com.example.reminder.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final String issuer;
    private final String audience;
    private final long accessTokenTtlSeconds;
    private final Key key;

    public JwtTokenService(
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.audience}") String audience,
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds
    ) {
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.key = resolveSigningKey(secret);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenTtlSeconds);

        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("uid", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    private Key resolveSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is required for token generation");
        }

        byte[] utf8Bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (utf8Bytes.length >= 32) {
            return Keys.hmacShaKeyFor(utf8Bytes);
        }

        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
