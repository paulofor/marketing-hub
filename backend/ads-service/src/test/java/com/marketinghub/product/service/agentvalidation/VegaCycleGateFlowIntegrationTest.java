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
    cycle.setId(91002L);
    cycle.setProductId(91004L);
    cycle.setExperimentId(91092L);
    cycle.setProductVersion(technical.path("prototypeVersion").asText());
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    var cycleRepo = mock(LearningSalesCycleRepository.class);
    when(cycleRepo.findByExperimentId(91092L)).thenReturn(Optional.of(cycle));
    var cycleExecutions = mock(LearningCycleExecutionContext.class);
    when(cycleExecutions.source(91002L, product, process, true)).thenReturn("experiment:91092");
    var context = json.createObjectNode();
    var plan =
        (ObjectNode)
            json.readTree(
                getClass().getResourceAsStream("/contracts/pde-agent-validation-plan-v1.json"));
    plan.put("sourceReference", "experiment:91092");
    context.set("agentValidationPlan", plan);
    context
        .putObject("lineage")
        .put("learningCycleId", 91002)
        .put("productId", 91004)
        .put("experimentId", 91092);
    context
        .putObject("privatePrototypeAcceptance")
        .put("status", "READY")
        .put("prototypeVersion", cycle.getProductVersion())
        .put("privateAccessUrl", technical.path("publicUrl").asText());
    var targets = mock(AgentTaskTargetContextProvider.class);
    when(targets.resolve("experiment:91092", process.getProcessCode()))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    "experiment:91092",
                    91092L,
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
      task.setSourceReference("experiment:91092");
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
            91070L, "experiment:91092"))
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
    var readiness = gate.readiness(process, activity, product, "experiment:91092");
    assertThat(readiness.ready()).as(readiness.reason()).isTrue();
    assertThat(gate.execute(process, activity, product, "experiment:91092").objectiveAchieved())
        .isTrue();
    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    verify(products, never()).save(any());
    Files.writeString(flow.resolve("GATE.json"), saved.getValue().getObjectiveEvidenceJson());
    // Uma prova alheia deve bloquear a mesma cadeia completa antes de qualquer nova gravação.
    tasks
        .getLast()
        .setResultJson(
            tasks.getLast().getResultJson().replace("experiment:91092", "experiment:91091"));
    assertThat(gate.readiness(process, activity, product, "experiment:91092").ready()).isFalse();
  }
}
