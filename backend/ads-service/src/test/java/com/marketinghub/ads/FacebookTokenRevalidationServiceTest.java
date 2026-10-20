package com.marketinghub.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** Verifica validade oficial e preservação segura da credencial durante a renovação. */
@ExtendWith(OutputCaptureExtension.class)
class FacebookTokenRevalidationServiceTest {
  private FacebookTokenRevalidationService service;
  private MockRestServiceServer server;

  /** Prepara somente transporte e persistência simulados, sem acesso à Meta. */
  @BeforeEach
  void setUp() {
    RestTemplate template = new RestTemplate();
    RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
    when(builder.build()).thenReturn(template);
    FacebookAccountRepository repository = mock(FacebookAccountRepository.class);
    when(repository.save(any(FacebookAccount.class))).thenAnswer(call -> call.getArgument(0));
    service =
        new FacebookTokenRevalidationService(
            builder, repository, new ObjectMapper(), "https://graph.facebook.com", "v23.0");
    server = MockRestServiceServer.bindTo(template).build();
  }

  /** Usa o prazo informado pela Meta em contas diferentes e preserva o caminho de sessenta dias. */
  @ParameterizedTest
  @CsvSource({"1,3600", "27,86400", "31,5184000"})
  void respectsProviderExpiration(Long accountId, long seconds) {
    server
        .expect(requestTo(url()))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"access_token\":\"new-token\",\"expires_in\":" + seconds + "}",
                MediaType.APPLICATION_JSON));
    FacebookAccount account = account(accountId);
    LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);
    var result = service.revalidate(account);
    LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);
    server.verify();
    assertThat(result.status()).isEqualTo(FacebookTokenRenewalStatus.SUCCESS);
    assertThat(result.tokenExpiresAt())
        .isBetween(before.plusSeconds(seconds), after.plusSeconds(seconds));
    assertThat(account.getTokenExpiresAt()).isEqualTo(result.tokenExpiresAt());
    assertThat(account.getTokenRenewedAt()).isBetween(before, after);
    assertThat(account.getAccessToken()).isEqualTo("new-token");
  }

  /** Impede declarar validade fictícia quando a resposta omite ou invalida a duração. */
  @ParameterizedTest
  @ValueSource(strings = {"", ",\"expires_in\":0", ",\"expires_in\":-1", ",\"expires_in\":null"})
  void rejectsUnknownExpiration(String expiration) {
    server
        .expect(requestTo(url()))
        .andRespond(
            withSuccess(
                "{\"access_token\":\"new-token\"" + expiration + "}", MediaType.APPLICATION_JSON));
    FacebookAccount account = account(27L);
    LocalDateTime previous = account.getTokenExpiresAt();
    var result = service.revalidate(account);
    server.verify();
    assertThat(result.status()).isEqualTo(FacebookTokenRenewalStatus.FAILED);
    assertThat(account.getAccessToken()).isEqualTo("synthetic-old-token");
    assertThat(account.getTokenExpiresAt()).isEqualTo(previous);
    assertThat(account.getTokenRenewedAt()).isNull();
  }

  /** Mantém a automação ligada e preserva a causa da recusa sem expor segredo ou token em logs. */
  @Test
  void preservesExpiredCredentialAndSafeFailure(CapturedOutput output) {
    server
        .expect(requestTo(url()))
        .andRespond(
            withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    "{\"error\":{\"message\":\"Session has expired\",\"type\":\"OAuthException\",\"code\":190,\"error_subcode\":463}}"));
    FacebookAccount account = account(1L);
    var result = service.revalidate(account);
    assertThat(result.status()).isEqualTo(FacebookTokenRenewalStatus.FAILED);
    assertThat(result.errorMessage()).contains("190", "463", "Session has expired");
    assertThat(account.getAccessToken()).isEqualTo("synthetic-old-token");
    assertThat(account.isTokenRenewalEnabled()).isTrue();
    assertThat(output.getAll()).doesNotContain("synthetic-old-token", "synthetic-app-secret");
    server.verify();
  }

  /** Monta uma conta fictícia com prazo anterior preservável. */
  private FacebookAccount account(Long id) {
    return FacebookAccount.builder()
        .id(id)
        .name("Conta de teste " + id)
        .accessToken("synthetic-old-token")
        .appId("test-app")
        .appSecret("synthetic-app-secret")
        .tokenExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(2))
        .tokenRenewalEnabled(true)
        .build();
  }

  /** Retorna o endpoint contratual com credenciais exclusivamente sintéticas. */
  private String url() {
    return "https://graph.facebook.com/v23.0/oauth/access_token?grant_type=fb_exchange_token"
        + "&client_id=test-app&client_secret=synthetic-app-secret&fb_exchange_token=synthetic-old-token";
  }
}
