package com.example.reminder.config;

import com.example.reminder.dto.common.BaseResponse;
import com.example.reminder.dto.common.ErrorDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.convert.converter.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

        @Value("${app.cors.allowed-origins}")
        private String[] allowedOrigins;

    private BaseResponse<Void> buildErrorResponse(
            String code,
            String message,
            List<ErrorDetail> errors,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        return BaseResponse.<Void>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .errors(errors)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .requestId(request.getHeader("X-Request-Id"))
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authEx) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), buildErrorResponse(
                            "UNAUTHORIZED",
                            "Authentication required",
                            List.of(new ErrorDetail("UNAUTHORIZED", "Authentication required", null)),
                            request
                    ));
                                }).accessDeniedHandler((request, response, accessDeniedEx) -> {
                                        response.setStatus(403);
                                        response.setContentType("application/json");
                                        objectMapper.writeValue(response.getWriter(), buildErrorResponse(
                                                        "FORBIDDEN",
                                                        "Access denied",
                                                        List.of(new ErrorDetail("FORBIDDEN", "Access denied", null)),
                                                        request
                                        ));
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/sign-up",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/auth/sign-in",
                                "/api/auth/refresh-token",
                                "/api/auth/log-out",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/notifications/**").hasAnyRole("ADMIN", "CONSULTANT")
                        .requestMatchers("/api/habits/**", "/api/reminders/**").hasAnyRole("ADMIN", "CUSTOMER", "CONSULTANT")
                        .requestMatchers(HttpMethod.POST, "/api/digital-assets/*/decrypt").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/digital-assets/secrets/*/consume").authenticated()
                        .requestMatchers("/api/digital-assets/**").permitAll()
                        .anyRequest().authenticated()
                                )
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

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
                        SecretKey secretKey = new SecretKeySpec(resolveSecretBytes(secret), "HmacSHA256");
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

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(java.util.List.of(allowedOrigins));
                configuration.setAllowedMethods(java.util.List.of("*"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
                JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
                authoritiesConverter.setAuthorityPrefix("ROLE_");
                authoritiesConverter.setAuthoritiesClaimName("roles");

                JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
                authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
                return authenticationConverter;
        }

        private byte[] resolveSecretBytes(String secret) {
                byte[] utf8Bytes = secret.getBytes(StandardCharsets.UTF_8);
                if (utf8Bytes.length >= 32) {
                        return utf8Bytes;
                }

                return java.util.Base64.getDecoder().decode(secret);
        }
}
