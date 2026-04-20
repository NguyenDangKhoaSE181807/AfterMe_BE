package com.example.reminder.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authEx) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), Map.of(
                            "code", "UNAUTHORIZED",
                            "message", "Authentication required",
                            "timestamp", LocalDateTime.now().toString()
                    ));
                                }).accessDeniedHandler((request, response, accessDeniedEx) -> {
                                        response.setStatus(403);
                                        response.setContentType("application/json");
                                        objectMapper.writeValue(response.getWriter(), Map.of(
                                                        "code", "FORBIDDEN",
                                                        "message", "Access denied",
                                                        "timestamp", LocalDateTime.now().toString()
                                        ));
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/digital-assets/*/decrypt").authenticated()
                        .requestMatchers("/api/digital-assets/secrets/*/consume").authenticated()
                        .anyRequest().permitAll()
                                )
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

        @Bean
        public JwtDecoder jwtDecoder(
                        @Value("${app.security.jwt.issuer}") String issuer,
                        @Value("${app.security.jwt.audience}") String audience,
                        @Value("${app.security.jwt.jwk-set-uri:}") String jwkSetUri,
                        @Value("${app.security.jwt.secret:}") String secret
        ) {
                NimbusJwtDecoder jwtDecoder;
                if (jwkSetUri != null && !jwkSetUri.isBlank()) {
                        jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
                } else if (secret != null && !secret.isBlank()) {
                        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                        jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                                        .macAlgorithm(MacAlgorithm.HS256)
                                        .build();
                } else {
                        throw new IllegalStateException("JWT decoder misconfigured: provide app.security.jwt.jwk-set-uri or app.security.jwt.secret");
                }

                OAuth2TokenValidator<Jwt> defaultWithIssuer = JwtValidators.createDefaultWithIssuer(issuer);
                OAuth2TokenValidator<Jwt> audienceValidator = token -> {
                        if (token.getAudience().contains(audience)) {
                                return OAuth2TokenValidatorResult.success();
                        }

                        return OAuth2TokenValidatorResult.failure(
                                        new OAuth2Error("invalid_token", "The required audience is missing", null)
                        );
                };

                jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultWithIssuer, audienceValidator));
                return jwtDecoder;
        }
}
