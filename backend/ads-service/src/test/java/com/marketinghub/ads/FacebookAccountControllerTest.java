package com.marketinghub.ads;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
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
            .andExpect(jsonPath("$.defaultCreativeMessageTemplate").value("Campanha %s"));
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
