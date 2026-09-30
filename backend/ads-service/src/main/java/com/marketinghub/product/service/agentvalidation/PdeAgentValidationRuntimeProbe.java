package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: comprovar a identidade e as travas do runtime privado publicado do PDE. */
@Component
@Slf4j
public class PdeAgentValidationRuntimeProbe {
  private static final List<String> CANONICAL_SIGNALS =
      List.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");
  private static final Set<String> REQUIRED_ERROR_STATES =
      Set.of(
          "ACCESS_INVALID",
          "SESSION_EXPIRED",
          "INPUT_INCOMPLETE",
          "HARNESS_FAILURE",
          "RESULT_UNAVAILABLE",
          "RESUME_FAILED");
  private final JdkClientHttpRequestFactory requestFactory;

  /** Configura timeouts curtos para que o callback nunca espere indefinidamente pelo runtime. */
  public PdeAgentValidationRuntimeProbe() {
    var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    requestFactory = new JdkClientHttpRequestFactory(http);
    requestFactory.setReadTimeout(Duration.ofSeconds(15));
  }

  /** Lê diagnóstico e contrato no mesmo acesso privado e devolve apenas uma identidade coerente. */
  public RuntimeIdentity probe(String privateAccessUrl, Long productId, String productSlug) {
    String baseUrl = validatedBaseUrl(privateAccessUrl);
    try {
      RestClient client = RestClient.builder().requestFactory(requestFactory).build();
      JsonNode diagnostic =
          client
              .get()
              .uri(URI.create(baseUrl + "/version-diagnostics.json"))
              .retrieve()
              .body(JsonNode.class);
      JsonNode contract =
          client
              .get()
              .uri(
                  URI.create(
                      baseUrl
                          + "/api/pde/agent-validation/v1/products/"
                          + validatedSlug(productSlug)
                          + "/contract"))
              .retrieve()
              .body(JsonNode.class);
      return validatedIdentity(baseUrl, productId, productSlug, diagnostic, contract);
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao reconciliar runtime privado do PDE. productId={} productSlug={} baseUrl={}",
          productId,
          productSlug,
          baseUrl,
          ex);
      throw new IllegalStateException(
          "O runtime privado não pôde comprovar sua identidade publicada.", ex);
    }
  }

  /** Recusa destinos mutáveis, credenciais na URL e origens que não sejam HTTPS. */
  String validatedBaseUrl(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("A aceitação privada não possui URL de runtime.");
    }
    URI uri = URI.create(value.trim());
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null) {
      throw new IllegalArgumentException("A URL do runtime privado precisa ser HTTPS e opaca.");
    }
    return value.trim().replaceAll("/+$", "");
  }

  /** Impede que o slug persistido altere o caminho fixo da consulta de contrato. */
  private String validatedSlug(String value) {
    if (value == null || !value.matches("[a-z0-9][a-z0-9-]{1,190}")) {
      throw new IllegalArgumentException("O produto não possui slug seguro para o runtime PDE.");
    }
    return value;
  }

  /**
   * Exige versão única, cinco sinais, seis falhas e efeitos comerciais integralmente desligados.
   */
  RuntimeIdentity validatedIdentity(
      String baseUrl, Long productId, String productSlug, JsonNode diagnostic, JsonNode contract) {
    if (diagnostic == null || contract == null) {
      throw new IllegalArgumentException(
          "O runtime não retornou diagnóstico e contrato completos.");
    }
    String version = text(diagnostic, "experienceVersion");
    String commitSha = text(diagnostic, "commitSha");
    String sourceSha = text(diagnostic, "frontendSourceSha256");
    String image = text(diagnostic, "image");
    String imageTag = text(diagnostic, "imageTag");
    if (!"UP".equals(text(diagnostic, "status"))
        || !productId.equals(diagnostic.path("productId").longValue())
        || !productSlug.equals(text(diagnostic, "productSlug"))
        || !baseUrl.equals(trimTrailingSlash(text(diagnostic, "publicUrl")))
        || !version.equals(text(diagnostic, "version"))
        || !version.equals(text(diagnostic, "imageVersionId"))
        || !version.matches("[a-z0-9][a-z0-9._-]{2,63}")
        || !commitSha.matches("[0-9a-f]{40}")
        || !sourceSha.matches("[0-9a-f]{64}")
        || !imageTag.equals(commitSha)
        || image.isBlank()
        || !image.endsWith(":" + imageTag)) {
      throw new IllegalArgumentException("A identidade publicada do runtime PDE é divergente.");
    }
    JsonNode published = contract.path("published");
    JsonNode paymentEnabled = contract.path("paymentEnabled");
    JsonNode mediaSpend = contract.path("mediaSpendBrl");
    JsonNode providerCalls = contract.path("providerCallsAuthorized");
    if (!productId.equals(contract.path("productId").longValue())
        || !productSlug.equals(text(contract, "productSlug"))
        || !version.equals(text(contract, "prototypeVersion"))
        || !"SIMULATED_NO_CHARGE".equals(text(contract, "checkoutMode"))
        || text(contract, "intakeConsentVersion").isBlank()
        || !published.isBoolean()
        || published.booleanValue()
        || !paymentEnabled.isBoolean()
        || paymentEnabled.booleanValue()
        || !mediaSpend.isNumber()
        || mediaSpend.decimalValue().signum() != 0
        || !providerCalls.isIntegralNumber()
        || providerCalls.intValue() != 0
        || !CANONICAL_SIGNALS.equals(textValues(contract.path("instrumentationEvents")))
        || contract.path("errorStates").size() != REQUIRED_ERROR_STATES.size()
        || !REQUIRED_ERROR_STATES.equals(errorCodes(contract.path("errorStates")))) {
      throw new IllegalArgumentException("O contrato publicado do runtime PDE não está travado.");
    }
    Instant deployedAt;
    try {
      deployedAt = Instant.parse(text(diagnostic, "deployedAt"));
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(
          "O runtime não informou uma data de publicação válida.", ex);
    }
    return new RuntimeIdentity(
        version,
        commitSha,
        sourceSha,
        image,
        imageTag,
        deployedAt,
        text(contract, "intakeConsentVersion"),
        diagnostic.deepCopy());
  }

  /** Lê uma propriedade textual sem aceitar ausência como prova válida. */
  private String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isTextual() ? value.asText().trim() : "";
  }

  /** Remove somente barras finais ao comparar a URL aceita com a URL autodeclarada. */
  private String trimTrailingSlash(String value) {
    return value == null ? "" : value.replaceAll("/+$", "");
  }

  /** Preserva ordem e cardinalidade dos sinais para impedir aliases ou eventos extras. */
  private List<String> textValues(JsonNode values) {
    if (!values.isArray()) return List.of();
    List<String> result = new ArrayList<>();
    values.forEach(value -> result.add(value.isTextual() ? value.asText() : ""));
    return List.copyOf(result);
  }

  /** Extrai os códigos de recuperação sem confiar nas mensagens apresentadas pelo frontend. */
  private Set<String> errorCodes(JsonNode values) {
    if (!values.isArray()) return Set.of();
    java.util.HashSet<String> result = new java.util.HashSet<>();
    values.forEach(value -> result.add(text(value, "code")));
    return Set.copyOf(result);
  }

  /** Representa a identidade mínima comprovada pelos dois endpoints do mesmo runtime. */
  public record RuntimeIdentity(
      String prototypeVersion,
      String commitSha,
      String frontendSourceSha256,
      String image,
      String imageTag,
      Instant deployedAt,
      String intakeConsentVersion,
      JsonNode diagnosticSnapshot) {}
}
