package com.marketinghub.ads;

import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
public class FacebookAccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookAccountRepository repository;

    @Test
    void shouldReturnAccountsWithTokenStatus() throws Exception {
        repository.save(FacebookAccount.builder()
                .name("Account valid")
                .currency("USD")
                .accessToken("token-valid")
                .tokenExpiresAt(LocalDateTime.now().plusDays(30))
                .authorizedUserId("123")
                .authorizedUserName("Marketing Hub")
                .authorizedUserEmail("contato@example.com")
                .build());

        repository.save(FacebookAccount.builder()
                .name("Account expiring")
                .currency("USD")
                .accessToken("token-expiring")
                .tokenExpiresAt(LocalDateTime.now().plusDays(2))
                .build());

        repository.save(FacebookAccount.builder()
                .name("Account missing token")
                .currency("USD")
                .build());

        mockMvc.perform(get("/api/accounts/facebook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.name=='Account valid')].requiresTokenRenewal", contains(false)))
                .andExpect(jsonPath("$[?(@.name=='Account expiring')].requiresTokenRenewal", contains(true)))
                .andExpect(jsonPath("$[?(@.name=='Account missing token')].requiresTokenRenewal", contains(true)))
                .andExpect(jsonPath("$[?(@.name=='Account expiring')].tokenExpired", contains(false)));
    }

    @Test
    void shouldPersistAppCredentialsOnCreate() throws Exception {
        repository.deleteAll();

        String payload = "{" +
            "\"name\":\"Conta BM\"," +
            "\"currency\":\"BRL\"," +
            "\"appId\":\" 123456 \"," +
            "\"appSecret\":\" segredo-super-seguro \"," +
            "\"tokenRenewalEnabled\":true" +
            "}";

        mockMvc.perform(post("/api/accounts/facebook")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isOk());

        FacebookAccount saved = repository.findAll().stream().findFirst().orElseThrow();
        assertThat(saved.getAppId()).isEqualTo("123456");
        assertThat(saved.getAppSecret()).isEqualTo("segredo-super-seguro");
        assertThat(saved.isTokenRenewalEnabled()).isTrue();

        repository.deleteAll();
    }

    @Test
    void shouldExposeHasAppSecretFlagAfterSavingSecret() throws Exception {
        repository.deleteAll();

        repository.save(FacebookAccount.builder()
            .name("Conta com segredo")
            .currency("BRL")
            .appSecret("segredo-armazenado")
            .build());

        mockMvc.perform(get("/api/accounts/facebook"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hasAppSecret").value(true));

        repository.deleteAll();
    }

    @Test
    void shouldReturnAccountsEligibleForRenewal() throws Exception {
        FacebookAccount eligible = repository.save(FacebookAccount.builder()
            .name("Eligible")
            .currency("USD")
            .accessToken("token-eligible")
            .tokenExpiresAt(LocalDateTime.now().plusDays(1))
            .appId("123")
            .appSecret("secret")
            .tokenRenewalEnabled(true)
            .build());

        repository.save(FacebookAccount.builder()
            .name("Disabled")
            .currency("USD")
            .accessToken("token-disabled")
            .tokenExpiresAt(LocalDateTime.now().plusDays(1))
            .appId("123")
            .appSecret("secret")
            .tokenRenewalEnabled(false)
            .build());

        mockMvc.perform(get("/api/accounts/facebook/renewal/eligible"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(eligible.getId()))
            .andExpect(jsonPath("$[0].accessToken").value("token-eligible"))
            .andExpect(jsonPath("$[0].appSecret").value("secret"));
    }

    @Test
    void shouldRegisterSuccessfulRenewal() throws Exception {
        FacebookAccount account = repository.save(FacebookAccount.builder()
            .name("Needs Renewal")
            .currency("USD")
            .accessToken("old-token")
            .tokenExpiresAt(LocalDateTime.now().plusDays(1))
            .appId("app-1")
            .appSecret("secret")
            .tokenRenewalEnabled(true)
            .build());

        String payload = "{" +
            "\"status\":\"SUCCESS\"," +
            "\"accessToken\":\"new-token\"," +
            "\"tokenExpiresAt\":\"2030-01-01T00:00:00\"," +
            "\"renewedAt\":\"2029-12-31T23:00:00\"" +
            "}";

        mockMvc.perform(post("/api/accounts/facebook/" + account.getId() + "/token/renewal")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isAccepted());

        FacebookAccount updated = repository.findById(account.getId()).orElseThrow();
        assertThat(updated.getAccessToken()).isEqualTo("new-token");
        assertThat(updated.getTokenExpiresAt()).isEqualTo(LocalDateTime.parse("2030-01-01T00:00:00"));
        assertThat(updated.getTokenRenewedAt()).isEqualTo(LocalDateTime.parse("2029-12-31T23:00:00"));
        assertThat(updated.getTokenRenewalStatus()).isEqualTo(FacebookTokenRenewalStatus.SUCCESS.name());
        assertThat(updated.getTokenRenewalLastError()).isNull();
    }

    @Test
    void shouldRejectRevalidationWhenTokenMissing() throws Exception {
        FacebookAccount account = repository.save(FacebookAccount.builder()
            .name("Missing token")
            .currency("USD")
            .appId("app-1")
            .appSecret("secret")
            .build());

        mockMvc.perform(post("/api/accounts/facebook/" + account.getId() + "/token/revalidation"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRevalidationWhenAppCredentialsMissing() throws Exception {
        FacebookAccount account = repository.save(FacebookAccount.builder()
            .name("Missing app id")
            .currency("USD")
            .accessToken("token")
            .build());

        mockMvc.perform(post("/api/accounts/facebook/" + account.getId() + "/token/revalidation"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPreserveTokenAndHiddenFieldsWhenUpdatingAccountWithoutExplicitValues() throws Exception {
        FacebookAccount account = repository.save(FacebookAccount.builder()
            .name("Original")
            .currency("BRL")
            .accessToken("original-token")
            .tokenExpiresAt(LocalDateTime.parse("2024-01-05T10:00:00"))
            .tokenLastRefreshedAt(LocalDateTime.parse("2024-01-01T08:00:00"))
            .tokenRenewalStatus(FacebookTokenRenewalStatus.SUCCESS.name())
            .tokenRenewalLastAttemptAt(LocalDateTime.parse("2023-12-31T12:00:00"))
            .tokenRenewedAt(LocalDateTime.parse("2023-12-30T15:00:00"))
            .tokenRenewalLastError("previous-error")
            .defaultPageId("page-123")
            .defaultInstagramActorId("ig-456")
            .defaultWebsiteUrl("https://old.example.com")
            .defaultLeadGenFormId("form-old")
            .defaultCreativeMessageTemplate("Campanha antiga %s")
            .defaultCallToActionType("SIGN_UP")
            .adAccountId("act_old")
            .adSetDailyBudget("1500")
            .adSetBillingEvent("IMPRESSIONS")
            .adSetOptimizationGoal("LINK_CLICKS")
            .adSetDestinationType("WEBSITE")
            .adSetBidStrategy("LOWEST_COST_WITHOUT_CAP")
            .adSetBidAmount("500")
            .adSetTargetCountry("BR")
            .tokenRenewalEnabled(true)
            .workerEnabled(true)
            .authorizedUserId("auth-1")
            .authorizedUserName("Original User")
            .authorizedUserEmail("original@example.com")
            .appId("old-app")
            .build());

        String payload = ("{" +
            "\"id\":" + account.getId() + ',' +
            "\"name\":\"Atualizada\"," +
            "\"currency\":\"BRL\"," +
            "\"authorizedUserId\":\"auth-2\"," +
            "\"authorizedUserName\":\"Usuário Atualizado\"," +
            "\"authorizedUserEmail\":\"updated@example.com\"," +
            "\"appId\":\"new-app\"," +
            "\"tokenRenewalEnabled\":false," +
            "\"adAccountId\":\"act_new\"," +
            "\"defaultWebsiteUrl\":\"https://new.example.com\"," +
            "\"defaultLeadGenFormId\":\"form-new\"," +
            "\"defaultCreativeMessageTemplate\":\"Nova campanha %s\"," +
            "\"defaultCallToActionType\":\"LEARN_MORE\"," +
            "\"adSetDailyBudget\":\"2000\"," +
            "\"adSetBillingEvent\":\"IMPRESSIONS\"," +
            "\"adSetOptimizationGoal\":\"LINK_CLICKS\"," +
            "\"adSetDestinationType\":\"WEBSITE\"," +
            "\"adSetBidStrategy\":\"LOWEST_COST_WITHOUT_CAP\"," +
            "\"adSetBidAmount\":\"600\"," +
            "\"adSetTargetCountry\":\"BR\"," +
            "\"workerEnabled\":false" +
            "}");

        mockMvc.perform(put("/api/accounts/facebook/" + account.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isOk());

        FacebookAccount updated = repository.findById(account.getId()).orElseThrow();
        assertThat(updated.getAccessToken()).isEqualTo("original-token");
        assertThat(updated.getTokenExpiresAt()).isEqualTo(LocalDateTime.parse("2024-01-05T10:00:00"));
        assertThat(updated.getTokenLastRefreshedAt()).isEqualTo(LocalDateTime.parse("2024-01-01T08:00:00"));
        assertThat(updated.getTokenRenewalStatus()).isEqualTo(FacebookTokenRenewalStatus.SUCCESS.name());
        assertThat(updated.getTokenRenewalLastAttemptAt()).isEqualTo(LocalDateTime.parse("2023-12-31T12:00:00"));
        assertThat(updated.getTokenRenewedAt()).isEqualTo(LocalDateTime.parse("2023-12-30T15:00:00"));
        assertThat(updated.getTokenRenewalLastError()).isEqualTo("previous-error");
        assertThat(updated.getDefaultPageId()).isEqualTo("page-123");
        assertThat(updated.getDefaultInstagramActorId()).isEqualTo("ig-456");

        assertThat(updated.getName()).isEqualTo("Atualizada");
        assertThat(updated.getAuthorizedUserId()).isEqualTo("auth-2");
        assertThat(updated.getAuthorizedUserName()).isEqualTo("Usuário Atualizado");
        assertThat(updated.getAuthorizedUserEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getAppId()).isEqualTo("new-app");
        assertThat(updated.isTokenRenewalEnabled()).isFalse();
        assertThat(updated.getAdAccountId()).isEqualTo("act_new");
        assertThat(updated.getDefaultWebsiteUrl()).isEqualTo("https://new.example.com");
        assertThat(updated.getDefaultLeadGenFormId()).isEqualTo("form-new");
        assertThat(updated.getDefaultCreativeMessageTemplate()).isEqualTo("Nova campanha %s");
        assertThat(updated.getDefaultCallToActionType()).isEqualTo("LEARN_MORE");
        assertThat(updated.getAdSetDailyBudget()).isEqualTo("2000");
        assertThat(updated.getAdSetBillingEvent()).isEqualTo("IMPRESSIONS");
        assertThat(updated.getAdSetOptimizationGoal()).isEqualTo("LINK_CLICKS");
        assertThat(updated.getAdSetDestinationType()).isEqualTo("WEBSITE");
        assertThat(updated.getAdSetBidStrategy()).isEqualTo("LOWEST_COST_WITHOUT_CAP");
        assertThat(updated.getAdSetBidAmount()).isEqualTo("600");
        assertThat(updated.getAdSetTargetCountry()).isEqualTo("BR");
        assertThat(updated.isWorkerEnabled()).isFalse();
    }

    @Test
    void shouldExposeWorkerConfigurationWhenAccountEnabled() throws Exception {
        repository.deleteAll();
        FacebookAccount account = repository.save(FacebookAccount.builder()
            .name("Worker Account")
            .currency("BRL")
            .accessToken("worker-token")
            .adAccountId("1234567890")
            .defaultWebsiteUrl("https://example.com")
            .defaultCreativeMessageTemplate("Campanha %s")
            .defaultCallToActionType("SIGN_UP")
            .adSetDailyBudget("2000")
            .adSetBillingEvent("IMPRESSIONS")
            .adSetOptimizationGoal("LINK_CLICKS")
            .adSetDestinationType("WEBSITE")
            .adSetBidStrategy("LOWEST_COST_WITHOUT_CAP")
            .adSetTargetCountry("BR")
            .pixelOwnerBusinessId("123456789012345")
            .workerEnabled(true)
            .build());

        mockMvc.perform(get("/api/accounts/facebook/worker-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(account.getId()))
            .andExpect(jsonPath("$.accessToken").value("worker-token"))
            .andExpect(jsonPath("$.adAccountId").value("1234567890"))
            .andExpect(jsonPath("$.defaultWebsiteUrl").value("https://example.com"))
            .andExpect(jsonPath("$.adSetDailyBudget").value("2000"))
            .andExpect(jsonPath("$.adSetBillingEvent").value("IMPRESSIONS"))
            .andExpect(jsonPath("$.defaultCallToActionType").value("SIGN_UP"))
            .andExpect(jsonPath("$.defaultCreativeMessageTemplate").value("Campanha %s"))
            .andExpect(jsonPath("$.pixelOwnerBusinessId").value("123456789012345"));
    }

    @Test
    void shouldExposeWorkerConfigurationWhenDefaultWebsiteMissing() throws Exception {
        repository.deleteAll();
        FacebookAccount account = repository.save(FacebookAccount.builder()
            .name("Worker Account")
            .currency("BRL")
            .accessToken("worker-token")
            .adAccountId("1234567890")
            .defaultCreativeMessageTemplate("Campanha %s")
            .defaultCallToActionType("SIGN_UP")
            .adSetDailyBudget("2000")
            .adSetBillingEvent("IMPRESSIONS")
            .adSetOptimizationGoal("LINK_CLICKS")
            .adSetDestinationType("WEBSITE")
            .adSetBidStrategy("LOWEST_COST_WITHOUT_CAP")
            .adSetTargetCountry("BR")
            .pixelOwnerBusinessId("123456789012345")
            .workerEnabled(true)
            .build());

        mockMvc.perform(get("/api/accounts/facebook/worker-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(account.getId()))
            .andExpect(jsonPath("$.defaultWebsiteUrl").value(nullValue()));

        FacebookAccount persisted = repository.findById(account.getId()).orElseThrow();
        assertThat(persisted.getWorkerLastValidationAt()).isNotNull();
        assertThat(persisted.getWorkerLastValidationErrorCode()).isNull();
        assertThat(persisted.getWorkerLastValidationErrorDetail()).isNull();
    }

    @Test
    void shouldReturnBadRequestWhenWorkerConfigurationIncomplete() throws Exception {
        repository.deleteAll();
        repository.save(FacebookAccount.builder()
            .name("Worker Account")
            .currency("BRL")
            .accessToken("worker-token")
            .defaultWebsiteUrl("https://example.com")
            .adSetDailyBudget("2000")
            .adSetBillingEvent("IMPRESSIONS")
            .adSetOptimizationGoal("LINK_CLICKS")
            .adSetDestinationType("WEBSITE")
            .adSetTargetCountry("BR")
            .pixelOwnerBusinessId("123456789012345")
            .workerEnabled(true)
            .build());

        mockMvc.perform(get("/api/accounts/facebook/worker-config"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Facebook worker account is missing ad account id"));
    }

    @Test
    void shouldPersistWorkerValidationErrorsAndClearOnSuccess() throws Exception {
        repository.deleteAll();
        FacebookAccount account = repository.save(FacebookAccount.builder()
            .name("Worker Account")
            .currency("BRL")
            .accessToken("worker-token")
            .defaultWebsiteUrl("https://example.com")
            .adSetDailyBudget("2000")
            .adSetBillingEvent("IMPRESSIONS")
            .adSetOptimizationGoal("LINK_CLICKS")
            .adSetDestinationType("WEBSITE")
            .adSetTargetCountry("BR")
            .pixelOwnerBusinessId("123456789012345")
            .workerEnabled(true)
            .build());

        mockMvc.perform(get("/api/accounts/facebook/worker-config"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Facebook worker account is missing ad account id"));

        FacebookAccount failed = repository.findById(account.getId()).orElseThrow();
        assertThat(failed.getWorkerLastValidationAt()).isNotNull();
        assertThat(failed.getWorkerLastValidationErrorCode()).isEqualTo("AD_ACCOUNT_ID_MISSING");
        assertThat(failed.getWorkerLastValidationErrorDetail())
            .isEqualTo("Facebook worker account is missing ad account id");

        failed.setAdAccountId("1234567890");
        repository.save(failed);

        mockMvc.perform(get("/api/accounts/facebook/worker-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.adAccountId").value("1234567890"));

        FacebookAccount succeeded = repository.findById(account.getId()).orElseThrow();
        assertThat(succeeded.getWorkerLastValidationAt()).isNotNull();
        assertThat(succeeded.getWorkerLastValidationErrorCode()).isNull();
        assertThat(succeeded.getWorkerLastValidationErrorDetail()).isNull();
    }
}
