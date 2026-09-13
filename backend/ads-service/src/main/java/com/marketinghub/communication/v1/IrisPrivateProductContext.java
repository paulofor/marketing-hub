package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.product.service.agentvalidation.PdeAgentValidationGateActivityExecutor;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: entregar a comunicação privada do produto validado antes do experimento. */
@Component
@RequiredArgsConstructor
@Slf4j
public class IrisPrivateProductContext {
  public static final String MODE = "PRODUCT_PRIVATE";
  private static final String CONSTRUCTION = "pde-construction-approval";
  private final ProductRepository products;
  private final AgentTaskRepository tasks;
  private final BusinessProcessActivityInstanceRepository instances;
  private final AgentTaskTargetContextProvider targets;
  private final PdeAgentValidationGateActivityExecutor gateValidator;
  private final ObjectMapper json;

  /** Reconhece somente a referência canônica, sem converter identidade de experimento ou plano. */
  public static boolean supports(String reference) {
    return reference != null && reference.matches("product:[1-9][0-9]*@agent-validation-v1");
  }

  /** Mantém bloqueios explícitos antes de chamar o modelo, sem recorrer a contratos comerciais. */
  @Transactional(readOnly = true)
  public Optional<Map<String, Object>> resolve(String reference) {
    if (!supports(reference)) return Optional.empty();
    try {
      return Optional.of(context(reference));
    } catch (Exception ex) {
      log.warn("Comunicação privada do produto aguarda provas. sourceReference={}", reference, ex);
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
              "reason",
              Objects.requireNonNullElse(
                  ex.getMessage(),
                  "Confirme estratégia, economia, protótipo e gate da mesma versão do produto.")));
    }
  }

  /** Confere contratos de origem, gate vigente e identidade sem criar ciclo ou experimento. */
  private Map<String, Object> context(String reference) throws Exception {
    long productId = Long.parseLong(reference.substring(8, reference.indexOf('@')));
    var product = products.findById(productId).orElseThrow();
    require(
        "PDE_AGENT_VALIDATED_V1".equals(product.getValidationDefinitionVersion()),
        "Conclua a validação multiagente deste produto antes da comunicação.");
    var target = targets.resolve(reference, CONSTRUCTION).orElseThrow();
    require(
        Objects.equals(productId, target.productId())
            && reference.equals(target.sourceReference())
            && target.experimentId() == null,
        "O alvo privado pertence a outro contexto.");
    var pde = target.pdeContext();
    require(pde != null && pde.isObject(), "O contrato PDE privado não está disponível.");
    var gate =
        instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                CONSTRUCTION, reference)
            .stream()
            .filter(i -> "agentValidationGate".equals(i.getActivityDefinition().getActivityId()))
            .max(Comparator.comparing(i -> i.getId()))
            .orElseThrow(
                () ->
                    new IllegalStateException("O produto não possui gate multiagente registrado."));
    var definition = gate.getActivityDefinition();
    require(
        "COMPLETED".equals(gate.getStatus()) && gate.isObjectiveAchieved(),
        "O último gate multiagente não está aprovado.");
    var readiness =
        gateValidator.readiness(definition.getProcessDefinition(), definition, product, reference);
    require(readiness.ready(), readiness.reason());
    var proof = json.readTree(gate.getObjectiveEvidenceJson());
    require(
        "PDE_AGENT_VALIDATION_GATE_V1".equals(proof.path("evidenceType").asText())
            && reference.equals(proof.path("sourceReference").asText())
            && productId == proof.path("productId").asLong()
            && product.getSlug().equals(proof.path("productSlug").asText())
            && Objects.equals(target.experienceVersion(), proof.path("prototypeVersion").asText())
            && Objects.equals(target.publicUrl(), proof.path("publicUrl").asText())
            && target.publicUrl() != null
            && target.publicUrl().startsWith("https://")
            && "AGENT_VALIDATION".equals(proof.path("trafficClass").asText())
            && "mh_internal_test".equals(proof.path("internalMarker").asText())
            && !proof.path("paymentEnabled").asBoolean(true)
            && !proof.path("publicationAuthorized").asBoolean(true)
            && !proof.path("campaignAuthorized").asBoolean(true)
            && !proof.path("humanEvidenceClaimed").asBoolean(true)
            && !proof.path("commercialEvidenceClaimed").asBoolean(true)
            && proof.path("mediaSpendAuthorizedBrl").asDouble(-1) == 0,
        "O gate registrado não corresponde à versão e aos limites privados do produto.");
    var reviewTasks = tasks.findPdeValidationTaskSnapshots(reference, CONSTRUCTION);
    List<Map<String, Object>> artifacts = new ArrayList<>();
    var reviewed = new HashSet<String>();
    for (var evidence : proof.path("taskEvidence")) {
      String activity = evidence.path("activityId").asText();
      var task =
          reviewTasks.stream()
              .filter(
                  t ->
                      definition.getProcessDefinition().getId().equals(t.processDefinitionId())
                          && activity.equals(t.processActivityId()))
              .max(Comparator.comparing(t -> t.id()))
              .orElseThrow();
      require(
          reviewed.add(activity)
              && task.id() == evidence.path("taskId").asLong()
              && "COMPLETED".equals(task.status())
              && sha(task.resultJson()).equals(evidence.path("resultSha256").asText()),
          "Uma avaliação posterior exige renovar o gate antes da comunicação.");
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
    require(
        reviewed.equals(
            Set.of(
                "technicalHomologation",
                "psiqueAdherent",
                "psiqueRecovery",
                "psiqueSafety",
                "commercialIntegrityReview")),
        "O conjunto aprovado de provas está incompleto.");
    var lineage = pde.path("lineage");
    require(
        lineage.path("cycleId").asLong() > 0 && lineage.path("dossierId").asLong() > 0,
        "Falta a origem da estratégia, economia e arquitetura do produto.");
    String origin = "product-discovery-cycle:" + lineage.path("cycleId").asLong();
    var originTasks =
        tasks.findFunctionalSnapshots(origin, Set.of("pde-commercial-plan-offer"), null);
    var strategyTask =
        upstream(
            originTasks,
            "marketStrategy",
            "experiment-strategist",
            pde,
            "marketStrategicContract",
            "marketStrategy");
    var economicsTask =
        upstream(originTasks, "economics", "financial-agent", pde, "economics", "economics");
    var architectureTask =
        upstream(
            originTasks,
            "productArchitecture",
            "landing-generator",
            pde,
            "productArchitecture",
            "harness");
    long technicalTaskId =
        proof.path("taskEvidence").findValues("taskId").stream()
            .mapToLong(JsonNode::asLong)
            .min()
            .orElseThrow();
    require(
        List.of(strategyTask, economicsTask, architectureTask).stream()
            .allMatch(t -> t.id() < technicalTaskId),
        "Novo planejamento exige homologação e gate posteriores à alteração.");
    var strategyResult = json.readTree(strategyTask.resultJson());
    require(
        lineage.path("dossierId").asLong() == strategyResult.path("selectedDossierId").asLong()
            && lineage.path("opportunityId").asLong()
                == strategyResult.path("selectedOpportunityId").asLong()
            && "MARKET_STRATEGY_V3"
                .equals(pde.path("marketStrategy").path("contractVersion").asText()),
        "A estratégia aprovada não corresponde à descoberta que originou o produto.");
    for (var task : List.of(economicsTask, architectureTask)) artifacts.add(artifact(task));
    var strategy =
        Map.<String, Object>of(
            "availability",
            "AVAILABLE",
            "sourceAgent",
            "ATENA",
            "sourceReference",
            origin,
            "strategistTaskId",
            strategyTask.id(),
            "contractVersion",
            "MARKET_STRATEGY_V3",
            "contentHash",
            sha(pde.path("marketStrategy").toString()),
            "contract",
            pde.path("marketStrategy"));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("availability", "AVAILABLE");
    result.put("inputReadiness", "READY");
    result.put("contractVersion", "IRIS_INPUT_V1");
    result.put("mode", MODE);
    result.put("sourceReference", reference);
    result.put("discoveryLineage", lineage);
    result.put("prototypeVersion", target.experienceVersion());
    result.put(
        "product",
        Map.of(
            "id",
            productId,
            "slug",
            product.getSlug(),
            "name",
            product.getName(),
            "internalName",
            product.getInternalName(),
            "publicUrl",
            target.publicUrl(),
            "experienceVersion",
            target.experienceVersion()));
    result.put("marketStrategicContract", strategy);
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
    result.put(
        "communicationArtifacts",
        communicationArtifacts(reference, proof.path("completedAt").asText()));
    result.put("approvedLandingAssets", List.of());
    result.put("missingRequiredPredecessors", List.of());
    result.put("gateInstanceId", gate.getId());
    result.put("constructionProcessDefinitionId", definition.getProcessDefinition().getId());
    result.put("validationGate", proof);
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
    return Collections.unmodifiableMap(result);
  }

  /** Exige o último parecer aprovado e a correspondência integral com o contrato materializado. */
  private AgentTaskFunctionalSnapshot upstream(
      List<AgentTaskFunctionalSnapshot> candidates,
      String activity,
      String agent,
      JsonNode pde,
      String resultKey,
      String productKey)
      throws Exception {
    var task =
        candidates.stream()
            .filter(t -> activity.equals(t.processActivityId()))
            .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id))
            .orElseThrow(() -> new IllegalStateException("Falta o parecer de origem: " + activity));
    require(
        "COMPLETED".equals(task.status()) && agent.equals(task.agentKey()),
        "O último parecer de origem não está aprovado: " + activity);
    var result = json.readTree(task.resultJson());
    require(
        "APPROVE".equals(result.path("decision").asText())
            && result.path(resultKey).isObject()
            && result.path(resultKey).equals(pde.path(productKey)),
        "O contrato do produto diverge do parecer aprovado: " + activity);
    return task;
  }

  /** Preserva a origem real, o resultado completo e o hash de cada predecessor. */
  private Map<String, Object> artifact(AgentTaskFunctionalSnapshot task) throws Exception {
    return Map.of(
        "taskId",
        task.id(),
        "agentKey",
        task.agentKey(),
        "activityId",
        task.processActivityId(),
        "processDefinitionId",
        task.processDefinitionId(),
        "result",
        json.readTree(task.resultJson()),
        "resultSha256",
        sha(task.resultJson()));
  }

  /** Entrega apenas a tentativa vigente de cada artefato de comunicação do próprio produto. */
  private List<Map<String, Object>> communicationArtifacts(String reference, String approvedAt)
      throws Exception {
    Map<String, AgentTaskFunctionalSnapshot> latest = new LinkedHashMap<>();
    for (var task :
        tasks.findFunctionalSnapshots(
            reference,
            Set.of(
                "pde-communication-sales-journey",
                "creative-production-approval",
                "landing-page-generation"),
            null)) {
      if (!"communication-director".equals(task.agentKey())) continue;
      latest.merge(
          task.processCode() + ":" + task.processActivityId(),
          task,
          (a, b) -> a.id() > b.id() ? a : b);
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (var task : latest.values()) {
      if (!"COMPLETED".equals(task.status())) continue;
      if (!approvedAt.isBlank()
          && (task.createdAt() == null
              || task.createdAt().isBefore(java.time.Instant.parse(approvedAt)))) continue;
      var output = json.readTree(task.resultJson());
      require(
          "IRIS_COMMUNICATION_V1".equals(output.path("contractVersion").asText())
              && reference.equals(output.path("sourceReference").asText()),
          "Artefato de comunicação divergente.");
      result.add(artifact(task));
    }
    return List.copyOf(result);
  }

  /** Calcula a identidade dos bytes persistidos, sem normalizar o resultado auditável. */
  private String sha(String text) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
  }

  /** Interrompe a projeção quando uma condição obrigatória não foi comprovada. */
  private void require(boolean condition, String reason) {
    if (!condition) throw new IllegalStateException(reason);
  }
}
