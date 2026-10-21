package com.marketinghub.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookads.controller.FacebookAccountController;
import com.marketinghub.facebookads.service.token.*;
import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import com.marketinghub.web.ApiExceptionHandler;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Homologa a passagem tela → controller → Meta simulada → banco, sem credenciais reais. */
@DataJpaTest(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"},
    showSql = false)
@ExtendWith(OutputCaptureExtension.class)
class FacebookTokenReplacementTest {
  private static final String CANDIDATE = "synthetic-candidate-token";
  @Autowired private FacebookAccountRepository repository;
  private MockRestServiceServer meta;
  private MockMvc mvc;
  private FacebookAccount account;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** Prepara conta e transporte isolados; nenhuma chamada alcança a Meta real. */
  @BeforeEach
  void setUp() {
    repository.deleteAll();
    account =
        repository.saveAndFlush(
            FacebookAccount.builder()
                .name("Conta de teste")
                .currency("BRL")
                .accessToken("synthetic-old-token")
                .appId("12345")
                .appSecret("synthetic-app-secret")
                .authorizedUserId("9988")
                .adAccountId("7777")
                .tokenRenewalEnabled(true)
                .workerEnabled(true)
                .adSetDailyBudget("2000")
                .tokenRenewalStatus("FAILED")
                .tokenRenewalLastError("erro antigo")
                .tokenExpiresAt(LocalDateTime.now().minusDays(2))
                .build());
    RestTemplateBuilder builder =
        new RestTemplateBuilder()
            .additionalCustomizers(
                template -> meta = MockRestServiceServer.bindTo(template).build());
    var verifier =
        new FacebookTokenVerificationClient(builder, "https://graph.facebook.test", "v23.0");
    var service = new FacebookTokenReplacementService(repository, verifier);
    mvc =
        MockMvcBuilders.standaloneSetup(
                new FacebookAccountController(
                    repository,
                    mock(FacebookTokenRevalidationService.class),
                    mock(FacebookTokenDiagnosticsService.class),
                    service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
  }

  /** Monta resposta válida do provedor sem presumir duração fixa. */
  private String validDebug() {
    return "{\"data\":{\"is_valid\":true,\"type\":\"USER\",\"app_id\":\"12345\",\"user_id\":\"9988\",\"expires_at\":"
        + Instant.now().plusSeconds(3600).getEpochSecond()
        + ",\"scopes\":[\"ads_read\",\"ads_management\"]}}";
  }

  /** Configura a inspeção da candidata e exige autorização do aplicativo no cabeçalho. */
  private void debug(String body) {
    meta.expect(requestTo("https://graph.facebook.test/v23.0/debug_token?input_token=" + CANDIDATE))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer 12345|synthetic-app-secret"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }

  /** Confere acesso somente à conta anunciadora cadastrada. */
  private void adAccount() {
    meta.expect(requestTo("https://graph.facebook.test/v23.0/act_7777?fields=id,account_id"))
        .andExpect(header("Authorization", "Bearer " + CANDIDATE))
        .andRespond(
            withSuccess(
                "{\"id\":\"act_7777\",\"account_id\":\"7777\"}", MediaType.APPLICATION_JSON));
  }

  /** Envia a candidata pelo contrato HTTP usado pela interface. */
  private org.springframework.test.web.servlet.ResultActions submit(String token) throws Exception {
    return mvc.perform(
        post("/api/accounts/facebook/" + account.getId() + "/token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("accessToken", token))));
  }

  /** Demonstra a atualização em duas identidades, preservando orçamento, worker e automação. */
  @ParameterizedTest
  @ValueSource(strings = {"Capella QA", "Outra empresa QA"})
  void updatesOnlyVerifiedCredential(String name, CapturedOutput output) throws Exception {
    account.setName(name);
    repository.saveAndFlush(account);
    debug(validDebug());
    adAccount();
    String response =
        submit(" " + CANDIDATE + " ")
            .andExpect(status().isOk())
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                    .string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.accountId").value(account.getId()))
            .andExpect(jsonPath("$.tokenRenewalEnabled").value(true))
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    meta.verify();
    FacebookAccount saved = repository.findById(account.getId()).orElseThrow();
    assertThat(saved.getAccessToken()).isEqualTo(CANDIDATE);
    assertThat(saved.getTokenExpiresAt())
        .isAfter(LocalDateTime.now())
        .isBefore(LocalDateTime.now().plusHours(2));
    assertThat(saved.getTokenRenewalLastError()).isNull();
    assertThat(saved.isTokenRenewalEnabled()).isTrue();
    assertThat(saved.isWorkerEnabled()).isTrue();
    assertThat(saved.getAdSetDailyBudget()).isEqualTo("2000");
    String listing =
        mvc.perform(get("/api/accounts/facebook"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hasAccessToken").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response + listing + saved + output)
        .doesNotContain(CANDIDATE, "synthetic-app-secret", "synthetic-old-token");
  }

  /** Rejeita validade falsa, titular/aplicativo trocado, tipo errado e permissões insuficientes. */
  @ParameterizedTest
  @ValueSource(
      strings = {"invalid", "app", "user", "type", "expired", "scopes", "accessExpired", "missing"})
  void refusesInvalidEvidence(String failure) throws Exception {
    var data =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(validDebug()).path("data");
    switch (failure) {
      case "invalid" -> data.put("is_valid", false);
      case "app" -> data.put("app_id", "other-app");
      case "user" -> data.put("user_id", "other-user");
      case "type" -> data.put("type", "PAGE");
      case "expired" -> data.put("expires_at", 1);
      case "scopes" -> data.putArray("scopes").add("ads_read");
      case "accessExpired" -> data.put("data_access_expires_at", 1);
      case "missing" -> data.remove("expires_at");
    }
    debug(json.writeValueAsString(Map.of("data", data)));
    submit(CANDIDATE).andExpect(status().isUnprocessableEntity());
    assertThat(repository.findById(account.getId()).orElseThrow().getAccessToken())
        .isEqualTo("synthetic-old-token");
    meta.verify();
  }

  /** Não grava quando a conta de anúncios não pertence à candidata. */
  @Test
  void rejectsWrongAdAccount() throws Exception {
    debug(validDebug());
    meta.expect(requestTo("https://graph.facebook.test/v23.0/act_7777?fields=id,account_id"))
        .andRespond(
            withSuccess(
                "{\"id\":\"act_9999\",\"account_id\":\"9999\"}", MediaType.APPLICATION_JSON));
    submit(CANDIDATE).andExpect(status().isUnprocessableEntity());
    meta.verify();
    assertThat(repository.findById(account.getId()).orElseThrow().getAccessToken())
        .isEqualTo("synthetic-old-token");
  }

  /** Preserva a credencial e não repete a chamada quando o provedor falha ou rejeita. */
  @ParameterizedTest
  @ValueSource(ints = {400, 401, 429, 503})
  void sanitizesProviderFailure(int status, CapturedOutput output) throws Exception {
    meta.expect(requestTo("https://graph.facebook.test/v23.0/debug_token?input_token=" + CANDIDATE))
        .andRespond(
            withStatus(HttpStatus.valueOf(status))
                .body("error " + CANDIDATE + " synthetic-app-secret"));
    String response =
        submit(CANDIDATE)
            .andExpect(status().is(status < 429 ? 422 : 503))
            .andReturn()
            .getResponse()
            .getContentAsString();
    meta.verify();
    assertThat(response + output).doesNotContain(CANDIDATE, "synthetic-app-secret");
    assertThat(repository.findById(account.getId()).orElseThrow().getAccessToken())
        .isEqualTo("synthetic-old-token");
  }

  /** Recusa formatos indevidos antes da consulta ou gravação. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "Bearer synthetic-token",
        "https://example.test/?access_token=synthetic",
        "bad token"
      })
  void rejectsMalformedInput(String token) throws Exception {
    submit(token).andExpect(status().isBadRequest());
    meta.verify();
  }

  /** Não deixa uma validação antiga substituir uma renovação já gravada. */
  @Test
  void preventsConcurrentOverwrite() {
    account.setAccessToken("concurrent-token");
    repository.saveAndFlush(account);
    int result =
        repository.replaceVerifiedToken(
            account.getId(),
            "synthetic-old-token",
            "12345",
            "synthetic-app-secret",
            "7777",
            "9988",
            CANDIDATE,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now());
    assertThat(result).isZero();
    assertThat(repository.findById(account.getId()).orElseThrow().getAccessToken())
        .isEqualTo("concurrent-token");
  }

  /** Confirma que credenciais não são serializadas ou exibidas pelo DTO. */
  @Test
  void redactsRequestObject() throws Exception {
    var request = new FacebookTokenReplacementRequest(CANDIDATE);
    assertThat(request + json.writeValueAsString(request)).doesNotContain(CANDIDATE);
  }

  /** JSON malformado não pode despejar a credencial no log global de binding. */
  @Test
  void hidesMalformedJson(CapturedOutput output) throws Exception {
    mvc.perform(
            post("/api/accounts/facebook/" + account.getId() + "/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accessToken\":\"synthetic-candidate-token\", invalid}"))
        .andExpect(status().isBadRequest());
    assertThat(output.toString()).doesNotContain(CANDIDATE);
  }

  /** Conta sem credenciais do aplicativo não deve consultar a Meta. */
  @Test
  void rejectsMissingAppConfiguration() throws Exception {
    account.overwriteAppSecret(null);
    repository.saveAndFlush(account);
    submit(CANDIDATE).andExpect(status().isConflict());
    meta.verify();
    assertThat(repository.findById(account.getId()).orElseThrow().getAccessToken())
        .isEqualTo("synthetic-old-token");
  }

  /** A substituição preserva também uma automação que o operador já havia desativado. */
  @Test
  void preservesDisabledRenewal() throws Exception {
    account.setTokenRenewalEnabled(false);
    repository.saveAndFlush(account);
    debug(validDebug());
    adAccount();
    submit(CANDIDATE)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tokenRenewalEnabled").value(false));
    assertThat(repository.findById(account.getId()).orElseThrow().isTokenRenewalEnabled())
        .isFalse();
  }
}
