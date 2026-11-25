package com.marketinghub.businessprocess.automation.v1.service.continuity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleConstructionContext;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleWorkResolver;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.valuedelivery.v1.service.ProductValueContract;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: conferir a adoção inicial da construção com as entradas reais do planejamento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FirstProcessVersionContinuity {
  public static final String CONSTRUCTION = "pde-construction-approval";
  private static final String PLANNING = "pde-commercial-plan-offer";
  private static final Map<String, String> INPUTS =
      Map.of(
          "marketStrategy", "strategyTaskId",
          "economics", "economicsTaskId",
          "productArchitecture", "architectureTaskId");

  private final ProcessRunRepository runs;
  private final LearningSalesCycleRepository cycles;
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessActivityDefinitionRepository definitions;
  private final BusinessProcessActivityInstanceRepository instances;
  private final AgentTaskRepository tasks;
  private final ExperimentRepository experiments;
  private final LearningCycleWorkResolver work;
  private final LearningCycleConstructionContext construction;
  private final ProcessVersionContinuityLookup lookup;
  private final ObjectMapper json;

  /** Responsabilidade: identificar uma entrega existente usada como entrada da construção. */
  public record Input(String name, Long occurrenceId, Long taskId) {}

  /** Responsabilidade: registrar o escopo e a compatibilidade verificada no destino inicial. */
  public record Compatibility(
      String policy,
      Long publishedChainId,
      Integer sourceVersion,
      Integer targetVersion,
      Long experimentId,
      String productVersion,
      List<Input> inputs,
      List<String> updatedRequirements,
      String targetContractHash,
      String planningContractHash) {}

  /** Responsabilidade: transportar a avaliação e a origem da primeira execução. */
  record Assessment(ProcessRun predecessor, Compatibility compatibility, String blocker) {}

  /** Confere etapa, exclusividade, grafo e entregas oficiais antes de permitir a adoção. */
  Assessment assess(
      Product product,
      LearningSalesCycle cycle,
      BusinessProcessChainDefinition originalChain,
      BusinessProcessChainDefinition publishedChain,
      BusinessProcessDefinition retired,
      BusinessProcessDefinition target,
      String reference) {
    if (!CONSTRUCTION.equals(retired.getProcessCode())
        || !("experiment:" + cycle.getExperimentId()).equals(reference))
      return blocked("A primeira passagem exige construção vinculada ao experimento deste ciclo.");
    if (!"OPEN".equals(cycle.getStatus()))
      return blocked("Ciclo encerrado. As entregas continuam disponíveis no histórico.");
    if (!"ADJUSTMENT".equals(cycle.getStage()))
      return blocked(
          "O planejamento ainda precisa registrar a passagem para o ajuste. Confira a execução do Processo 2.");
    if (!"RETIRED".equals(retired.getStatus())
        || !"PUBLISHED".equals(target.getStatus())
        || !"PRODUCT".equals(target.getExecutionScope())
        || publishedChain == null
        || !"PUBLISHED".equals(publishedChain.getStatus())
        || !Objects.equals(originalChain.getChainCode(), publishedChain.getChainCode())
        || target.getVersionNumber() <= retired.getVersionNumber())
      return blocked(
          "A construção precisa de uma versão posterior publicada na mesma família de cadeias.");

    var experiment =
        experiments
            .findById(cycle.getExperimentId())
            .filter(
                value ->
                    value.getProduct() != null
                        && Objects.equals(value.getProduct().getId(), product.getId())
                        && value.getStatus() == ExperimentStatus.PLANNED);
    if (experiment.isEmpty())
      return blocked("A primeira construção exige um experimento planejado deste produto.");
    var pending = work.resolve(cycle);
    if (pending == null || !retired.getId().equals(pending.processDefinitionId()))
      return blocked("A construção escolhida ainda não é o trabalho pendente deste ciclo.");
    var versions =
        processes.findAllByProcessCodeOrderByVersionNumberDesc(CONSTRUCTION).stream()
            .map(BusinessProcessDefinition::getId)
            .toList();
    if (runs.existsByProductIdAndSourceReferenceAndProcessDefinitionIdIn(
            product.getId(), reference, versions)
        || tasks.existsBySourceReferenceAndProcessDefinitionProcessCode(reference, CONSTRUCTION)
        || !instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                CONSTRUCTION, reference)
            .isEmpty())
      return blocked(
          "Já existe trabalho de construção nesta referência. Confira o histórico antes de adotar outra versão.");

    Long planningId =
        originalChain.getItems().stream()
            .map(item -> item.getProcessDefinition())
            .filter(process -> PLANNING.equals(process.getProcessCode()))
            .map(BusinessProcessDefinition::getId)
            .findFirst()
            .map(id -> lookup.effective(product.getId(), cycle.getId(), reference, id))
            .orElse(null);
    if (planningId == null)
      return blocked(
          "A cadeia original não informa o planejamento responsável por esta construção.");
    var predecessor =
        runs.findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                product.getId(), planningId, originalChain.getId(), cycle.getId(), reference)
            .filter(
                run ->
                    "COMPLETED".equals(run.getStatus())
                        && run.getParentRunId() == null
                        && run.getCompletedActivities() > 0
                        && run.getRemainingActivities() == 0);
    if (predecessor.isEmpty())
      return blocked(
          "Conclua o planejamento deste ciclo para preparar a versão publicada da construção.");

    var oldActivities = activityContracts(retired);
    var newActivities = activityContracts(target);
    JsonNode oldGraph = read(retired.getDiagramJson());
    JsonNode newGraph = read(target.getDiagramJson());
    if (!compatibleGraph(oldGraph, newGraph)
        || oldActivities.isEmpty()
        || !oldActivities.keySet().equals(newActivities.keySet())
        || oldActivities.keySet().stream()
            .anyMatch(
                id ->
                    !operationalContract(oldActivities.get(id))
                        .equals(operationalContract(newActivities.get(id)))))
      return blocked(
          "A versão publicada mudou as atividades, os responsáveis ou o caminho da construção. É preciso mapear a integração antes de adotar esta versão.");

    // O próprio consumidor da construção confirma formato, responsáveis, decisões e ordem.
    var targetContext =
        construction
            .resolve(reference, experiment.orElseThrow(), CONSTRUCTION)
            .map(value -> value.pdeContext());
    if (targetContext.isEmpty())
      return blocked(
          "A construção ainda não consegue ler a estratégia, a economia e a arquitetura aceitas neste ciclo.");
    var inputs = acceptedInputs(predecessor.orElseThrow(), cycle, targetContext.orElseThrow());
    if (inputs.size() != INPUTS.size())
      return blocked(
          "As entregas do planejamento precisam estar aceitas nesta candidata e vinculadas às suas ocorrências.");
    var updated =
        newActivities.keySet().stream()
            .filter(id -> !oldActivities.get(id).equals(newActivities.get(id)))
            .map(id -> newActivities.get(id).path("label").asText(id))
            .toList();
    var compatibility =
        new Compatibility(
            "FIRST_CONSTRUCTION_V1",
            publishedChain.getId(),
            retired.getVersionNumber(),
            target.getVersionNumber(),
            cycle.getExperimentId(),
            cycle.getProductVersion(),
            inputs,
            updated,
            ProductValueContract.hash(json.valueToTree(List.of(newGraph, newActivities))),
            planningHash(targetContext.orElseThrow()));
    return new Assessment(predecessor.orElseThrow(), compatibility, null);
  }

  /** Confere o recibo e obtém a origem somente para a primeira retomada. */
  public java.util.Optional<ProcessRun> predecessorToRelease(ProcessRun destination) {
    var receipt =
        lookup
            .firstExecution(
                destination.getProductId(),
                destination.getLearningCycleId(),
                destination.getSourceReference(),
                null,
                destination.getProcessDefinitionId())
            .orElse(null);
    if (receipt == null || lookup.released(receipt)) return java.util.Optional.empty();
    var cycle =
        cycles
            .findLocked(destination.getProductId(), destination.getLearningCycleId())
            .orElseThrow();
    var predecessor = runs.findById(receipt.predecessorRunId()).orElseThrow();
    if (!Objects.equals(receipt.binding().successorRunId(), destination.getId())
        || !Objects.equals(receipt.binding().chainId(), destination.getChainDefinitionId())
        || !Objects.equals(predecessor.getProductId(), destination.getProductId())
        || !Objects.equals(predecessor.getLearningCycleId(), destination.getLearningCycleId())
        || !Objects.equals(predecessor.getChainDefinitionId(), destination.getChainDefinitionId())
        || !Objects.equals(predecessor.getSourceReference(), destination.getSourceReference())
        || !Objects.equals(cycle.getChainDefinitionId(), destination.getChainDefinitionId())
        || !stillCompatible(predecessor, cycle, receipt.compatibility()))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT,
          "As entregas ou a candidata deste recibo mudaram. Confira o planejamento antes de retomar a construção.");
    return java.util.Optional.of(predecessor);
  }

  /** Confere a mesma entrada no momento de enfileirar, após o comando oficial de retomada. */
  private boolean stillCompatible(
      ProcessRun predecessor, LearningSalesCycle cycle, Compatibility recorded) {
    if (!"COMPLETED".equals(predecessor.getStatus())
        || !"ADJUSTMENT".equals(cycle.getStage())
        || !"OPEN".equals(cycle.getStatus())
        || !Objects.equals(recorded.experimentId(), cycle.getExperimentId())
        || !Objects.equals(recorded.productVersion(), cycle.getProductVersion())) return false;
    return experiments
        .findById(cycle.getExperimentId())
        .filter(value -> value.getStatus() == ExperimentStatus.PLANNED)
        .flatMap(
            experiment ->
                construction.resolve(predecessor.getSourceReference(), experiment, CONSTRUCTION))
        .map(value -> value.pdeContext())
        .filter(context -> recorded.planningContractHash().equals(planningHash(context)))
        .map(context -> recorded.inputs().equals(acceptedInputs(predecessor, cycle, context)))
        .orElse(false);
  }

  /** Identifica apenas as três entregas consumidas, preservando o documento original. */
  private String planningHash(JsonNode context) {
    return ProductValueContract.hash(
        json.valueToTree(
            List.of(
                context.path("marketStrategy"),
                context.path("economics"),
                context.path("harness"))));
  }

  /** Confere a linhagem consumida pelo executor com as ocorrências aceitas desta referência. */
  private List<Input> acceptedInputs(
      ProcessRun predecessor, LearningSalesCycle cycle, JsonNode context) {
    var lineage = context.path("lineage");
    if (lineage.path("learningCycleId").asLong() != cycle.getId()
        || lineage.path("productId").asLong() != cycle.getProductId()
        || lineage.path("experimentId").asLong() != cycle.getExperimentId()
        || !cycle.getProductVersion().equals(context.path("experienceVersion").asText()))
      return List.of();
    var accepted =
        instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                predecessor.getProcessDefinitionId(), predecessor.getSourceReference())
            .stream()
            .filter(
                i -> i.getCreatedAt() != null && !i.getCreatedAt().isBefore(cycle.getCreatedAt()))
            .collect(
                Collectors.toMap(
                    i -> i.getActivityDefinition().getActivityId(),
                    i -> i,
                    (older, latest) -> latest,
                    TreeMap::new));
    return List.of("marketStrategy", "economics", "productArchitecture").stream()
        .filter(accepted::containsKey)
        .map(id -> accepted.get(id))
        .filter(i -> i.isObjectiveAchieved() && "COMPLETED".equals(i.getStatus()))
        .flatMap(
            i -> {
              var definition = i.getActivityDefinition();
              long taskId = lineage.path(INPUTS.get(definition.getActivityId())).asLong();
              return tasks.existsByIdAndActivityInstance_IdAndStatus(taskId, i.getId(), "COMPLETED")
                  ? java.util.stream.Stream.of(new Input(definition.getName(), i.getId(), taskId))
                  : java.util.stream.Stream.empty();
            })
        .toList();
  }

  /** Compara o grafo e permite somente a adição conhecida do gate de entrega do tipo de produto. */
  private boolean compatibleGraph(JsonNode oldGraph, JsonNode newGraph) {
    if (!oldGraph.path("nodes").isArray()
        || !newGraph.path("nodes").isArray()
        || !oldGraph.path("flows").isArray()
        || !newGraph.path("flows").isArray()) return false;
    var before = ((com.fasterxml.jackson.databind.node.ObjectNode) oldGraph).deepCopy();
    var after = ((com.fasterxml.jackson.databind.node.ObjectNode) newGraph).deepCopy();
    if (!before.has("productValueDeliveryContract")
        && ProductValueContract.VERSION.equals(after.path("productValueDeliveryContract").asText()))
      after.remove("productValueDeliveryContract");
    for (var graph : List.of(before, after)) {
      var nodes = json.createArrayNode();
      graph.path("nodes").forEach(node -> nodes.add(operationalContract(node)));
      graph.set("nodes", nodes);
    }
    return before.equals(after);
  }

  /** Obtém o contrato por identidade de atividade, sem depender da ordem de gravação. */
  private Map<String, JsonNode> activityContracts(BusinessProcessDefinition process) {
    return definitions.findAllByProcessDefinitionIdOrderByIdAsc(process.getId()).stream()
        .collect(
            Collectors.toMap(
                BusinessProcessActivityDefinition::getActivityId,
                definition -> read(definition.getDefinitionJson()),
                (first, second) -> first,
                TreeMap::new));
  }

  /** Permite novos critérios descritivos, mantendo o contrato operacional dos executores. */
  private JsonNode operationalContract(JsonNode node) {
    if (!node.isObject()) return node;
    var copy = (com.fasterxml.jackson.databind.node.ObjectNode) node.deepCopy();
    copy.remove(List.of("description", "label"));
    return copy;
  }

  /** Lê o catálogo e interrompe a conferência quando o dado publicado é inválido. */
  private JsonNode read(String value) {
    try {
      return json.readTree(value);
    } catch (Exception ex) {
      log.error("Falha ao conferir catálogo da primeira construção", ex);
      throw new IllegalStateException("Contrato da construção inválido.", ex);
    }
  }

  /** Explica a condição funcional da passagem sem indicar uma execução fictícia. */
  private Assessment blocked(String reason) {
    return new Assessment(null, null, reason);
  }
}
