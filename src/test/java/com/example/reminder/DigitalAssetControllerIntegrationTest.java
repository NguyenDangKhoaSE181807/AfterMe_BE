package com.example.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb-digital-assets;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class DigitalAssetControllerIntegrationTest {

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
                                .header("X-Actor-Id", "trusted-contact:" + trustedContact.getId())
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
            .header("X-Actor-Id", "trusted-contact:" + trustedContact.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(consumeBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assetId").value(assetId))
            .andExpect(jsonPath("$.secret").value(plainSecret))
            .andExpect(jsonPath("$.consumedAt").isString());

        mockMvc.perform(post("/api/digital-assets/secrets/{token}/consume", oneTimeToken)
            .header("X-Actor-Id", "trusted-contact:" + trustedContact.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(consumeBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
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
        String requestBody = """
            {
                                    "trustedContactId": %d
            }
            """.formatted(trustedContactId);

        return mockMvc.perform(post("/api/digital-assets/{assetId}/decrypt", assetId)
                                .header("X-Actor-Id", "trusted-contact:" + trustedContactId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody));
        }
}
