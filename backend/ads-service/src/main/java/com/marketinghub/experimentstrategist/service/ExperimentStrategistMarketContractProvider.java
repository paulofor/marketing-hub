package com.marketinghub.experimentstrategist.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.MarketStrategicContextProvider;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: publicar o último contrato estratégico imutável produzido por Atena. */
@Service
public class ExperimentStrategistMarketContractProvider implements MarketStrategicContextProvider {
  private static final Logger log =
      LoggerFactory.getLogger(ExperimentStrategistMarketContractProvider.class);
  private static final String AUTHORITY_MODE = "READ_ONLY_RESEARCH";
  private static final Pattern PLAN_REFERENCE =
      Pattern.compile("commercial-plan:([1-9][0-9]*)(?:@v[1-9][0-9]*)?");
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("experiment:([1-9][0-9]*)");
  private final ExperimentStrategistExecutionRepository executions;
  private final CommercialPlanRepository plans;
  private final ObjectMapper objectMapper;

  /** Configura as fontes persistidas usadas para resolver estratégia sem recomputação. */
  public ExperimentStrategistMarketContractProvider(
      ExperimentStrategistExecutionRepository executions,
      CommercialPlanRepository plans,
      ObjectMapper objectMapper) {
    this.executions = executions;
    this.plans = plans;
    this.objectMapper = objectMapper;
  }

  /** Resolve plano ou experimento sem misturar produtos e devolve disponibilidade auditável. */
  @Override
  @Transactional(readOnly = true)
  public Optional<Map<String, Object>> resolve(String sourceReference) {
    Long planId = planId(sourceReference);
    if (planId == null) return Optional.empty();
    return Optional.of(resolveForPlan(planId));
  }

  /** Entrega o contrato vigente do plano ou uma lacuna funcional explícita. */
  @Transactional(readOnly = true)
  public Map<String, Object> resolveForPlan(Long planId) {
    Optional<ExperimentStrategistExecution> execution =
        executions.findFirstByCommercialPlanIdAndStatusAndAuthorityModeOrderByFinishedAtDescIdDesc(
            planId, ExperimentStrategistExecutionStatus.COMPLETED, AUTHORITY_MODE);
    if (execution.isEmpty()) {
      return unavailable(
          planId, "Atena ainda não concluiu uma estratégia de mercado para o plano.");
    }
    return contract(execution.get());
  }

  /** Extrai somente o artefato formal e recusa parecer histórico sem o contrato v2. */
  private Map<String, Object> contract(ExperimentStrategistExecution execution) {
    try {
      JsonNode recommendation = objectMapper.readTree(execution.getRecommendationJson());
      JsonNode contract = recommendation.path("marketStrategicContract");
      if (!contract.isObject() || contract.path("contractVersion").asText().isBlank()) {
        return unavailable(
            execution.getCommercialPlan().getId(),
            "A última pesquisa de Atena é histórica e não contém Contrato Estratégico de Mercado v2.");
      }
      String canonicalJson = objectMapper.writeValueAsString(contract);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("availability", "AVAILABLE");
      result.put("sourceAgent", "ATENA");
      result.put("strategistExecutionId", execution.getId());
      result.put("contractVersion", contract.path("contractVersion").asText());
      result.put("contentHash", sha256(canonicalJson));
      if (execution.getFinishedAt() != null) result.put("finishedAt", execution.getFinishedAt());
      result.put("contract", objectMapper.convertValue(contract, Map.class));
      return Map.copyOf(result);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao ler contrato estratégico de Atena. executionId={} planId={}",
          execution.getId(),
          execution.getCommercialPlan().getId(),
          ex);
      return unavailable(
          execution.getCommercialPlan().getId(),
          "O parecer mais recente de Atena não contém JSON estratégico válido.");
    }
  }

  /** Identifica o plano explicitamente ou pelo experimento pertencente ao portfólio. */
  private Long planId(String sourceReference) {
    if (sourceReference == null) return null;
    Matcher planMatcher = PLAN_REFERENCE.matcher(sourceReference.trim());
    if (planMatcher.matches()) return Long.valueOf(planMatcher.group(1));
    Matcher experimentMatcher = EXPERIMENT_REFERENCE.matcher(sourceReference.trim());
    if (!experimentMatcher.matches()) return null;
    Long experimentId = Long.valueOf(experimentMatcher.group(1));
    return plans.findByExperimentReference(experimentId).stream()
        .findFirst()
        .map(com.marketinghub.planning.CommercialPlan::getId)
        .orElse(null);
  }

  /** Expõe uma lacuna acionável sem fabricar estratégia a partir de campos operacionais. */
  private Map<String, Object> unavailable(Long planId, String reason) {
    return Map.of(
        "availability",
        "MISSING",
        "sourceAgent",
        "ATENA",
        "commercialPlanId",
        planId,
        "reason",
        reason,
        "requiredAction",
        "Solicitar nova análise estratégica à Atena antes de operar crescimento.");
  }

  /** Calcula a identidade imutável do conteúdo entregue aos agentes consumidores. */
  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error("Falha ao calcular hash do contrato estratégico de Atena.", ex);
      throw new IllegalStateException("SHA-256 indisponível para o contrato estratégico.", ex);
    }
  }
}
