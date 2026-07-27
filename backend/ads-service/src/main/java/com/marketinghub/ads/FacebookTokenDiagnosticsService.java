package com.marketinghub.ads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Executa diagnosticos seguros do token Meta sem expor credenciais para a interface administrativa.
 */
@Service
public class FacebookTokenDiagnosticsService {
  private static final Logger log = LoggerFactory.getLogger(FacebookTokenDiagnosticsService.class);
  private static final List<String> REQUIRED_PERMISSIONS = List.of("ads_management", "ads_read");
  private static final List<String> RECOMMENDED_PERMISSIONS =
      List.of("business_management", "pages_show_list", "pages_read_engagement");
  private static final byte[] TEST_VIDEO_BYTES =
      Base64.getDecoder()
          .decode(
              "AAAAIGZ0eXBpc29tAAACAGlzb21pc28yYXZjMW1wNDEAAAN1bW9vdgAAAGxtdmhkAAAAAAAAAAAAAAAAAAAD6AAAAMgAAQAAAQAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAp90cmFrAAAAXHRraGQAAAADAAAAAAAAAAAAAAABAAAAAAAAAMgAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAABAAAAAQAAAAAAAkZWR0cwAAABxlbHN0AAAAAAAAAAEAAADIAAAEAAABAAAAAAIXbWRpYQAAACBtZGhkAAAAAAAAAAAAAAAAAAAyAAAACgBVxAAAAAAALWhkbHIAAAAAAAAAAHZpZGUAAAAAAAAAAAAAAABWaWRlb0hhbmRsZXIAAAABwm1pbmYAAAAUdm1oZAAAAAEAAAAAAAAAAAAAACRkaW5mAAAAHGRyZWYAAAAAAAAAAQAAAAx1cmwgAAAAAQAAAYJzdGJsAAAAvnN0c2QAAAAAAAAAAQAAAK5hdmMxAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAABAAEABIAAAASAAAAAAAAAABFUxhdmM1OS4zNy4xMDAgbGli"
                  + "eDI2NAAAAAAAAAAAAAAAGP//AAAANGF2Y0MBZAAK/+EAF2dkAAqs2V7ARAAAAwAEAAADAMg8SJZYAQAGaOvjyyLA/fj4AAAAABBwYXNwAAAAAQAAAAEAAAAUYnRydAAAAAAAAHZIAAB2SAAAABhzdHRzAAAAAAAAAAEAAAAFAAACAAAAABRzdHNzAAAAAAAAAAEAAAABAAAAOGN0dHMAAAAAAAAABQAAAAEAAAQAAAAAAQAACgAAAAABAAAEAAAAAAEAAAAAAAAAAQAAAgAAAAAcc3RzYwAAAAAAAAABAAAAAQAAAAUAAAABAAAAKHN0c3oAAAAAAAAAAAAAAAUAAALFAAAADAAAAAwAAAAMAAAADAAAABRzdGNvAAAAAAAAAAEAAAOlAAAAYnVkdGEAAABabWV0YQAAAAAAAAAhaGRscgAAAAAAAAAAbWRpcmFwcGwAAAAAAAAAAAAAAAAtaWxzdAAAACWpdG9vAAAAHWRhdGEAAAABAAAAAExhdmY1OS4yNy4xMDAAAAAIZnJlZQAAAv1tZGF0AAACrgYF//+q3EXpvebZSLeWLNgg2SPu73gyNjQgLSBjb3JlIDE2NCByMzA5NSBiYWVlNDAwIC0gSC4yNjQvTVBFRy00IEFWQyBjb2RlYyAtIENvcHlsZWZ0IDIwMDMtMjAyMiAtIGh0dHA6Ly93d3cudmlkZW9sYW4ub3JnL3gyNjQuaHRtbCAtIG9wdGlvbnM6IGNhYmFjPTEgcmVmPTMgZGVibG9jaz0xOjA6MCBhbmFseXNlPTB4MzoweDExMyBtZT1oZXggc3VibWU9NyBwc3k9MSBwc3lfcmQ9MS4wMDowLjAwIG1peGVkX3JlZj0xIG1lX3JhbmdlPTE2IGNocm9tYV9tZT0xIHRyZWxsaXM9MSA4eDhkY3Q9MSBjcW09MCBkZWFkem9uZT0yMSwxMSBmYXN0X3Bza2lwPTEgY2hyb21hX3FwX29mZnNldD0tMiB0aHJlYWRzPTEgbG9va2FoZWFkX3RocmVhZHM9MSBzbGljZWRfdGhyZWFkcz0wIG5yPTAgZGVjaW1hdGU9MSBpbnRlcmxhY2VkPTAgYmx1cmF5X2NvbXBhdD0wIGNvbnN0cmFp"
                  + "bmVkX2ludHJhPTAgYmZyYW1lcz0zIGJfcHlyYW1pZD0yIGJfYWRhcHQ9MSBiX2JpYXM9MCBkaXJlY3Q9MSB3ZWlnaHRiPTEgb3Blbl9nb3A9MCB3ZWlnaHRwPTIga2V5aW50PTI1MCBrZXlpbnRfbWluPTI1IHNjZW5lY3V0PTQwIGludHJhX3JlZnJlc2g9MCByY19sb29rYWhlYWQ9NDAgcmM9Y3JmIG1idHJlZT0xIGNyZj0yMy4wIHFjb21wPTAuNjAgcXBtaW49MCBxcG1heD02OSBxcHN0ZXA9NCBpcF9yYXRpbz0xLjQwIGFxPTE6MS4wMACAAAAAD2WIhAAz//727L4FNhTIwQAAAAhBmiRsQr/+wAAAAAhBnkJ4hf/BgQAAAAgBnmF0Qr/EgAAAAAgBnmNqQr/EgQ==");

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final String graphApiBaseUrl;
  private final String graphApiVersion;

  public FacebookTokenDiagnosticsService(
      RestTemplateBuilder restTemplateBuilder,
      ObjectMapper objectMapper,
      @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String graphApiBaseUrl,
      @Value("${facebook.graph-api.version:v23.0}") String graphApiVersion) {
    this.restTemplate = restTemplateBuilder.build();
    this.objectMapper = objectMapper;
    this.graphApiBaseUrl = trimTrailingSlash(graphApiBaseUrl);
    this.graphApiVersion = trimSlashes(graphApiVersion);
  }

  /** Monta o retrato de validade, permissoes e acesso basico da conta Meta. */
  public FacebookTokenDiagnosticResponse diagnose(FacebookAccount account) {
    LocalDateTime checkedAt = LocalDateTime.now();
    String token = resolveOperationalToken(account);
    String tokenSource =
        StringUtils.hasText(account.getAccessToken()) ? "ACCESS_TOKEN" : "SYSTEM_USER_ACCESS_TOKEN";
    if (!StringUtils.hasText(token)) {
      return new FacebookTokenDiagnosticResponse(
          account.getId(),
          account.getName(),
          account.getAdAccountId(),
          "MISSING",
          checkedAt,
          false,
          GraphCallResult.skipped("debug_token", "Token nao configurado"),
          List.of(),
          GraphCallResult.skipped("ad_account", "Token nao configurado"),
          GraphCallResult.skipped("advideos_read", "Token nao configurado"),
          REQUIRED_PERMISSIONS,
          RECOMMENDED_PERMISSIONS);
    }

    return new FacebookTokenDiagnosticResponse(
        account.getId(),
        account.getName(),
        account.getAdAccountId(),
        tokenSource,
        checkedAt,
        true,
        debugToken(account, token),
        fetchPermissions(token),
        checkAdAccountAccess(account, token),
        checkVideoLibraryReadiness(account, token),
        REQUIRED_PERMISSIONS,
        RECOMMENDED_PERMISSIONS);
  }

  /**
   * Executa um upload real de video minimo na biblioteca Meta para validar a causa-raiz do envio.
   */
  public FacebookVideoUploadTestResponse testVideoUpload(FacebookAccount account) {
    LocalDateTime checkedAt = LocalDateTime.now();
    String token = resolveOperationalToken(account);
    if (!StringUtils.hasText(token)) {
      return new FacebookVideoUploadTestResponse(
          account.getId(),
          account.getName(),
          checkedAt,
          false,
          null,
          GraphCallResult.skipped("advideos_upload", "Token nao configurado"));
    }
    if (!StringUtils.hasText(account.getAdAccountId())) {
      return new FacebookVideoUploadTestResponse(
          account.getId(),
          account.getName(),
          checkedAt,
          false,
          null,
          GraphCallResult.skipped("advideos_upload", "Conta de anuncios nao configurada"));
    }

    String endpoint = "/act_" + normalizeAdAccountId(account.getAdAccountId()) + "/advideos";
    String url = graphUrl(endpoint).toUriString();
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("name", "marketinghub-token-diagnostic-" + account.getId());
    body.add("access_token", token);
    body.add(
        "source",
        new ByteArrayResource(TEST_VIDEO_BYTES) {
          @Override
          public String getFilename() {
            return "marketinghub-token-diagnostic.mp4";
          }
        });

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
      JsonNode json = parseJson(response.getBody());
      String videoId = firstText(json, "id", "video_id");
      return new FacebookVideoUploadTestResponse(
          account.getId(),
          account.getName(),
          checkedAt,
          true,
          videoId,
          GraphCallResult.success(
              "advideos_upload",
              response.getStatusCode().value(),
              "Upload de video aceito pela Meta"));
    } catch (RestClientResponseException ex) {
      log.error(
          "Falha no teste de upload de video Meta: accountId={}, endpoint={}, status={},"
              + " response={}",
          account.getId(),
          endpoint,
          ex.getRawStatusCode(),
          sanitizeBody(ex.getResponseBodyAsString()),
          ex);
      return new FacebookVideoUploadTestResponse(
          account.getId(),
          account.getName(),
          checkedAt,
          false,
          null,
          resultFromGraphError("advideos_upload", ex));
    } catch (RestClientException ex) {
      log.error(
          "Falha de integracao no teste de upload de video Meta: accountId={}, endpoint={}",
          account.getId(),
          endpoint,
          ex);
      return new FacebookVideoUploadTestResponse(
          account.getId(),
          account.getName(),
          checkedAt,
          false,
          null,
          GraphCallResult.failed("advideos_upload", null, ex.getMessage(), null, null, null));
    } catch (RuntimeException ex) {
      log.error(
          "Falha inesperada no teste de upload de video Meta: accountId={}, endpoint={}",
          account.getId(),
          endpoint,
          ex);
      return new FacebookVideoUploadTestResponse(
          account.getId(),
          account.getName(),
          checkedAt,
          false,
          null,
          GraphCallResult.failed("advideos_upload", null, ex.getMessage(), null, null, null));
    }
  }

  /** Consulta o endpoint oficial debug_token usando credenciais do aplicativo. */
  private GraphCallResult debugToken(FacebookAccount account, String token) {
    if (!StringUtils.hasText(account.getAppId()) || !StringUtils.hasText(account.getAppSecret())) {
      return GraphCallResult.skipped("debug_token", "App ID/App Secret nao configurados");
    }
    String appAccessToken = account.getAppId().trim() + "|" + account.getAppSecret().trim();
    String url =
        graphUrl("/debug_token")
            .queryParam("input_token", token)
            .queryParam("access_token", appAccessToken)
            .toUriString();
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      JsonNode data = parseJson(response.getBody()).path("data");
      Boolean valid = data.has("is_valid") ? data.path("is_valid").asBoolean() : null;
      String message =
          Boolean.TRUE.equals(valid) ? "Token valido na Meta" : "Token invalido na Meta";
      return GraphCallResult.success("debug_token", response.getStatusCode().value(), message)
          .withTokenDebug(
              new TokenDebugDetails(
                  valid,
                  text(data, "type"),
                  text(data, "app_id"),
                  text(data, "application"),
                  text(data, "user_id"),
                  epochSecondsToDateTime(data, "expires_at"),
                  epochSecondsToDateTime(data, "issued_at")));
    } catch (RestClientResponseException ex) {
      log.error(
          "Falha ao consultar debug_token da Meta: accountId={}, status={}, response={}",
          account.getId(),
          ex.getRawStatusCode(),
          sanitizeBody(ex.getResponseBodyAsString()),
          ex);
      return resultFromGraphError("debug_token", ex);
    } catch (RestClientException ex) {
      log.error(
          "Falha de integracao ao consultar debug_token da Meta: accountId={}",
          account.getId(),
          ex);
      return GraphCallResult.failed("debug_token", null, ex.getMessage(), null, null, null);
    }
  }

  /** Lista permissoes concedidas e marca as permissoes comerciais esperadas. */
  private List<PermissionDiagnostic> fetchPermissions(String token) {
    String url = graphUrl("/me/permissions").queryParam("access_token", token).toUriString();
    List<PermissionDiagnostic> permissions = new ArrayList<>();
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      JsonNode data = parseJson(response.getBody()).path("data");
      if (data.isArray()) {
        for (JsonNode node : data) {
          String permission = text(node, "permission");
          if (StringUtils.hasText(permission)) {
            permissions.add(
                new PermissionDiagnostic(
                    permission,
                    text(node, "status"),
                    REQUIRED_PERMISSIONS.contains(permission),
                    RECOMMENDED_PERMISSIONS.contains(permission)));
          }
        }
      }
      return permissions;
    } catch (RestClientResponseException ex) {
      log.error(
          "Falha ao listar permissoes Meta: status={}, response={}",
          ex.getRawStatusCode(),
          sanitizeBody(ex.getResponseBodyAsString()),
          ex);
      return List.of(
          new PermissionDiagnostic(
              "permissions_lookup", "FAILED: " + graphMessage(ex), false, false));
    } catch (RestClientException ex) {
      log.error("Falha de integracao ao listar permissoes Meta", ex);
      return List.of(
          new PermissionDiagnostic(
              "permissions_lookup", "FAILED: " + ex.getMessage(), false, false));
    }
  }

  /** Verifica se o token acessa a conta de anuncios configurada. */
  private GraphCallResult checkAdAccountAccess(FacebookAccount account, String token) {
    if (!StringUtils.hasText(account.getAdAccountId())) {
      return GraphCallResult.skipped("ad_account", "Conta de anuncios nao configurada");
    }
    String endpoint = "/act_" + normalizeAdAccountId(account.getAdAccountId());
    String url =
        graphUrl(endpoint)
            .queryParam("fields", "id,name,currency,account_status")
            .queryParam("access_token", token)
            .toUriString();
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      return GraphCallResult.success(
          "ad_account", response.getStatusCode().value(), "Conta de anuncios acessivel");
    } catch (RestClientResponseException ex) {
      log.error(
          "Falha ao validar conta de anuncios Meta: accountId={}, endpoint={}, status={},"
              + " response={}",
          account.getId(),
          endpoint,
          ex.getRawStatusCode(),
          sanitizeBody(ex.getResponseBodyAsString()),
          ex);
      return resultFromGraphError("ad_account", ex);
    } catch (RestClientException ex) {
      log.error(
          "Falha de integracao ao validar conta de anuncios Meta: accountId={}, endpoint={}",
          account.getId(),
          endpoint,
          ex);
      return GraphCallResult.failed("ad_account", null, ex.getMessage(), null, null, null);
    }
  }

  /** Verifica se a biblioteca de videos responde antes do teste destrutivo controlado. */
  private GraphCallResult checkVideoLibraryReadiness(FacebookAccount account, String token) {
    if (!StringUtils.hasText(account.getAdAccountId())) {
      return GraphCallResult.skipped("advideos_read", "Conta de anuncios nao configurada");
    }
    String endpoint = "/act_" + normalizeAdAccountId(account.getAdAccountId()) + "/advideos";
    String url =
        graphUrl(endpoint)
            .queryParam("fields", "id,title")
            .queryParam("limit", 1)
            .queryParam("access_token", token)
            .toUriString();
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      return GraphCallResult.success(
          "advideos_read", response.getStatusCode().value(), "Biblioteca de videos acessivel");
    } catch (RestClientResponseException ex) {
      log.error(
          "Falha ao validar biblioteca de videos Meta: accountId={}, endpoint={}, status={},"
              + " response={}",
          account.getId(),
          endpoint,
          ex.getRawStatusCode(),
          sanitizeBody(ex.getResponseBodyAsString()),
          ex);
      return resultFromGraphError("advideos_read", ex);
    } catch (RestClientException ex) {
      log.error(
          "Falha de integracao ao validar biblioteca de videos Meta: accountId={}, endpoint={}",
          account.getId(),
          endpoint,
          ex);
      return GraphCallResult.failed("advideos_read", null, ex.getMessage(), null, null, null);
    }
  }

  /** Resolve o token usado operacionalmente pelo worker de campanhas. */
  private String resolveOperationalToken(FacebookAccount account) {
    if (StringUtils.hasText(account.getAccessToken())) {
      return account.getAccessToken().trim();
    }
    return StringUtils.hasText(account.getSystemUserAccessToken())
        ? account.getSystemUserAccessToken().trim()
        : null;
  }

  /** Converte erro Graph em resposta segura para a tela. */
  private GraphCallResult resultFromGraphError(String operation, RestClientResponseException ex) {
    JsonNode error = parseJson(ex.getResponseBodyAsString()).path("error");
    return GraphCallResult.failed(
        operation,
        ex.getRawStatusCode(),
        graphMessage(ex),
        error.has("code") ? error.path("code").asInt() : null,
        error.has("error_subcode") ? error.path("error_subcode").asInt() : null,
        text(error, "fbtrace_id"));
  }

  /** Extrai a mensagem principal de erro retornada pela Meta. */
  private String graphMessage(RestClientResponseException ex) {
    String message = text(parseJson(ex.getResponseBodyAsString()).path("error"), "message");
    return StringUtils.hasText(message) ? message : "HTTP " + ex.getRawStatusCode();
  }

  /** Monta uma URL da Graph API com versao configuravel. */
  private UriComponentsBuilder graphUrl(String endpoint) {
    return UriComponentsBuilder.fromHttpUrl(graphApiBaseUrl)
        .pathSegment(graphApiVersion)
        .path(endpoint.startsWith("/") ? endpoint : "/" + endpoint);
  }

  /** Remove prefixo act_ quando o usuario ja informou a conta nesse formato. */
  private String normalizeAdAccountId(String adAccountId) {
    String trimmed = adAccountId.trim();
    return trimmed.startsWith("act_") ? trimmed.substring(4) : trimmed;
  }

  /** Le JSON de resposta sem propagar erro de parse para a tela. */
  private JsonNode parseJson(String body) {
    if (!StringUtils.hasText(body)) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(body);
    } catch (Exception ex) {
      log.debug("Nao foi possivel ler JSON da resposta Meta: {}", sanitizeBody(body), ex);
      return objectMapper.createObjectNode();
    }
  }

  /** Busca o primeiro campo textual disponivel no JSON. */
  private String firstText(JsonNode node, String... fields) {
    for (String field : fields) {
      String value = text(node, field);
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  /** Busca um campo textual simples no JSON. */
  private String text(JsonNode node, String field) {
    if (node == null || !node.has(field) || node.path(field).isNull()) {
      return null;
    }
    String value = node.path(field).asText();
    return StringUtils.hasText(value) ? value : null;
  }

  /** Converte epoch seconds da Meta para LocalDateTime UTC. */
  private LocalDateTime epochSecondsToDateTime(JsonNode node, String field) {
    if (node == null || !node.has(field) || !node.path(field).canConvertToLong()) {
      return null;
    }
    long value = node.path(field).asLong();
    if (value <= 0) {
      return null;
    }
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneOffset.UTC);
  }

  /** Mascara corpo muito grande antes de registrar log. */
  private String sanitizeBody(String body) {
    if (!StringUtils.hasText(body)) {
      return "<empty>";
    }
    String trimmed = body.trim();
    if (trimmed.length() > 2048) {
      return trimmed.substring(0, 2048) + "...";
    }
    return trimmed;
  }

  /** Remove barra final da URL base configurada. */
  private String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return "https://graph.facebook.com";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  /** Normaliza a versao configurada da Graph API. */
  private String trimSlashes(String value) {
    if (value == null || value.isBlank()) {
      return "v23.0";
    }
    String trimmed = value;
    if (trimmed.startsWith("/")) {
      trimmed = trimmed.substring(1);
    }
    if (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  public record FacebookTokenDiagnosticResponse(
      Long accountId,
      String accountName,
      String adAccountId,
      String tokenSource,
      LocalDateTime checkedAt,
      boolean hasToken,
      GraphCallResult tokenDebug,
      List<PermissionDiagnostic> permissions,
      GraphCallResult adAccountAccess,
      GraphCallResult videoLibraryReadiness,
      List<String> requiredPermissions,
      List<String> recommendedPermissions) {}

  public record FacebookVideoUploadTestResponse(
      Long accountId,
      String accountName,
      LocalDateTime checkedAt,
      boolean success,
      String videoId,
      GraphCallResult upload) {}

  public record PermissionDiagnostic(
      String permission, String status, boolean required, boolean recommended) {}

  public record TokenDebugDetails(
      Boolean valid,
      String type,
      String appId,
      String application,
      String userId,
      LocalDateTime expiresAt,
      LocalDateTime issuedAt) {}

  public record GraphCallResult(
      String operation,
      String status,
      Integer httpStatus,
      String message,
      Integer code,
      Integer subcode,
      String fbtraceId,
      TokenDebugDetails tokenDebug) {
    /** Cria resultado de sucesso para uma chamada Graph. */
    public static GraphCallResult success(String operation, Integer httpStatus, String message) {
      return new GraphCallResult(operation, "SUCCESS", httpStatus, message, null, null, null, null);
    }

    /** Cria resultado de falha para uma chamada Graph. */
    public static GraphCallResult failed(
        String operation,
        Integer httpStatus,
        String message,
        Integer code,
        Integer subcode,
        String fbtraceId) {
      return new GraphCallResult(
          operation, "FAILED", httpStatus, message, code, subcode, fbtraceId, null);
    }

    /** Cria resultado ignorado por pre-condicao incompleta. */
    public static GraphCallResult skipped(String operation, String message) {
      return new GraphCallResult(operation, "SKIPPED", null, message, null, null, null, null);
    }

    /** Anexa os metadados seguros do debug_token ao resultado. */
    public GraphCallResult withTokenDebug(TokenDebugDetails tokenDebug) {
      return new GraphCallResult(
          operation, status, httpStatus, message, code, subcode, fbtraceId, tokenDebug);
    }
  }
}
