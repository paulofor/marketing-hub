package com.marketinghub.facebookads.service.token;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.ads.FacebookAccount;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/** Confere a credencial candidata na Meta sem alterar conta, campanhas ou orçamento. */
@Component
public class FacebookTokenVerificationClient {
  private static final Logger log = LoggerFactory.getLogger(FacebookTokenVerificationClient.class);
  private final RestTemplate http;
  private final String baseUrl;
  private final String version;

  /** Configura consultas limitadas em tempo e permite um servidor local nas homologações. */
  public FacebookTokenVerificationClient(
      RestTemplateBuilder builder,
      @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String baseUrl,
      @Value("${facebook.graph-api.version:v23.0}") String version) {
    this.http =
        builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    this.baseUrl = baseUrl;
    this.version = version;
  }

  /** Exige validade, aplicativo, titular, permissões e acesso à conta de anúncios cadastrada. */
  public LocalDateTime verify(FacebookAccount account, String candidate) {
    URI debugUrl = url("debug_token").queryParam("input_token", candidate).build().encode().toUri();
    JsonNode data =
        read(
                account.getId(),
                "debug_token",
                debugUrl,
                account.getAppId() + "|" + account.getAppSecret())
            .path("data");
    require(
        data.path("is_valid").isBoolean() && data.path("is_valid").booleanValue(),
        "A Meta recusou o token. Gere um novo token de usuário e tente novamente.");
    require(
        account.getAppId().equals(data.path("app_id").asText()),
        "O token pertence a outro aplicativo. Use o App ID exibido nesta conta.");
    require(
        "USER".equals(data.path("type").asText()),
        "Informe um token de usuário do aplicativo, não um token de página ou de aplicativo.");
    if (account.getAuthorizedUserId() != null && !account.getAuthorizedUserId().isBlank()) {
      require(
          account.getAuthorizedUserId().equals(data.path("user_id").asText()),
          "O token pertence a outro usuário. Autentique o titular cadastrado nesta conta.");
    }
    long now = java.time.Instant.now().getEpochSecond();
    require(
        data.path("expires_at").canConvertToLong() && data.path("expires_at").asLong() > now,
        "O token expirou ou não tem validade verificável. Gere um novo token de usuário.");
    require(
        !data.has("data_access_expires_at")
            || (data.path("data_access_expires_at").canConvertToLong()
                && (data.path("data_access_expires_at").asLong() == 0
                    || data.path("data_access_expires_at").asLong() > now)),
        "O acesso aos dados expirou. Autorize novamente o aplicativo na Meta.");
    Set<String> scopes = new HashSet<>();
    data.path("scopes").forEach(scope -> scopes.add(scope.asText()));
    require(
        scopes.containsAll(Set.of("ads_read", "ads_management")),
        "Conceda as permissões ads_read e ads_management ao gerar o token.");
    String adId = account.getAdAccountId().replaceFirst("^act_", "");
    JsonNode ad =
        read(
            account.getId(),
            "ad_account",
            url("act_" + adId).queryParam("fields", "id,account_id").build().toUri(),
            candidate);
    require(
        adId.equals(ad.path("account_id").asText())
            && ("act_" + adId).equals(ad.path("id").asText()),
        "O token não comprovou acesso à conta de anúncios cadastrada.");
    return LocalDateTime.ofEpochSecond(data.path("expires_at").asLong(), 0, ZoneOffset.UTC);
  }

  /** Consulta a Meta e transforma erros externos em mensagens que nunca contêm credenciais. */
  private JsonNode read(Long accountId, String operation, URI uri, String bearer) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(bearer);
    try {
      JsonNode body =
          http.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
      require(
          body != null && body.isObject() && !body.has("error"),
          "A Meta não confirmou o acesso. A credencial anterior foi preservada.");
      return body;
    } catch (RestClientException ex) {
      // A exceção HTTP original pode conter URL, corpo e cabeçalhos com segredos.
      RuntimeException safe =
          new IllegalStateException("Falha Meta: " + ex.getClass().getSimpleName());
      safe.setStackTrace(ex.getStackTrace());
      log.warn(
          "Meta: validação de token falhou; accountId={}, operação={}", accountId, operation, safe);
      boolean rejected =
          ex instanceof RestClientResponseException response
              && response.getStatusCode().is4xxClientError()
              && response.getStatusCode().value() != 429;
      throw new ResponseStatusException(
          rejected ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.SERVICE_UNAVAILABLE,
          rejected
              ? "A Meta recusou o token ou o acesso à conta. Confira aplicativo, titular e permissões."
              : "Não foi possível consultar a Meta agora. A credencial anterior foi preservada; tente novamente.");
    }
  }

  /** Constrói somente caminhos oficiais da Graph API configurada. */
  private UriComponentsBuilder url(String path) {
    return UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(version, path);
  }

  /** Rejeita evidência incompleta sem aceitar sucesso apenas pelo HTTP 200. */
  private void require(boolean condition, String message) {
    if (!condition) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
  }
}
