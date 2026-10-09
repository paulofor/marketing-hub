package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleConstructionContext;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Responsabilidade: reproduzir offline contratos exportados por leitura, sem acesso ao runtime. */
@EnabledIfEnvironmentVariable(named = "PRIVATE_JOURNEY_REPLAY", matches = ".+")
class PrivateCommunicationJourneyReplayTest {
  /** Confere contratos exportados e, quando disponível, sua projeção real antes da retomada. */
  @Test
  void completesUsingExportedEvidenceWithoutCallingProduction() throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var exported = json.readTree(Path.of(System.getenv("PRIVATE_JOURNEY_REPLAY")).toFile());
    var input = exported.path("context");
    String reference = input.path("sourceReference").asText();
    var product = Product.builder().id(input.path("product").path("id").asLong()).build();
    var parent = new BusinessProcessDefinition();
    parent.setId(exported.path("parentProcessDefinition").path("id").asLong(63L));
    parent.setProcessCode("pde-communication-sales-journey");
    parent.setVersionNumber(7);
    parent.setDiagramJson(
        """
        {"nodes":[{"id":"start","type":"START"},
        {"id":"communicationContract","type":"TASK","responsibleAgentKeys":["communication-director"],
        "responsibilityDomain":"COMMUNICATION_MATERIALIZATION","executionResourceCode":"iris-communication-worker"},
        {"id":"creatives","type":"TASK","subprocessCode":"creative-production-approval"},
        {"id":"destination","type":"TASK","subprocessCode":"landing-page-generation"},
        {"id":"integration","type":"TASK"},{"id":"gate","type":"GATEWAY"},{"id":"end","type":"END"}],
        "flows":[{"from":"start","to":"communicationContract"},{"from":"communicationContract","to":"creatives"},
        {"from":"creatives","to":"destination"},{"from":"destination","to":"integration"},
        {"from":"integration","to":"gate"},{"from":"gate","to":"end"}]}
        """);
    if (exported.has("parentProcessDefinition")) {
      parent.setVersionNumber(
          exported.path("parentProcessDefinition").path("version_number").asInt());
      parent.setDiagramJson(exported.path("parentProcessDefinition").path("diagram_json").asText());
    }
    var child = new BusinessProcessDefinition();
    child.setId(exported.path("childProcessDefinition").path("id").asLong(64L));
    child.setProcessCode("creative-production-approval");
    Map<Long, BusinessProcessActivityDefinition> definitions = new HashMap<>();
    for (var entry :
        Map.of(
                637L,
                "communicationContract",
                638L,
                "creatives",
                639L,
                "destination",
                640L,
                "integration",
                646L,
                "human")
            .entrySet()) {
      var a = new BusinessProcessActivityDefinition();
      a.setId(entry.getKey());
      a.setActivityId(entry.getValue());
      a.setProcessDefinition(entry.getKey() == 646L ? child : parent);
      definitions.put(a.getId(), a);
    }
    if (exported.has("activities")) {
      definitions.clear();
      for (var row : exported.path("activities")) {
        long processId = row.path("process_definition_id").asLong();
        var process =
            processId == parent.getId() ? parent : processId == child.getId() ? child : null;
        if (process == null) continue;
        var definition = new BusinessProcessActivityDefinition();
        definition.setId(row.path("id").asLong());
        definition.setActivityId(row.path("activity_id").asText());
        definition.setProcessDefinition(process);
        definitions.put(definition.getId(), definition);
      }
    }
    List<BusinessProcessActivityInstance> persisted = new ArrayList<>();
    for (var row : exported.path("instances")) {
      var activity = definitions.get(row.path("activity_definition_id").asLong());
      if (activity == null) continue;
      var instance = new BusinessProcessActivityInstance();
      instance.setId(row.path("id").asLong());
      instance.setActivityDefinition(activity);
      instance.setSourceReference(row.path("source_reference").asText(reference));
      instance.setOccurrenceNumber(row.path("occurrence_number").asInt());
      instance.setStatus(row.path("status").asText());
      instance.setObjectiveAchieved(row.path("objective_achieved").asBoolean());
      instance.setObjectiveEvidenceJson(row.path("objective_evidence_json").asText());
      instance.setExitedAt(instant(row.path("exited_at").asText()));
      persisted.add(instance);
    }
    List<AgentTaskFunctionalSnapshot> creativeTasks = new ArrayList<>();
    for (var row : exported.path("tasks")) {
      if (row.path("process_definition_id").asLong() != child.getId()) continue;
      String code = row.path("process_activity_id").asText();
      String agent =
          switch (code) {
            case "nonAudiovisual" -> "communication-director";
            case "customer" -> "customer-agent";
            case "commercial" -> "meta-ad-approver";
            default -> "";
          };
      creativeTasks.add(
          new AgentTaskFunctionalSnapshot(
              row.path("id").asLong(),
              child.getId(),
              child.getProcessCode(),
              code,
              row.path("agent_key").asText(agent),
              row.path("status").asText(),
              instant(row.path("created_at").asText()),
              instant(row.path("delivered_at").asText()),
              row.path("result_json").asText()));
    }
    var context = mock(IrisLearningCycleContext.class);
    when(context.resolve(reference)).thenReturn(Optional.of(json.convertValue(input, Map.class)));
    var tasks = mock(AgentTaskRepository.class);
    when(tasks.findFunctionalSnapshotsByProcessSince(child.getId(), reference, null))
        .thenReturn(creativeTasks);
    JsonNode communicationArtifact = null;
    for (var value : input.path("communicationArtifacts"))
      if ("communicationContract".equals(value.path("activityId").asText())) {
        communicationArtifact = value;
        break;
      }
    long communicationTaskId =
        exported
            .path("communicationTaskId")
            .asLong(
                communicationArtifact == null ? 0 : communicationArtifact.path("taskId").asLong());
    if (communicationTaskId <= 0) throw new IllegalStateException("Comunicação ausente.");
    JsonNode communicationRow = null;
    for (var value : exported.path("tasks"))
      if (value.path("id").asLong() == communicationTaskId) {
        communicationRow = value;
        break;
      }
    if (communicationRow == null) throw new IllegalStateException("Tarefa de comunicação ausente.");
    var communicationTask = new AgentTask();
    communicationTask.setId(communicationTaskId);
    communicationTask.setAssignedAgent(
        Agent.builder()
            .id(990401L)
            .agentKey(communicationRow.path("agent_key").asText("communication-director"))
            .build());
    communicationTask.setProcessDefinition(parent);
    communicationTask.setProcessActivityId("communicationContract");
    communicationTask.setSourceReference(reference);
    communicationTask.setStatus(communicationRow.path("status").asText());
    communicationTask.setResultJson(communicationRow.path("result_json").asText());
    communicationTask.setActivityInstance(
        persisted.stream()
            .filter(
                value ->
                    "communicationContract".equals(value.getActivityDefinition().getActivityId()))
            .findFirst()
            .orElseThrow());
    when(tasks.findById(communicationTaskId)).thenReturn(Optional.of(communicationTask));
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                anyLong(), eq(reference)))
        .thenAnswer(
            i ->
                persisted.stream()
                    .filter(
                        p ->
                            p.getActivityDefinition()
                                .getProcessDefinition()
                                .getId()
                                .equals(i.getArgument(0)))
                    .toList());
    org.mockito.stubbing.Answer<Optional<BusinessProcessActivityInstance>> latestOccurrence =
        i ->
            persisted.stream()
                .filter(p -> p.getActivityDefinition().getId().equals(i.getArgument(0)))
                .max(Comparator.comparing(BusinessProcessActivityInstance::getOccurrenceNumber));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq(reference)))
        .thenAnswer(latestOccurrence);
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq(reference)))
        .thenAnswer(latestOccurrence);
    when(instances.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var value = (BusinessProcessActivityInstance) i.getArgument(0);
              value.setId(990000L + persisted.size());
              persisted.add(value);
              return value;
            });
    var cycles = mock(LearningSalesCycleRepository.class);
    if (exported.has("cycle")) context = projectedContext(exported, tasks, instances, cycles, json);
    var journey =
        new PrivateCommunicationJourney(
            context,
            cycles,
            new ProductProcessActivityPredecessorService(tasks, instances, json),
            instances,
            tasks,
            new PrivateCommunicationCreativeProof(tasks, instances, json),
            json);
    for (String activityId : List.of("destination", "integration")) {
      var activity =
          definitions.values().stream()
              .filter(a -> activityId.equals(a.getActivityId()))
              .findFirst()
              .orElseThrow();
      var readiness = journey.readiness(parent, activity, product, reference);
      assertThat(readiness.ready()).as(readiness.reason()).isTrue();
      assertThat(journey.complete(parent, activity, product, reference).objectiveAchieved())
          .isTrue();
      assertThat(journey.stale(parent, activity, product, reference)).isFalse();
    }
    var finalProof = json.readTree(persisted.getLast().getObjectiveEvidenceJson());
    assertThat(finalProof.path("creativeApproval").path("producerTaskId").asLong())
        .isEqualTo(exported.path("expectedProducerTaskId").asLong(406L));
    assertThat(finalProof.path("checkoutMode").asText()).isEqualTo("SIMULATED");
    verify(tasks, never()).save(any());
  }

  /**
   * Recompõe o produtor com SELECTs exportados, preservando identidades e bytes sem chamar APIs.
   */
  private IrisLearningCycleContext projectedContext(
      JsonNode exported,
      AgentTaskRepository tasks,
      BusinessProcessActivityInstanceRepository instances,
      LearningSalesCycleRepository cycles,
      ObjectMapper json)
      throws Exception {
    var row = exported.path("cycle");
    var cycle = new LearningSalesCycle();
    cycle.setId(row.path("id").asLong());
    cycle.setProductId(row.path("product_id").asLong());
    cycle.setExperimentId(row.path("experiment_id").asLong());
    cycle.setChainDefinitionId(row.path("chain_definition_id").asLong());
    cycle.setProductVersion(row.path("product_version").asText());
    cycle.setStage(row.path("stage").asText());
    cycle.setStatus(row.path("status").asText());
    cycle.setCreatedAt(instant(row.path("created_at").asText()));
    cycle.setBriefJson(exported.path("context").path("cycleBrief").toString());
    cycle.setInheritedLearningJson(exported.path("context").path("inheritedLearning").toString());
    var target =
        json.treeToValue(exported.path("constructionTarget"), AgentTaskTargetResponse.class);
    String reference = target.sourceReference();
    when(cycles.findByExperimentId(cycle.getExperimentId())).thenReturn(Optional.of(cycle));
    var experiment = new Experiment();
    experiment.setId(cycle.getExperimentId());
    experiment.setStatus(ExperimentStatus.PLANNED);
    var experiments = mock(ExperimentRepository.class);
    when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    var constructionProcess = new BusinessProcessDefinition();
    constructionProcess.setId(exported.path("constructionProcessDefinition").path("id").asLong());
    constructionProcess.setProcessCode("pde-construction-approval");
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(constructionProcess);
    chain.setItems(List.of(item));
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    when(chains.findById(cycle.getChainDefinitionId())).thenReturn(Optional.of(chain));
    var construction = mock(LearningCycleConstructionContext.class);
    when(construction.resolve(reference, experiment, constructionProcess.getProcessCode()))
        .thenReturn(Optional.of(target));
    var gateDefinition = new BusinessProcessActivityDefinition();
    gateDefinition.setActivityId("agentValidationGate");
    gateDefinition.setProcessDefinition(constructionProcess);
    var gate = new BusinessProcessActivityInstance();
    gate.setId(exported.path("gateInstance").path("id").asLong());
    gate.setActivityDefinition(gateDefinition);
    gate.setOccurrenceNumber(exported.path("gateInstance").path("occurrence_number").asInt());
    gate.setStatus(exported.path("gateInstance").path("status").asText());
    gate.setObjectiveAchieved(exported.path("gateInstance").path("objective_achieved").asBoolean());
    gate.setObjectiveEvidenceJson(
        exported.path("gateInstance").path("objective_evidence_json").asText());
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                constructionProcess.getId(), reference))
        .thenReturn(List.of(gate));
    List<PdeValidationTaskSnapshot> reviews = new ArrayList<>();
    List<AgentTaskFunctionalSnapshot> artifacts = new ArrayList<>();
    for (var task : exported.path("tasks")) {
      long processId = task.path("process_definition_id").asLong();
      if (processId == constructionProcess.getId()) {
        reviews.add(
            new PdeValidationTaskSnapshot(
                task.path("id").asLong(),
                processId,
                task.path("process_activity_id").asText(),
                task.path("status").asText(),
                task.path("blocker_category").asText(null),
                task.path("blocker_action").asText(null),
                task.path("result_json").asText(),
                task.path("execution_error").asText(null)));
      } else if (processId == exported.path("parentProcessDefinition").path("id").asLong()
          || processId == exported.path("childProcessDefinition").path("id").asLong()) {
        String processCode =
            processId == exported.path("parentProcessDefinition").path("id").asLong()
                ? "pde-communication-sales-journey"
                : "creative-production-approval";
        artifacts.add(
            new AgentTaskFunctionalSnapshot(
                task.path("id").asLong(),
                processId,
                processCode,
                task.path("process_activity_id").asText(),
                task.path("agent_key").asText(),
                task.path("status").asText(),
                instant(task.path("created_at").asText()),
                instant(task.path("delivered_at").asText()),
                task.path("result_json").asText()));
      }
    }
    when(tasks.findPdeValidationTaskSnapshots(reference, constructionProcess.getProcessCode()))
        .thenReturn(reviews);
    when(tasks.findFunctionalSnapshots(eq(reference), anyCollection(), eq(cycle.getCreatedAt())))
        .thenReturn(artifacts);
    return new IrisLearningCycleContext(
        cycles, experiments, chains, construction, instances, tasks, json);
  }

  /** Converte o formato UTC retornado pelo MCP sem inferir horário local. */
  private Instant instant(String value) {
    if (value == null || value.equals("null") || value.isBlank()) return null;
    String normalized = value.replace(' ', 'T');
    return Instant.parse(normalized.endsWith("Z") ? normalized : normalized + "Z");
  }
}
