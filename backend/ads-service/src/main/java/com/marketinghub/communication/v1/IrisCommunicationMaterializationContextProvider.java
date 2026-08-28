package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.CommunicationMaterializationContextProvider;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
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

/** Responsabilidade: consolidar a entrada funcional e segregada usada por Íris. */
@Service
public class IrisCommunicationMaterializationContextProvider
    implements CommunicationMaterializationContextProvider {
  private static final Logger log =
      LoggerFactory.getLogger(IrisCommunicationMaterializationContextProvider.class);
  private static final Pattern PLAN_REFERENCE =
      Pattern.compile("commercial-plan:([1-9][0-9]*)(?:@v([1-9][0-9]*))?(?::[A-Za-z0-9_-]+)*");
  private static final Pattern EXPERIMENT_REFERENCE = Pattern.compile("experiment:([1-9][0-9]*)");
  private final CommercialPlanRepository plans;
  private final CommercialPlanVersionService versions;
  private final CommercialPlanLandingAssetService landingAssets;
  private final AgentTaskRepository tasks;
  private final ObjectMapper objectMapper;

  /** Configura as fontes canônicas de plano, produto e provas aprovadas. */
  public IrisCommunicationMaterializationContextProvider(
      CommercialPlanRepository plans,
      CommercialPlanVersionService versions,
      CommercialPlanLandingAssetService landingAssets,
      AgentTaskRepository tasks,
      ObjectMapper objectMapper) {
    this.plans = plans;
    this.versions = versions;
    this.landingAssets = landingAssets;
    this.tasks = tasks;
    this.objectMapper = objectMapper;
  }

  /** Resolve plano e experimento sem consultar banco fora do backend ou misturar produtos. */
  @Override
  @Transactional(readOnly = true)
  public Optional<Map<String, Object>> resolve(String sourceReference) {
    Optional<ResolvedScope> scope = scope(sourceReference);
    if (scope.isEmpty()) return Optional.empty();
    return Optional.of(context(sourceReference, scope.get()));
  }

  /** Expõe o experimento exato para aplicar uma landing materializada no mesmo escopo. */
  @Transactional(readOnly = true)
  public Optional<Long> experimentId(String sourceReference) {
    return scope(sourceReference).map(ResolvedScope::experiment).map(Experiment::getId);
  }

  /** Monta contrato imutável com versão, hash, PDE e provas reais. */
  private Map<String, Object> context(String sourceReference, ResolvedScope scope) {
    try {
      CommercialPlanVersionDto version = versions.current(scope.plan().getId());
      if (scope.requestedPlanVersion() != null
          && !scope.requestedPlanVersion().equals(version.versionNumber())) {
        return unavailable(
            sourceReference,
            "A referência solicita uma versão diferente do plano comercial vigente.");
      }
      Experiment experiment = scope.experiment();
      Product product = experiment == null ? null : experiment.getProduct();
      if (experiment == null || product == null) {
        return unavailable(
            sourceReference, "Plano sem experimento e PDE vinculados para comunicação.");
      }
      java.util.List<Map<String, Object>> upstreamArtifacts =
          upstreamArtifacts(scope.plan().getId(), version.versionNumber(), experiment.getId());
      java.util.Set<String> upstreamAgentKeys =
          upstreamArtifacts.stream()
              .map(artifact -> String.valueOf(artifact.get("agentKey")))
              .collect(java.util.stream.Collectors.toUnmodifiableSet());
      java.util.List<String> missingPredecessors = new java.util.ArrayList<>();
      if (!upstreamAgentKeys.contains("financial-agent")) {
        missingPredecessors.add("Parecer econômico concluído de Plutus");
      }
      if (!upstreamAgentKeys.contains("landing-generator")) {
        missingPredecessors.add("PDE e prova funcional concluídos de Dédalo");
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("availability", "AVAILABLE");
      result.put("contractVersion", "IRIS_INPUT_V1");
      result.put("sourceReference", sourceReference);
      result.put("commercialPlanId", scope.plan().getId());
      result.put("commercialPlanVersion", version.versionNumber());
      result.put("commercialPlanSnapshotHash", sha256(version.snapshotJson()));
      result.put("commercialPlanSnapshot", objectMapper.readTree(version.snapshotJson()));
      result.put("experiment", experimentContract(experiment));
      result.put("product", productContract(product));
      result.put("approvedLandingAssets", landingAssets.payloadForExperiment(experiment.getId()));
      result.put("approvedUpstreamArtifacts", upstreamArtifacts);
      result.put("inputReadiness", missingPredecessors.isEmpty() ? "READY" : "BLOCKED");
      result.put("missingRequiredPredecessors", java.util.List.copyOf(missingPredecessors));
      result.put(
          "publicIdentityPolicy",
          "Marca, CNPJ, suporte e políticas; sem razão social completa ou endereço.");
      result.put("publicationAuthorized", false);
      result.put("externalMediaSpendAuthorized", false);
      return java.util.Collections.unmodifiableMap(result);
    } catch (Exception ex) {
      log.error(
          "Falha ao consolidar entrada de Íris. sourceReference={} commercialPlanId={} experimentId={}",
          sourceReference,
          scope.plan().getId(),
          scope.experiment() == null ? null : scope.experiment().getId(),
          ex);
      return unavailable(sourceReference, "A entrada de comunicação não pôde ser consolidada.");
    }
  }

  /** Consolida somente artefatos concluídos dos agentes predecessores no mesmo plano e versão. */
  private java.util.List<Map<String, Object>> upstreamArtifacts(
      Long planId, Integer planVersion, Long experimentId) {
    Map<Long, com.marketinghub.agenttask.AgentTask> candidates = new LinkedHashMap<>();
    tasks
        .findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:" + planId + "@v" + planVersion)
        .forEach(task -> candidates.put(task.getId(), task));
    tasks
        .findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:" + experimentId)
        .forEach(task -> candidates.put(task.getId(), task));
    Map<String, com.marketinghub.agenttask.AgentTask> latest = new LinkedHashMap<>();
    candidates.values().stream()
        .filter(task -> "COMPLETED".equals(task.getStatus()))
        .filter(task -> task.getProcessDefinition() != null)
        .filter(
            task ->
                java.util.Set.of(
                        "experiment-strategist",
                        "financial-agent",
                        "landing-generator",
                        "communication-director")
                    .contains(task.getAssignedAgent().getAgentKey()))
        .forEach(
            task ->
                latest.merge(
                    task.getProcessDefinition().getProcessCode()
                        + ":"
                        + task.getProcessActivityId(),
                    task,
                    (current, replacement) ->
                        replacement.getId() > current.getId() ? replacement : current));
    return latest.values().stream()
        .map(
            task -> {
              try {
                Map<String, Object> artifact = new LinkedHashMap<>();
                artifact.put("taskId", task.getId());
                artifact.put("agentKey", task.getAssignedAgent().getAgentKey());
                artifact.put("processCode", task.getProcessDefinition().getProcessCode());
                artifact.put("activityId", task.getProcessActivityId());
                artifact.put(
                    "result",
                    task.getResultJson() == null
                        ? null
                        : objectMapper.readTree(task.getResultJson()));
                artifact.put(
                    "evidence",
                    task.getEvidenceJson() == null
                        ? null
                        : objectMapper.readTree(task.getEvidenceJson()));
                return java.util.Collections.unmodifiableMap(artifact);
              } catch (Exception ex) {
                log.error(
                    "Artefato predecessor de Íris contém JSON inválido. taskId={} sourceReference={}",
                    task.getId(),
                    "commercial-plan:" + planId + "@v" + planVersion,
                    ex);
                throw new IllegalArgumentException(
                    "Artefato predecessor da comunicação contém JSON inválido.", ex);
              }
            })
        .toList();
  }

  /** Reduz o experimento aos contratos comerciais que a comunicação deve preservar. */
  private Map<String, Object> experimentContract(Experiment experiment) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", experiment.getId());
    result.put("name", experiment.getName());
    result.put("primaryCta", experiment.getPrimaryCta());
    result.put("checkoutUrl", experiment.getCommercialCheckoutUrl());
    result.put("unitPriceBrl", experiment.getUnitPrice());
    result.put("platform", experiment.getPlatform());
    result.put("campaignObjective", experiment.getCampaignObjective());
    result.put("currentLandingHtml", experiment.getHtmlGeraLanding());
    return java.util.Collections.unmodifiableMap(result);
  }

  /** Reduz o produto à experiência real, identidade e provas que Dédalo materializou. */
  private Map<String, Object> productContract(Product product) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", product.getId());
    result.put("slug", product.getSlug());
    result.put("name", product.getName());
    result.put("internalName", product.getInternalName());
    result.put("productType", product.getProductType());
    result.put("productFormat", product.getProductFormat());
    result.put("deliveryMode", product.getDeliveryMode());
    result.put("revenueModel", product.getRevenueModel());
    result.put("valueUnit", product.getValueUnit());
    result.put("valueEvidenceMetric", product.getValueEvidenceMetric());
    result.put("publicUrl", product.getPublicUrl());
    result.put("logoUrl", product.getLogoUrl());
    result.put("colorPalette", product.getColorPalette());
    result.put("languageStyle", product.getLanguageStyle());
    result.put("sevenDayJourney", product.getSevenDayJourney());
    result.put("supportMaterialPositioning", product.getSupportMaterialPositioning());
    result.put("primaryCta", product.getPrimaryCta());
    result.put("pdeExperience", product.getPdeExperienceJson());
    result.put("scientificEvidencePack", product.getScientificEvidencePack());
    result.put("socialProof", product.getSocialProof());
    return java.util.Collections.unmodifiableMap(result);
  }

  /** Localiza o plano explicitamente ou pelo experimento governado. */
  private Optional<ResolvedScope> scope(String sourceReference) {
    if (sourceReference == null) return Optional.empty();
    Matcher planMatcher = PLAN_REFERENCE.matcher(sourceReference.trim());
    if (planMatcher.matches()) {
      Long planId = Long.valueOf(planMatcher.group(1));
      Integer requestedVersion =
          planMatcher.group(2) == null ? null : Integer.valueOf(planMatcher.group(2));
      return plans
          .findById(planId)
          .map(plan -> new ResolvedScope(plan, experiment(plan), requestedVersion));
    }
    Matcher experimentMatcher = EXPERIMENT_REFERENCE.matcher(sourceReference.trim());
    if (!experimentMatcher.matches()) return Optional.empty();
    Long experimentId = Long.valueOf(experimentMatcher.group(1));
    return plans.findByExperimentReference(experimentId).stream()
        .findFirst()
        .map(plan -> new ResolvedScope(plan, experiment(plan, experimentId), null));
  }

  /** Escolhe o experimento direto ou o primeiro membro estável do portfólio. */
  private Experiment experiment(CommercialPlan plan) {
    if (plan.getExperiment() != null) return plan.getExperiment();
    return plan.getExperiments().stream()
        .sorted(java.util.Comparator.comparing(Experiment::getId))
        .findFirst()
        .orElse(null);
  }

  /** Confirma que o experimento solicitado pertence ao plano resolvido. */
  private Experiment experiment(CommercialPlan plan, Long experimentId) {
    if (plan.getExperiment() != null && experimentId.equals(plan.getExperiment().getId())) {
      return plan.getExperiment();
    }
    return plan.getExperiments().stream()
        .filter(candidate -> experimentId.equals(candidate.getId()))
        .findFirst()
        .orElse(null);
  }

  /** Expõe lacuna funcional sem fabricar conteúdo para o modelo. */
  private Map<String, Object> unavailable(String sourceReference, String reason) {
    return Map.of(
        "availability",
        "MISSING",
        "contractVersion",
        "IRIS_INPUT_V1",
        "sourceReference",
        sourceReference,
        "reason",
        reason,
        "requiredAction",
        "Concluir o contrato ausente no agente proprietário antes de executar Íris.");
  }

  /** Calcula a identidade determinística do snapshot comercial. */
  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      log.error("Falha ao calcular hash do contexto de Íris.", ex);
      throw new IllegalStateException("SHA-256 indisponível para o contexto de comunicação.", ex);
    }
  }

  /** Representa plano, experimento e versão solicitada dentro do mesmo escopo. */
  private record ResolvedScope(
      CommercialPlan plan, Experiment experiment, Integer requestedPlanVersion) {}
}
