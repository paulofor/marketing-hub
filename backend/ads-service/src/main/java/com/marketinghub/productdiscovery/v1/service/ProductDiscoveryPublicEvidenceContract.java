package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunityDecision;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Confere suporte público por candidata sem aceitar classificação do modelo como venda real. */
final class ProductDiscoveryPublicEvidenceContract {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ProductDiscoveryPublicEvidenceContract.class);
  private static final Set<String> ROLES =
      Set.of("PUBLIC_CUSTOMER_REPORT", "SELLER_CLAIM", "EDITORIAL", "SCIENTIFIC", "OTHER");
  private static final Set<String> ACTIONS =
      Set.of(
          "PURCHASE_REPORTED",
          "ABANDONMENT_REPORTED",
          "USE_REPORTED",
          "FRUSTRATION_REPORTED",
          "UNKNOWN");

  /** Impede instanciação de um validador puro de contrato. */
  private ProductDiscoveryPublicEvidenceContract() {}

  /** Valida trechos, hashes, datas e independência antes de aceitar uma candidata promovida. */
  static void validate(Long cycleId, ProductDiscoveryOpportunityResultRequest candidate) {
    try {
      JsonNode root = JSON.readTree(candidate.evidenceJson());
      JsonNode assessment = root.path("publicEvidenceAssessment");
      if (!"PUBLIC_SOURCES_V1".equals(assessment.path("policy").asText())
          || !assessment.path("observations").isArray()) {
        throw new IllegalArgumentException("Avaliação pública estruturada ausente");
      }
      Set<String> domains = new HashSet<>();
      Set<String> seen = new HashSet<>();
      Set<String> ids = new HashSet<>();
      root.path("candidateEvidence").path("evidenceIds").forEach(id -> ids.add(id.asText()));
      JsonNode sources = root.path("referencedEvidence").path("publicEvidence");
      for (JsonNode observation : assessment.path("observations")) {
        String id = observation.path("evidenceId").asText();
        JsonNode source = null;
        for (JsonNode item : sources) {
          if (id.equals(item.path("evidenceId").asText())) source = item;
        }
        String excerpt = observation.path("supportingExcerpt").asText().trim();
        String role = observation.path("sourceRole").asText();
        String action = observation.path("reportedAction").asText();
        if (source == null
            || !ids.contains(id)
            || !seen.add(id)
            || !ROLES.contains(role)
            || !ACTIONS.contains(action)
            || excerpt.length() < 15
            || excerpt.length() > 500
            || !source.path("snippet").asText().contains(excerpt)
            || observation.path("limitation").asText().isBlank()
            || !"SEARCH_EXCERPT_ONLY".equals(observation.path("evidenceScope").asText())
            || !"MODEL_CLASSIFIED".equals(observation.path("classification").asText())
            || !source.path("url").asText().equals(observation.path("url").asText())
            || !source
                .path("retrievedAt")
                .asText()
                .equals(observation.path("retrievedAt").asText())) {
          throw new IllegalArgumentException("Observação sem suporte exato no corpus da candidata");
        }
        URI url = URI.create(source.path("url").asText());
        if (!Set.of("http", "https").contains(url.getScheme()) || url.getHost() == null) {
          throw new IllegalArgumentException("URL da fonte inválida");
        }
        Instant.parse(source.path("retrievedAt").asText());
        String hash =
            HexFormat.of()
                .formatHex(
                    MessageDigest.getInstance("SHA-256")
                        .digest(source.path("snippet").asText().getBytes(StandardCharsets.UTF_8)));
        if (!hash.equals(observation.path("sourceSha256").asText())) {
          throw new IllegalArgumentException("Hash do trecho coletado diverge da observação");
        }
        String[] parts = url.getHost().toLowerCase(Locale.ROOT).split("\\.");
        if ("PUBLIC_CUSTOMER_REPORT".equals(role)
            && !"UNKNOWN".equals(action)
            && parts.length >= 2) {
          int labels = url.getHost().toLowerCase(Locale.ROOT).matches(".*\\.(com|net|org|co|gov|ac|edu)\\.[a-z]{2}$") ? 3 : 2;
          if (parts.length >= labels) domains.add(String.join(".", java.util.Arrays.copyOfRange(parts, parts.length - labels, parts.length)));
        }
      }
      boolean ready = domains.size() >= 2;
      if (ready != assessment.path("ready").asBoolean()
          || (candidate.decision() == ProductDiscoveryOpportunityDecision.APPROVE && !ready)) {
        throw new IllegalArgumentException(
            "Promoção sem dois domínios comportamentais independentes");
      }
    } catch (Exception ex) {
      LOGGER.error(
          "[product-discovery] Evidência pública inválida cycleId={} candidate={}",
          cycleId,
          candidate.name(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "A candidata não comprova o contrato de evidências públicas; preserve lacunas sem aprovar",
          ex);
    }
  }
}
