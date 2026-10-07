package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleConstructionContext;
import com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: entregar a Íris os contratos e as provas aprovadas do próprio ciclo privado.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IrisLearningCycleContext {
  public static final String MODE = "LEARNING_CYCLE_PRIVATE";
  private static final String CONSTRUCTION = "pde-construction-approval";
  private static final Set<String> REVIEWS =
      Set.of(
          "technicalHomologation",
          "psiqueAdherent",
          "psiqueRecovery",
          "psiqueSafety",
          "commercialIntegrityReview");
  private final LearningSalesCycleRepository cycles;
  private final ExperimentRepository experiments;
  private final BusinessProcessChainDefinitionRepository chains;
  private final LearningCycleConstructionContext construction;
  private final BusinessProcessActivityInstanceRepository instances;
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  /**
   * Reconhece o sucessor exato e mantém a lacuna explícita, sem buscar provas de plano ou produto.
   */
  @Transactional(readOnly = true)
  public Optional<Map<String, Object>> resolve(String reference) {
    if (reference == null || !reference.matches("experiment:[1-9][0-9]*")) return Optional.empty();
    var cycle = cycles.findByExperimentId(Long.parseLong(reference.substring(11))).orElse(null);
    if (cycle == null || cycle.isBaseline()) return Optional.empty();
    try {
      return Optional.of(context(reference, cycle));
    } catch (Exception ex) {
      log.error(
          "Entrada privada de Íris indisponível. sourceReference={} cycleId={} productId={}",
          reference,
          cycle.getId(),
          cycle.getProductId(),
          ex);
      return Optional.of(
          Map.of(
              "availability",
              "MISSING",
              "inputReadiness",
              "BLOCKED",
              "mode",
              MODE,
              "sourceReference",
              reference,
              "cycleId",
              cycle.getId(),
              "reason",
              "Confirme estratégia, economia, protótipo e gate da mesma versão deste ciclo."));
    }
  }

  /** Consolida estratégia, economia e gate, preservando a resolução auditável de provas. */
  private Map<String, Object> context(String reference, LearningSalesCycle cycle) throws Exception {
    require(
        "OPEN".equals(cycle.getStatus())
            && Set.of("ADJUSTMENT", "VALIDATION").contains(cycle.getStage()),
        "Ciclo privado não aberto.");
    var experiment = experiments.findById(cycle.getExperimentId()).orElseThrow();
    var target = construction.resolve(reference, experiment, CONSTRUCTION).orElseThrow();
    var pde = target.pdeContext();
    require(
        pde != null
            && Objects.equals(cycle.getProductId(), target.productId())
            && Objects.equals(cycle.getProductVersion(), target.experienceVersion())
            && pde.path("lineage").path("learningCycleId").asLong() == cycle.getId(),
        "Identidade divergente.");
    var process =
        chains.findById(cycle.getChainDefinitionId()).orElseThrow().getItems().stream()
            .map(item -> item.getProcessDefinition())
            .filter(value -> CONSTRUCTION.equals(value.getProcessCode()))
            .findFirst()
            .orElseThrow();
    var gate =
        instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                process.getId(), reference)
            .stream()
            .filter(
                value ->
                    "agentValidationGate".equals(value.getActivityDefinition().getActivityId()))
            .max(Comparator.comparing(value -> value.getOccurrenceNumber()))
            .orElseThrow();
    require(
        "COMPLETED".equals(gate.getStatus()) && gate.isObjectiveAchieved(), "Gate não aprovado.");
    var proof = json.readTree(gate.getObjectiveEvidenceJson());
    require(
        "PDE_AGENT_VALIDATION_GATE_V1".equals(proof.path("evidenceType").asText())
            && reference.equals(proof.path("sourceReference").asText())
            && proof.path("productId").asLong() == target.productId()
            && target.productSlug().equals(proof.path("productSlug").asText())
            && target.experienceVersion().equals(proof.path("prototypeVersion").asText())
            && Objects.equals(target.publicUrl(), proof.path("publicUrl").asText())
            && "AGENT_VALIDATION".equals(proof.path("trafficClass").asText())
            && "mh_internal_test".equals(proof.path("internalMarker").asText())
            && !proof.path("humanEvidenceClaimed").asBoolean(true)
            && !proof.path("commercialEvidenceClaimed").asBoolean(true)
            && !proof.path("paymentEnabled").asBoolean(true)
            && !proof.path("publicationAuthorized").asBoolean(true)
            && !proof.path("campaignAuthorized").asBoolean(true)
            && proof.path("mediaSpendAuthorizedBrl").asDouble(-1) == 0,
        "Prova do gate incompatível com a versão e os limites privados.");
    Map<String, PdeValidationTaskSnapshot> latest = new LinkedHashMap<>();
    var history = tasks.findPdeValidationTaskSnapshots(reference, CONSTRUCTION);
    history.stream()
        .filter(value -> process.getId().equals(value.processDefinitionId()))
        .forEach(
            value ->
                latest.merge(
                    value.processActivityId(),
                    value,
                    (old, current) -> current.id() > old.id() ? current : old));
    List<Map<String, Object>> artifacts = new ArrayList<>();
    var seen = new java.util.HashSet<String>();
    for (var evidence : proof.path("taskEvidence")) {
      String activity = evidence.path("activityId").asText();
      var task = latest.get(activity);
      require(
          REVIEWS.contains(activity)
              && seen.add(activity)
              && task != null
              && task.id() == evidence.path("taskId").asLong()
              && "COMPLETED".equals(task.status())
              && sha(task.resultJson()).equals(evidence.path("resultSha256").asText()),
          "Uma tentativa posterior substituiu a prova aprovada.");
      artifacts.add(
          Map.of(
              "taskId",
              task.id(),
              "activityId",
              activity,
              "agentKey",
              evidence.path("agentKey").asText(),
              "result",
              json.readTree(task.resultJson()),
              "resultSha256",
              evidence.path("resultSha256").asText()));
    }
    require(seen.equals(REVIEWS), "Conjunto de avaliações incompleto.");
    long technicalId = latest.get("technicalHomologation").id();
    var correction = latest.get("prototypeCorrection");
    require(
        correction == null
            || ("COMPLETED".equals(correction.status()) && correction.id() < technicalId)
            || evidenceCorrectionResolved(
                correction, latest.get("commercialIntegrityReview"), history, pde, proof),
        "Nova correção exige novo gate.");
    var lineage = pde.path("lineage");
    for (String key : List.of("strategyTaskId", "economicsTaskId", "architectureTaskId"))
      require(
          lineage.path(key).asLong() > 0 && lineage.path(key).asLong() < technicalId,
          "Planejamento posterior exige nova homologação.");
    var strategy = pde.path("marketStrategy");
    String strategyVersion = strategy.path("contractVersion").asText();
    require(
        Set.of("MARKET_STRATEGY_V3", "MARKET_STRATEGY_V4").contains(strategyVersion),
        "Estratégia governada ausente.");
    var strategyReference =
        Map.<String, Object>of(
            "availability",
            "AVAILABLE",
            "sourceAgent",
            "ATENA",
            "sourceReference",
            reference,
            "cycleId",
            cycle.getId(),
            "strategistTaskId",
            lineage.path("strategyTaskId").asLong(),
            "contractVersion",
            strategyVersion,
            "contentHash",
            sha(json.writeValueAsString(strategy)),
            "contract",
            strategy);
    artifacts.add(
        Map.of(
            "taskId",
            lineage.path("economicsTaskId").asLong(),
            "agentKey",
            "financial-agent",
            "contractVersion",
            pde.path("economicsContractVersion").asText("PDE_PRIVATE_ECONOMICS_V1"),
            "result",
            pde.path("economics")));
    artifacts.add(
        Map.of(
            "taskId",
            lineage.path("architectureTaskId").asLong(),
            "agentKey",
            "landing-generator",
            "result",
            pde.path("harness")));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("availability", "AVAILABLE");
    result.put("inputReadiness", "READY");
    result.put("contractVersion", "IRIS_INPUT_V1");
    result.put("mode", MODE);
    result.put("sourceReference", reference);
    result.put("cycleId", cycle.getId());
    result.put("chainDefinitionId", cycle.getChainDefinitionId());
    result.put("prototypeVersion", target.experienceVersion());
    result.put("marketStrategicContract", strategyReference);
    result.put("product", product(target));
    result.put(
        "experiment", Map.of("id", experiment.getId(), "status", experiment.getStatus().name()));
    result.put("privatePrototypeAcceptance", pde.path("privatePrototypeAcceptance"));
    result.put(
        "approvedDestination",
        Map.of(
            "contractVersion",
            "PRIVATE_PDE_DESTINATION_V1",
            "type",
            "APPROVED_PRIVATE_PDE",
            "url",
            target.publicUrl(),
            "prototypeVersion",
            target.experienceVersion(),
            "requiresLandingGeneration",
            false));
    result.put("approvedUpstreamArtifacts", List.copyOf(artifacts));
    result.put("communicationArtifacts", communicationArtifacts(reference, cycle));
    result.put("approvedLandingAssets", List.of());
    result.put("missingRequiredPredecessors", List.of());
    result.put("gateInstanceId", gate.getId());
    result.put("validationGate", proof);
    if (proof.has("evidenceCorrectionResolution"))
      result.put("operationalControlEvidence", pde.path("operationalControlEvidence"));
    result.put("inheritedLearning", pde.path("inheritedLearning"));
    result.put("cycleBrief", pde.path("cycleBrief"));
    result.put(
        "validationPolicy",
        Map.of(
            "mode",
            "AGENT_VALIDATION",
            "humanReadingsRequired",
            false,
            "historicalHumanCriteriaAreNotCurrentGate",
            true,
            "commercialEvidenceClaimed",
            false));
    result.put("publicationAuthorized", false);
    result.put("externalMediaSpendAuthorized", false);
    result.put("paymentEnabled", false);
    result.put(
        "publicationBoundary",
        "Preparar comunicação privada com provas sintéticas; sem alegar preferência, venda ou satisfação humana e sem autorizar publicação, cobrança ou mídia.");
    return java.util.Collections.unmodifiableMap(result);
  }

  /**
   * Confere o recibo calculado pelo gate, seus hashes e a prova atual antes de preservar revisões.
   */
  private boolean evidenceCorrectionResolved(
      PdeValidationTaskSnapshot correction,
      PdeValidationTaskSnapshot review,
      List<PdeValidationTaskSnapshot> history,
      JsonNode pde,
      JsonNode proof)
      throws Exception {
    var resolution = proof.path("evidenceCorrectionResolution");
    var report = pde.path("operationalControlEvidence");
    if (!"BLOCKED".equals(correction.status())
        || review == null
        || !"COMPLETED".equals(review.status())
        || !"PDE_EVIDENCE_CORRECTION_RESOLUTION_V1"
            .equals(resolution.path("contractVersion").asText())
        || correction.id() != resolution.path("correctionTaskId").asLong()
        || review.id() != resolution.path("independentReviewTaskId").asLong()
        || review.id() <= correction.id()
        || !sha(correction.resultJson()).equals(resolution.path("correctionResultSha256").asText())
        || !proof.path("sourceReference").equals(resolution.path("sourceReference"))
        || !proof.path("prototypeVersion").equals(resolution.path("prototypeVersion"))
        || !proof.path("prototypeVersion").equals(report.path("prototypeVersion"))
        || !report.path("reportSha256").asText().matches("[a-f0-9]{64}")
        || !report.path("reportSha256").equals(resolution.path("reportSha256"))) return false;
    var rejected =
        history.stream()
            .filter(task -> task.id() == resolution.path("rejectedTaskId").asLong())
            .findFirst()
            .orElse(null);
    return rejected != null
        && rejected.id() < correction.id()
        && "BLOCKED".equals(rejected.status())
        && "FUNCTIONAL_ADJUSTMENT".equals(rejected.blockerCategory())
        && "commercialIntegrityReview".equals(rejected.processActivityId())
        && sha(rejected.resultJson()).equals(resolution.path("rejectedResultSha256").asText())
        && "APPROVED".equals(json.readTree(review.resultJson()).path("decision").asText())
        && proof
            .path("prototypeVersion")
            .equals(json.readTree(review.resultJson()).path("prototypeVersion"))
        && java.time.Instant.parse(report.path("generatedAt").asText())
            .equals(java.time.Instant.parse(resolution.path("reportGeneratedAt").asText()));
  }

  /** Entrega somente a última tentativa de cada atividade de comunicação do próprio ciclo. */
  private List<Map<String, Object>> communicationArtifacts(
      String reference, LearningSalesCycle cycle) throws Exception {
    Map<String, com.marketinghub.agenttask.AgentTaskFunctionalSnapshot> latest =
        new LinkedHashMap<>();
    for (var task :
        tasks.findFunctionalSnapshots(
            reference,
            Set.of(
                "pde-communication-sales-journey",
                "creative-production-approval",
                "landing-page-generation"),
            cycle.getCreatedAt())) {
      if (task.processDefinitionId() == null
          || task.agentKey() == null
          || !"communication-director".equals(task.agentKey())
          || task.createdAt() == null
          || (cycle.getCreatedAt() != null && task.createdAt().isBefore(cycle.getCreatedAt())))
        continue;
      String process = task.processCode();
      if (!Set.of(
              "pde-communication-sales-journey",
              "creative-production-approval",
              "landing-page-generation")
          .contains(process)) continue;
      latest.merge(
          process + ":" + task.processActivityId(), task, (a, b) -> a.id() > b.id() ? a : b);
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (var task : latest.values()) {
      if (!"COMPLETED".equals(task.status())) continue;
      var output = json.readTree(task.resultJson());
      require(
          "IRIS_COMMUNICATION_V1".equals(output.path("contractVersion").asText())
              && reference.equals(output.path("sourceReference").asText()),
          "Artefato de comunicação divergente.");
      result.add(
          Map.of(
              "taskId",
              task.id(),
              "processDefinitionId",
              task.processDefinitionId(),
              "activityId",
              task.processActivityId(),
              "result",
              output,
              "resultSha256",
              sha(task.resultJson())));
    }
    return List.copyOf(result);
  }

  /** Expõe a versão privada correta em vez da página histórica publicada do produto. */
  private Map<String, Object> product(AgentTaskTargetResponse target) {
    return Map.of(
        "id",
        target.productId(),
        "slug",
        target.productSlug(),
        "name",
        target.productName(),
        "internalName",
        target.productInternalName(),
        "publicUrl",
        target.publicUrl(),
        "experienceVersion",
        target.experienceVersion());
  }

  /** Interrompe a projeção antes de disponibilizar um contrato sem prova suficiente. */
  private void require(boolean valid, String reason) {
    if (!valid) throw new IllegalStateException(reason);
  }

  /** Preserva a identidade do conteúdo efetivamente aprovado, sem reescrever o contrato. */
  private String sha(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
