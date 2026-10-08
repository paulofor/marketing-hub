package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleExecutionContext;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: consumir no gate real as provas produzidas pelos executores na matriz local.
 */
@EnabledIfEnvironmentVariable(named = "VEGA_GATE_FLOW_ARTIFACTS", matches = ".+")
class VegaCycleGateFlowIntegrationTest {
  /**
   * Liga técnica, três cenários e Têmis locais ao gate; somente cadastros e persistência BPM são
   * doubles.
   */
  @Test
  void approvesExactOutputsFromLocalWorkers() throws Exception {
    Path flow = Path.of(System.getenv("VEGA_GATE_FLOW_ARTIFACTS"));
    var json = new ObjectMapper();
    var technical = json.readTree(Files.readString(flow.resolve("TECHNICAL.json")));
    long cycleId = technical.path("cycleId").asLong(91002L);
    String reference = technical.path("sourceReference").asText();
    long experimentId = Long.parseLong(reference.substring("experiment:".length()));
    var product =
        Product.builder()
            .id(91004L)
            .slug("metodo-musa-7-dias")
            .commercialStatus("EXPERIMENTING")
            .automaticExecutionEnabled(true)
            .validationDefinitionVersion("v1")
            .build();
    var process = new BusinessProcessDefinition();
    process.setId(91070L);
    process.setProcessCode("pde-construction-approval");
    process.setVersionNumber(8);
    var activity = new BusinessProcessActivityDefinition();
    activity.setId(910711L);
    activity.setActivityId("agentValidationGate");
    activity.setProcessDefinition(process);
    var cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(91004L);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion(technical.path("prototypeVersion").asText());
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    var cycleRepo = mock(LearningSalesCycleRepository.class);
    when(cycleRepo.findByExperimentId(experimentId)).thenReturn(Optional.of(cycle));
    var cycleExecutions = mock(LearningCycleExecutionContext.class);
    when(cycleExecutions.source(cycleId, product, process, true)).thenReturn(reference);
    var context = json.createObjectNode();
    var plan =
        (ObjectNode)
            json.readTree(
                getClass().getResourceAsStream("/contracts/pde-agent-validation-plan-v1.json"));
    plan.put("sourceReference", reference);
    context.set("agentValidationPlan", plan);
    context
        .putObject("lineage")
        .put("learningCycleId", cycleId)
        .put("productId", 91004)
        .put("experimentId", experimentId);
    context
        .putObject("privatePrototypeAcceptance")
        .put("status", "READY")
        .put("prototypeVersion", cycle.getProductVersion())
        .put("privateAccessUrl", technical.path("publicUrl").asText());
    var targets = mock(AgentTaskTargetContextProvider.class);
    when(targets.resolve(reference, process.getProcessCode()))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    reference,
                    experimentId,
                    91004L,
                    product.getSlug(),
                    "Vega fixture",
                    "Vega fixture",
                    cycle.getProductVersion(),
                    technical.path("publicUrl").asText(),
                    null,
                    null,
                    null,
                    null,
                    context)));
    var tasks = new ArrayList<AgentTask>();
    for (String key : List.of("TECHNICAL", "ADHERENT", "RECOVERY", "SAFETY", "TEMIS")) {
      var task = new AgentTask();
      task.setId(910388L + tasks.size());
      task.setProcessDefinition(process);
      task.setSourceReference(reference);
      task.setProcessActivityId(
          Map.of(
                  "TECHNICAL",
                  "technicalHomologation",
                  "ADHERENT",
                  "psiqueAdherent",
                  "RECOVERY",
                  "psiqueRecovery",
                  "SAFETY",
                  "psiqueSafety",
                  "TEMIS",
                  "commercialIntegrityReview")
              .get(key));
      task.setAssignedAgent(
          Agent.builder()
              .agentKey("TEMIS".equals(key) ? "meta-ad-approver" : "customer-agent")
              .build());
      task.setExecutionMode("TECHNICAL".equals(key) ? "DETERMINISTIC" : "MODEL");
      task.setExecutionModelCode(
          "TECHNICAL".equals(key) ? "pde-agent-validation-harness-v1" : "test-double");
      task.setStatus("COMPLETED");
      task.setResultJson(Files.readString(flow.resolve(key + ".json")));
      Instant at = Files.getLastModifiedTime(flow.resolve(key + ".json")).toInstant();
      task.setCreatedAt(at);
      task.setDeliveredAt(at);
      tasks.add(task);
    }
    var repository = mock(AgentTaskRepository.class);
    when(repository.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            91070L, reference))
        .thenReturn(tasks);
    var instances = mock(BusinessProcessActivityInstanceRepository.class);
    var products = mock(ProductRepository.class);
    var gate =
        new PdeAgentValidationGateActivityExecutor(
            repository, instances, products, mock(ProductProcessPeriodService.class), json);
    ReflectionTestUtils.setField(
        gate,
        "cycleContracts",
        new PdeAgentValidationCycleContract(cycleRepo, cycleExecutions, targets));
    var readiness = gate.readiness(process, activity, product, reference);
    assertThat(readiness.ready()).as(readiness.reason()).isTrue();
    assertThat(gate.execute(process, activity, product, reference).objectiveAchieved()).isTrue();
    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    verify(products, never()).save(any());
    Files.writeString(flow.resolve("GATE.json"), saved.getValue().getObjectiveEvidenceJson());
    // Uma prova alheia deve bloquear a mesma cadeia completa antes de qualquer nova gravação.
    tasks
        .getLast()
        .setResultJson(
            tasks.getLast().getResultJson().replace(reference, "experiment:" + (experimentId - 1)));
    assertThat(gate.readiness(process, activity, product, reference).ready()).isFalse();
  }
}
