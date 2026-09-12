package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Responsabilidade: reproduzir offline contratos exportados por leitura, sem acesso ao runtime. */
@EnabledIfEnvironmentVariable(named = "PRIVATE_JOURNEY_REPLAY", matches = ".+")
class PrivateCommunicationJourneyReplayTest {
  /**
   * Confere os bytes reais de comunicação, peça, pareceres e decisão antes da aplicação autorizada.
   */
  @Test
  void completesUsingExportedEvidenceWithoutCallingProduction() throws Exception {
    var json = new ObjectMapper();
    var exported = json.readTree(Path.of(System.getenv("PRIVATE_JOURNEY_REPLAY")).toFile());
    var input = exported.path("context");
    String reference = input.path("sourceReference").asText();
    var product = Product.builder().id(input.path("product").path("id").asLong()).build();
    var parent = new BusinessProcessDefinition();
    parent.setId(63L);
    parent.setProcessCode("pde-communication-sales-journey");
    parent.setDiagramJson(
        """
        {"nodes":[{"id":"communicationContract","type":"TASK"},{"id":"creatives","type":"TASK"},
        {"id":"destination","type":"TASK"},{"id":"integration","type":"TASK"}],
        "flows":[{"from":"communicationContract","to":"creatives"},{"from":"creatives","to":"destination"},
        {"from":"destination","to":"integration"}]}
        """);
    var child = new BusinessProcessDefinition();
    child.setId(64L);
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
    List<BusinessProcessActivityInstance> persisted = new ArrayList<>();
    for (var row : exported.path("instances")) {
      var activity = definitions.get(row.path("activity_definition_id").asLong());
      if (activity == null) continue;
      var instance = new BusinessProcessActivityInstance();
      instance.setId(row.path("id").asLong());
      instance.setActivityDefinition(activity);
      instance.setSourceReference(reference);
      instance.setOccurrenceNumber(row.path("occurrence_number").asInt());
      instance.setStatus(row.path("status").asText());
      instance.setObjectiveAchieved(row.path("objective_achieved").asBoolean());
      instance.setObjectiveEvidenceJson(row.path("objective_evidence_json").asText());
      instance.setExitedAt(instant(row.path("exited_at").asText()));
      persisted.add(instance);
    }
    List<AgentTaskFunctionalSnapshot> creativeTasks = new ArrayList<>();
    for (var row : exported.path("tasks")) {
      if (row.path("process_definition_id").asLong() != 64L) continue;
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
              64L,
              child.getProcessCode(),
              code,
              agent,
              row.path("status").asText(),
              instant(row.path("created_at").asText()),
              instant(row.path("delivered_at").asText()),
              row.path("result_json").asText()));
    }
    var context = mock(IrisLearningCycleContext.class);
    when(context.resolve(reference)).thenReturn(Optional.of(json.convertValue(input, Map.class)));
    var tasks = mock(AgentTaskRepository.class);
    when(tasks.findFunctionalSnapshotsByProcessSince(64L, reference, null))
        .thenReturn(creativeTasks);
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
    var journey =
        new PrivateCommunicationJourney(
            context,
            mock(LearningSalesCycleRepository.class),
            new ProductProcessActivityPredecessorService(tasks, instances, json),
            instances,
            tasks,
            new PrivateCommunicationCreativeProof(tasks, instances, json),
            json);
    for (long activity : List.of(639L, 640L)) {
      var readiness = journey.readiness(parent, definitions.get(activity), product, reference);
      assertThat(readiness.ready()).as(readiness.reason()).isTrue();
      assertThat(
              journey
                  .complete(parent, definitions.get(activity), product, reference)
                  .objectiveAchieved())
          .isTrue();
      assertThat(journey.stale(parent, definitions.get(activity), product, reference)).isFalse();
    }
    var finalProof = json.readTree(persisted.getLast().getObjectiveEvidenceJson());
    assertThat(finalProof.path("creativeApproval").path("producerTaskId").asLong()).isEqualTo(406L);
    assertThat(finalProof.path("checkoutMode").asText()).isEqualTo("SIMULATED");
    verify(tasks, never()).save(any());
  }

  /** Converte o formato UTC retornado pelo MCP sem inferir horário local. */
  private Instant instant(String value) {
    if (value == null || value.equals("null") || value.isBlank()) return null;
    String normalized = value.replace(' ', 'T');
    return Instant.parse(normalized.endsWith("Z") ? normalized : normalized + "Z");
  }
}
