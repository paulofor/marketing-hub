package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeTechnicalHomologationReadinessProvider;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: comprovar a passagem da prova corrente à homologação, preservando o histórico.
 */
class LearningCycleHomologationHandoffTest {

  /**
   * Reproduz a falta do registro em Vega e outra identidade: somente a prova da mesma versão
   * entrega URL e linhagem válidas à homologação, preservando as aprovações históricas.
   */
  @ParameterizedTest
  @CsvSource({
    "4, 10, 103, musa-pde-entry-v13-primeiro-ajuste-aplicavel",
    "97004, 97010, 97103, another-private-v17"
  })
  void registeredProofConnectsConstructionToCurrentHomologation(
      long productId, long cycleId, long experimentId, String version) {
    var mapper = new ObjectMapper();
    var cycles = mock(LearningSalesCycleRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var tasks = mock(AgentTaskRepository.class);
    var events = mock(LearningSalesCycleEventRepository.class);
    var resolver = new LearningCycleConstructionContext(cycles, chains, tasks, mapper);
    var product =
        Product.builder()
            .id(productId)
            .slug("handoff-fixture")
            .pdeExperienceJson("{\"experienceVersion\":\"historical-v7\"}")
            .publicUrl("https://historical.invalid")
            .build();
    var cycle = new LearningSalesCycle();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setChainDefinitionId(26L);
    cycle.setPreviousCycleId(1L);
    cycle.setProductVersion(version);
    cycle.setStatus("OPEN");
    cycle.setCreatedAt(Instant.parse("2026-09-09T00:00:00Z"));
    cycle.setInheritedLearningJson(
        "{\"cycleId\":1,\"limitation\":\"Evidência sintética sem venda\"}");
    cycle.setBriefJson("{\"hypothesis\":\"Revalidar a comunicação da mesma versão privada\"}");
    var experiment = Experiment.builder().id(experimentId).product(product).build();
    String reference = "experiment:" + experimentId;
    when(cycles.findByExperimentId(experimentId)).thenReturn(Optional.of(cycle));
    var planning = new BusinessProcessDefinition();
    planning.setId(67L);
    planning.setProcessCode("pde-commercial-plan-offer");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(planning);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(new ArrayList<>(List.of(item)));
    when(chains.findById(26L)).thenReturn(Optional.of(chain));
    when(tasks.findFunctionalSnapshotsByProcessSince(67L, reference, cycle.getCreatedAt()))
        .thenReturn(
            List.of(
                approval(
                    3,
                    "productArchitecture",
                    "landing-generator",
                    "{\"decision\":\"APPROVE\",\"productArchitecture\":{\"privatePrototype\":{\"simpleInput\":\"Ocasião e peça\"}}}"),
                approval(
                    2,
                    "economics",
                    "financial-agent",
                    "{\"contractVersion\":\"PDE_AGENT_ECONOMICS_V1\",\"decision\":\"APPROVE\",\"economics\":{\"commercialSpendAuthorized\":false},\"metrics\":{\"primary\":\"Uso\"}}"),
                approval(
                    1,
                    "marketStrategy",
                    "experiment-strategist",
                    "{\"decision\":\"APPROVE\",\"marketStrategicContract\":{\"contractVersion\":\"MARKET_STRATEGY_V4\",\"status\":\"READY_FOR_AGENT_VALIDATION\",\"agentValidationPlan\":{}}}")));
    var history = new ArrayList<LearningSalesCycleEvent>();
    when(events.findByCycleIdOrderByRevisionAsc(cycleId)).thenAnswer(ignored -> history);
    var prototype = new LearningCyclePrototypeContext(events, mapper);
    ReflectionTestUtils.setField(resolver, "prototypeContext", prototype);
    AgentTaskTargetContextProvider targets =
        source -> resolver.resolve(source, experiment, "pde-construction-approval");
    var readiness = new PdeTechnicalHomologationReadinessProvider(targets);
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-construction-approval");
    process.setVersionNumber(8);
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("technicalHomologation");
    var before = targets.resolve(reference).orElseThrow();
    assertThat(before.pdeContext().path("status").asText()).isEqualTo("PLANNED");
    assertThat(before.publicUrl()).isNull();
    assertThat(readiness.readiness(process, activity, product, reference).ready()).isFalse();
    var predecessor = new LearningSalesCycleEvent();
    predecessor.setAction("REGISTER_PROTOTYPE");
    predecessor.setCreatedAt(Instant.now());
    var historicalEvidence = mapper.createObjectNode().put("productVersion", "historical-v7");
    historicalEvidence
        .putObject("privatePrototype")
        .put("prototypeVersion", "historical-v7")
        .put("privateAccessUrl", "https://historical.invalid/agent-validation");
    predecessor.setEvidenceJson(historicalEvidence.toString());
    history.add(predecessor);
    assertThat(readiness.readiness(process, activity, product, reference).ready()).isFalse();
    var proof = mapper.createObjectNode();
    proof.put("prototypeVersion", version);
    proof.put("privateAccessUrl", "https://private.invalid/agent-validation");
    proof.put("image", "repo/fixture:validated");
    proof.put(
        "evidenceReference",
        "Relatório local sintético: desktop, celular, recuperação e segurança.");
    proof.put("observedAt", Instant.now().toString());
    for (var check :
        List.of(
            "desktopValidated",
            "mobileValidated",
            "firstResultValidated",
            "resumeValidated",
            "failuresValidated",
            "testDataExcluded",
            "noExternalSideEffects")) {
      proof.put(check, true);
    }
    prototype.validate(proof, version);
    var registration = new LearningSalesCycleEvent();
    registration.setCycleId(cycleId);
    registration.setAction("REGISTER_PROTOTYPE");
    registration.setCreatedAt(Instant.now());
    var evidence = mapper.createObjectNode().put("productVersion", version);
    evidence.set("privatePrototype", proof);
    registration.setEvidenceJson(evidence.toString());
    history.add(registration);
    assertThat(readiness.readiness(process, activity, product, reference).ready()).isTrue();
    var target = targets.resolve(reference).orElseThrow();
    assertThat(target.publicUrl()).isEqualTo("https://private.invalid/agent-validation");
    assertThat(target.experienceVersion()).isEqualTo(version);
    assertThat(target.pdeContext().path("lineage").path("learningCycleId").asLong())
        .isEqualTo(cycleId);
    assertThat(target.pdeContext().path("lineage").path("productId").asLong()).isEqualTo(productId);
    assertThat(target.pdeContext().path("lineage").path("experimentId").asLong())
        .isEqualTo(experimentId);
    assertThat(product.getPdeExperienceJson()).contains("historical-v7");
    verify(cycles, never()).save(any());
  }

  /** Cria um parecer sintético concluído, com autoria e ordem temporal válidas. */
  private AgentTaskFunctionalSnapshot approval(
      long id, String activity, String agent, String result) {
    var created = Instant.parse("2026-09-09T00:00:00Z");
    return new AgentTaskFunctionalSnapshot(
        id,
        67L,
        "pde-commercial-plan-offer",
        activity,
        agent,
        "COMPLETED",
        created,
        created.plusSeconds(id),
        result);
  }
}
