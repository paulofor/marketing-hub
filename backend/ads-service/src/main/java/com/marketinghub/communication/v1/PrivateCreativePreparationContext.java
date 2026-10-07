package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: declarar as provas internas e a fronteira de vídeo da preparação privada. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrivateCreativePreparationContext {
  public static final String VERSION = "PDE_PRIVATE_CREATIVE_PREPARATION_V1";
  public static final String FIELD = "privateCreativePreparation";
  private static final Set<String> MODES =
      Set.of("LEARNING_CYCLE_PRIVATE", "PRODUCT_PRIVATE", "INITIAL_EXPERIMENT_PRIVATE");
  private final VideoProductionCycleRepository videos;
  private final VideoProjectRepository projects;
  private final ObjectMapper json;

  /** Reconhece somente os contextos privados documentados do contrato de comunicação. */
  static boolean isPrivateMode(String mode) {
    return MODES.contains(mode);
  }

  /**
   * Acrescenta requisitos de avaliação sem mudar a estratégia, os formatos comerciais ou verbas.
   */
  public Map<String, Object> enrich(String reference, Map<String, Object> input) {
    JsonNode context = json.valueToTree(input);
    if (!MODES.contains(context.path("mode").asText())
        || !"AVAILABLE".equals(context.path("availability").asText())
        || !"READY".equals(context.path("inputReadiness").asText())
        || reference == null
        || !reference.equals(context.path("sourceReference").asText())
        || context.path("prototypeVersion").asText().isBlank()
        || context.path("publicationAuthorized").asBoolean(true)
        || context.path("paymentEnabled").asBoolean(true)
        || context.path("externalMediaSpendAuthorized").asBoolean(true)) return input;
    var result = new LinkedHashMap<>(input);
    try {
      Long videoRequestId = currentVideoRequest(context);
      var contract = new LinkedHashMap<String, Object>();
      contract.put("contractVersion", VERSION);
      contract.put("scope", "PRIVATE_PREPARATION");
      contract.put("sourceReference", reference);
      contract.put("prototypeVersion", context.path("prototypeVersion").asText());
      contract.put("nonAudiovisualEvidenceRequired", true);
      contract.put("nonAudiovisualEvidencePurpose", "INDEPENDENT_REVIEW");
      contract.put("nonAudiovisualTemplate", "PROOF_CARD_V1");
      contract.put("commercialFormatDecisionPreserved", true);
      contract.put(
          "audiovisualProductionIntent",
          videoRequestId == null ? "BRIEF_ONLY" : "GOVERNED_PRODUCTION_REQUESTED");
      contract.put("videoProductionRequestId", videoRequestId == null ? 0L : videoRequestId);
      contract.put("publicationAuthorized", false);
      contract.put("spendAuthorized", false);
      contract.put("commercialEvidenceClaimed", false);
      result.put(FIELD, java.util.Collections.unmodifiableMap(contract));
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao declarar preparação criativa privada. sourceReference={} productId={} prototypeVersion={}",
          reference,
          context.path("product").path("id").asLong(),
          context.path("prototypeVersion").asText(),
          ex);
      result.put("availability", "UNAVAILABLE");
      result.put("inputReadiness", "BLOCKED");
      result.put("reason", "Não foi possível conferir o pedido governado de vídeo desta versão.");
    }
    return java.util.Collections.unmodifiableMap(result);
  }

  /** Reconhece somente pedido financeiro explícito da mesma identidade, sem aprovar seu gasto. */
  private Long currentVideoRequest(JsonNode context) {
    long productId = context.path("product").path("id").asLong();
    long numericExperimentId = context.path("experiment").path("id").asLong();
    Long experimentId = numericExperimentId > 0 ? numericExperimentId : null;
    if (productId <= 0) return null;
    var cycle =
        videos
            .findTopByProductIdAndExperimentIdOrderByCreatedAtDescIdDesc(productId, experimentId)
            .orElse(null);
    if (cycle == null
        || cycle.getId() == null
        || cycle.getVideoProjectId() == null
        || cycle.getRequestedBy() == null
        || cycle.getRequestedBy().isBlank()
        || cycle.getBudgetLimitUsd() == null
        || cycle.getBudgetLimitUsd().signum() <= 0
        || "CANCELLED".equals(cycle.getStatus())
        || "STOPPED".equals(cycle.getStatus())) return null;
    if (cycle.getStatus() == null || cycle.getStatus().isBlank())
      throw new IllegalStateException("Pedido de vídeo sem estado auditável.");
    var project = projects.findById(cycle.getVideoProjectId()).orElse(null);
    return project != null
            && java.util.Objects.equals(project.getProductId(), productId)
            && java.util.Objects.equals(project.getExperimentId(), experimentId)
            && context.path("prototypeVersion").asText().equals(project.getCampaignKey())
        ? cycle.getId()
        : null;
  }

  /** Reconhece a declaração privada da mesma referência sem conceder publicação ou gasto. */
  static boolean isPreparation(JsonNode contract, String reference) {
    return VERSION.equals(contract.path("contractVersion").asText())
        && "PRIVATE_PREPARATION".equals(contract.path("scope").asText())
        && reference != null
        && reference.equals(contract.path("sourceReference").asText())
        && contract.path("nonAudiovisualEvidenceRequired").asBoolean(false)
        && !contract.path("publicationAuthorized").asBoolean(true)
        && !contract.path("spendAuthorized").asBoolean(true)
        && !contract.path("commercialEvidenceClaimed").asBoolean(true);
  }

  /** Identifica briefing restrito; ausência mantém o contrato legado sem dispensar vídeo. */
  static boolean isBriefOnly(JsonNode contract, String reference) {
    return isPreparation(contract, reference)
        && "BRIEF_ONLY".equals(contract.path("audiovisualProductionIntent").asText());
  }
}
