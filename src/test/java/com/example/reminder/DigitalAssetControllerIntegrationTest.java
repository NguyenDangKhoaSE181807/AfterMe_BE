package com.example.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.entity.AssetShare;
import com.example.reminder.entity.DigitalAsset;
import com.example.reminder.entity.TrustedContact;
import com.example.reminder.entity.User;
import com.example.reminder.repository.AssetAccessLogRepository;
import com.example.reminder.repository.AssetShareRepository;
import com.example.reminder.repository.DigitalAssetRepository;
import com.example.reminder.repository.DigitalAssetVersionRepository;
import com.example.reminder.repository.TrustedContactRepository;
import com.example.reminder.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb-digital-assets;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.security.jwt.issuer=https://issuer.test",
    "app.security.jwt.audience=reminder-api",
    "app.security.jwt.secret=test-jwt-secret-key-32-bytes-min!!"
})
@AutoConfigureMockMvc
class DigitalAssetControllerIntegrationTest {

    private static final String TEST_ISSUER = "https://issuer.test";
    private static final String TEST_AUDIENCE = "reminder-api";
    private static final String TEST_JWT_SECRET = "test-jwt-secret-key-32-bytes-min!!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DigitalAssetRepository digitalAssetRepository;

    @Autowired
    private AssetShareRepository assetShareRepository;

    @Autowired
    private TrustedContactRepository trustedContactRepository;

    @Autowired
    private DigitalAssetVersionRepository digitalAssetVersionRepository;

    @Autowired
    private AssetAccessLogRepository assetAccessLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    void setUp() {
        assetShareRepository.deleteAll();
        assetAccessLogRepository.deleteAll();
        digitalAssetVersionRepository.deleteAll();
        digitalAssetRepository.deleteAll();
        trustedContactRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("asset-owner@example.com");
        user.setPasswordHash("hashed-password");
        user.setFullName("Asset Owner");
        user.setTonePreference(TonePreference.NORMAL);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);
    }

    @Test
    void createDigitalAsset_shouldPersistEncryptedSecret_andNotExposeSecretInResponse() throws Exception {
        String plainSecret = "my-super-secret-password";
        String requestBody = """
                {
                  "userId": %d,
                  "name": "Gmail Account",
                  "type": "PASSWORD",
                  "identifier": "asset-owner@example.com",
                  "secret": "%s",
                  "instructions": "Login Gmail then check backup codes"
                }
                """.formatted(user.getId(), plainSecret);

        mockMvc.perform(post("/api/digital-assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Gmail Account"))
                .andExpect(jsonPath("$.type").value("PASSWORD"))
                .andExpect(jsonPath("$.identifier").value("asset-owner@example.com"))
                .andExpect(jsonPath("$.instructions").value("Login Gmail then check backup codes"))
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.encryptedSecret").doesNotExist())
                .andExpect(jsonPath("$.encryptionKeyId").doesNotExist());

        List<DigitalAsset> assets = digitalAssetRepository.findByUserIdAndDeletedAtIsNull(user.getId());
        assertThat(assets).hasSize(1);

        DigitalAsset savedAsset = assets.getFirst();
        assertThat(savedAsset.getEncryptedSecret()).isNotBlank().isNotEqualTo(plainSecret);
        assertThat(savedAsset.getEncryptionIv()).isNotBlank();
        assertThat(savedAsset.getEncryptionAlgo()).isEqualTo("AES/GCM/NoPadding");
        assertThat(savedAsset.getEncryptionKeyId()).startsWith("kms-local-");
        assertThat(savedAsset.getIdentifierValue()).isEqualTo("asset-owner@example.com");
        assertThat(savedAsset.getIdentifierType()).isEqualTo("EMAIL");
    }

        @Test
        void decrypt_shouldReturnOneTimeToken_andConsumeShouldWorkOnlyOnce() throws Exception {
        String plainSecret = "vault-secret-123";
        Long assetId = createDigitalAsset(plainSecret);
        TrustedContact trustedContact = createTrustedContact();
        createUnlockedShare(assetId, trustedContact);

        String decryptRequestBody = """
            {
                            "trustedContactId": %d
            }
            """.formatted(trustedContact.getId());

        String decryptResponse = mockMvc.perform(post("/api/digital-assets/{assetId}/decrypt", assetId)
            .header("Authorization", bearerToken("trusted-contact:" + trustedContact.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(decryptRequestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assetId").value(assetId))
            .andExpect(jsonPath("$.oneTimeToken").isString())
            .andExpect(jsonPath("$.expiresAt").isString())
            .andExpect(jsonPath("$.secret").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode decryptJson = objectMapper.readTree(decryptResponse);
        String oneTimeToken = decryptJson.path("oneTimeToken").asText();
        assertThat(oneTimeToken).isNotBlank();

        String consumeBody = "{}";

        mockMvc.perform(post("/api/digital-assets/secrets/{token}/consume", oneTimeToken)
            .header("Authorization", bearerToken("trusted-contact:" + trustedContact.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(consumeBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assetId").value(assetId))
            .andExpect(jsonPath("$.secret").value(plainSecret))
            .andExpect(jsonPath("$.consumedAt").isString());

        mockMvc.perform(post("/api/digital-assets/secrets/{token}/consume", oneTimeToken)
                                .header("Authorization", bearerToken("trusted-contact:" + trustedContact.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(consumeBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        void decrypt_shouldReturnUnauthorized_whenMissingBearerToken() throws Exception {
                Long assetId = createDigitalAsset("missing-token-secret");
                TrustedContact trustedContact = createTrustedContact();
                createShare(assetId, trustedContact, true, "UNLOCKED", LocalDateTime.now().minusHours(2), 0, null);

                String requestBody = """
                                {
                                    "trustedContactId": %d
                                }
                                """.formatted(trustedContact.getId());

                mockMvc.perform(post("/api/digital-assets/{assetId}/decrypt", assetId)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(requestBody))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        void decrypt_shouldReturnUnauthorized_whenTokenHasInvalidAudience() throws Exception {
                Long assetId = createDigitalAsset("bad-audience-secret");
                TrustedContact trustedContact = createTrustedContact();
                createShare(assetId, trustedContact, true, "UNLOCKED", LocalDateTime.now().minusHours(2), 0, null);

                String invalidAudienceToken = bearerTokenWithClaims(
                    "trusted-contact:" + trustedContact.getId(),
                    TEST_ISSUER,
                    "another-api",
                    Instant.now().minusSeconds(10),
                    Instant.now().plusSeconds(120)
                );

                performDecryptWithAuthorizationHeader(assetId, trustedContact.getId(), invalidAudienceToken)
                    .andExpect(status().isUnauthorized())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")));
        }

        @Test
        void decrypt_shouldReturnUnauthorized_whenTokenHasInvalidIssuer() throws Exception {
                Long assetId = createDigitalAsset("bad-issuer-secret");
                TrustedContact trustedContact = createTrustedContact();
                createShare(assetId, trustedContact, true, "UNLOCKED", LocalDateTime.now().minusHours(2), 0, null);

                String invalidIssuerToken = bearerTokenWithClaims(
                    "trusted-contact:" + trustedContact.getId(),
                    "https://issuer.invalid",
                    TEST_AUDIENCE,
                    Instant.now().minusSeconds(10),
                    Instant.now().plusSeconds(120)
                );

                performDecryptWithAuthorizationHeader(assetId, trustedContact.getId(), invalidIssuerToken)
                    .andExpect(status().isUnauthorized())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")));
        }

        @Test
        void decrypt_shouldReturnUnauthorized_whenTokenIsExpired() throws Exception {
                Long assetId = createDigitalAsset("expired-token-secret");
                TrustedContact trustedContact = createTrustedContact();
                createShare(assetId, trustedContact, true, "UNLOCKED", LocalDateTime.now().minusHours(2), 0, null);

                String expiredToken = bearerTokenWithClaims(
                    "trusted-contact:" + trustedContact.getId(),
                    TEST_ISSUER,
                    TEST_AUDIENCE,
                    Instant.now().minusSeconds(300),
                    Instant.now().minusSeconds(120)
                );

                performDecryptWithAuthorizationHeader(assetId, trustedContact.getId(), expiredToken)
                    .andExpect(status().isUnauthorized())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")));
        }

        @Test
        void decrypt_shouldReturnUnauthorized_whenTokenNotBeforeIsInFuture() throws Exception {
                Long assetId = createDigitalAsset("future-nbf-secret");
                TrustedContact trustedContact = createTrustedContact();
                createShare(assetId, trustedContact, true, "UNLOCKED", LocalDateTime.now().minusHours(2), 0, null);

                String futureNbfToken = bearerTokenWithClaims(
                    "trusted-contact:" + trustedContact.getId(),
                    TEST_ISSUER,
                    TEST_AUDIENCE,
                    Instant.now().plusSeconds(120),
                    Instant.now().plusSeconds(300)
                );

                performDecryptWithAuthorizationHeader(assetId, trustedContact.getId(), futureNbfToken)
                    .andExpect(status().isUnauthorized())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")));
        }

        @Test
        void decryptPolicy_shouldPass_whenShareUnlockedAndPolicySatisfied() throws Exception {
        Long assetId = createDigitalAsset("policy-pass-secret");
        TrustedContact trustedContact = createTrustedContact();
        createShare(assetId, trustedContact, true, "UNLOCKED", LocalDateTime.now().minusHours(2), 0, null);

        performDecrypt(assetId, trustedContact.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assetId").value(assetId))
            .andExpect(jsonPath("$.oneTimeToken").isString());
        }

        @Test
        void decryptPolicy_shouldFail_whenShareNotUnlocked() throws Exception {
        Long assetId = createDigitalAsset("policy-fail-locked");
        TrustedContact trustedContact = createTrustedContact();
        createShare(assetId, trustedContact, false, "LOCKED", null, 0, null);

        performDecrypt(assetId, trustedContact.getId())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Asset share is not unlocked"));
        }

        @Test
        void decryptPolicy_shouldFail_whenUnlockDelayNotElapsed() throws Exception {
        Long assetId = createDigitalAsset("policy-fail-delay");
        TrustedContact trustedContact = createTrustedContact();
        createShare(assetId, trustedContact, true, "UNLOCKED", LocalDateTime.now(), 2, null);

        performDecrypt(assetId, trustedContact.getId())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Unlock delay has not elapsed yet"));
        }

        @Test
        void decryptPolicy_shouldFail_whenRequireUserInactiveAndUserStillActive() throws Exception {
        Long assetId = createDigitalAsset("policy-fail-inactive");
        TrustedContact trustedContact = createTrustedContact();
        createShare(
            assetId,
            trustedContact,
            true,
            "UNLOCKED",
            LocalDateTime.now().minusHours(1),
            0,
            "{\"require_user_inactive\":true}"
        );

        performDecrypt(assetId, trustedContact.getId())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Policy requires user to be inactive before decrypt"));
        }

        @Test
        void decryptPolicy_shouldFail_whenRequireMultipleContactsNotMet() throws Exception {
        Long assetId = createDigitalAsset("policy-fail-multi-contact");
        TrustedContact trustedContact = createTrustedContact();
        createShare(
            assetId,
            trustedContact,
            true,
            "UNLOCKED",
            LocalDateTime.now().minusHours(1),
            0,
            "{\"require_multiple_contacts\":2}"
        );

        performDecrypt(assetId, trustedContact.getId())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Policy requires multiple unlocked trusted contacts"));
        }

        private Long createDigitalAsset(String secret) throws Exception {
        String requestBody = """
            {
              "userId": %d,
              "name": "Vault",
              "type": "PASSWORD",
              "identifier": "asset-owner@example.com",
              "secret": "%s",
              "instructions": "Use only in emergency"
            }
            """.formatted(user.getId(), secret);

        String createResponse = mockMvc.perform(post("/api/digital-assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(createResponse).path("id").asLong();
        }

        private TrustedContact createTrustedContact() {
        TrustedContact trustedContact = new TrustedContact();
        trustedContact.setUser(user);
        trustedContact.setFullName("Trusted Contact");
        trustedContact.setRelationship("SIBLING");
        trustedContact.setEmail("trusted@example.com");
        trustedContact.setIsActive(true);
        trustedContact.setCreatedAt(LocalDateTime.now());
        return trustedContactRepository.save(trustedContact);
        }

        private void createUnlockedShare(Long assetId, TrustedContact trustedContact) {
        createShare(assetId, trustedContact, true, "UNLOCKED", LocalDateTime.now().minusMinutes(5), 0, null);
        }

        private void createShare(
            Long assetId,
            TrustedContact trustedContact,
            boolean isUnlocked,
            String unlockStatus,
            LocalDateTime unlockedAt,
            Integer unlockDelayHours,
            String unlockPolicy
        ) {
        DigitalAsset asset = digitalAssetRepository.findByIdAndDeletedAtIsNull(assetId).orElseThrow();

        AssetShare share = new AssetShare();
        share.setDigitalAsset(asset);
        share.setTrustedContact(trustedContact);
        share.setUnlockCondition("MANUAL");
        share.setIsUnlocked(isUnlocked);
        share.setUnlockStatus(unlockStatus);
        share.setUnlockedBy("SYSTEM");
        share.setUnlockedAt(unlockedAt);
        share.setUnlockDelayHours(unlockDelayHours);
        share.setUnlockPolicy(unlockPolicy);
        share.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        assetShareRepository.save(share);
        }

        private ResultActions performDecrypt(Long assetId, Long trustedContactId) throws Exception {
        return performDecryptWithAuthorizationHeader(assetId, trustedContactId, bearerToken("trusted-contact:" + trustedContactId));
        }

        private ResultActions performDecryptWithAuthorizationHeader(
            Long assetId,
            Long trustedContactId,
            String authorizationHeader
        ) throws Exception {
        String requestBody = """
            {
                                    "trustedContactId": %d
            }
            """.formatted(trustedContactId);

        return mockMvc.perform(post("/api/digital-assets/{assetId}/decrypt", assetId)
            .header("Authorization", authorizationHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody));
        }

        private String bearerToken(String actorId) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(TEST_ISSUER)
            .subject(actorId)
            .audience(TEST_AUDIENCE)
            .issueTime(Date.from(now))
            .notBeforeTime(Date.from(now.minusSeconds(5)))
            .expirationTime(Date.from(now.plusSeconds(120)))
            .jwtID(UUID.randomUUID().toString())
            .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(new MACSigner(TEST_JWT_SECRET));
        return "Bearer " + signedJWT.serialize();
        }

        private String bearerTokenWithClaims(
            String actorId,
            String issuer,
            String audience,
            Instant notBefore,
            Instant expiration
        ) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(actorId)
            .audience(audience)
            .issueTime(Date.from(notBefore))
            .notBeforeTime(Date.from(notBefore))
            .expirationTime(Date.from(expiration))
            .jwtID(UUID.randomUUID().toString())
            .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(new MACSigner(TEST_JWT_SECRET));
        return "Bearer " + signedJWT.serialize();
        }
}
