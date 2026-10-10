package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleImplementedInputContext;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: prevenir repetição de Dédalo sem corrigir a estratégia inconsistente. */
class PdeImplementedInputPlanningReadinessProviderTest {
  private final ObjectMapper json = new ObjectMapper();
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final LearningCycleImplementedInputContext inputs =
      new LearningCycleImplementedInputContext(products, json);
  private final PdeImplementedInputPlanningReadinessProvider gate =
      new PdeImplementedInputPlanningReadinessProvider(cycles, inputs, tasks, json);
  private final List<AgentTaskFunctionalSnapshot> history = new ArrayList<>();
  private final BusinessProcessDefinition process = process();

  /** Reproduz a falha original e refaz somente os pareceres dependentes, com outra identidade. */
  @ParameterizedTest
  @CsvSource({
    "91004,metodo-musa-7-dias,musa-pde-entry-v14-primeiro-ajuste-aplicavel",
    "92010,pde-planejado-36,mira-private-candidate-v3"
  })
  void revalidatesOriginBeforeEconomicsAndArchitecture(long productId, String slug, String version)
      throws Exception {
    var product = setup(productId, slug, version);
    var cycle = cycles.findByExperimentId(productId + 200L).orElseThrow();
    var reference = "experiment:" + cycle.getExperimentId();
    history.add(
        snapshot(97001L, "marketStrategy", "COMPLETED", strategy("Quatro escolhas antigas.")));
    history.add(snapshot(97002L, "economics", "COMPLETED", "{\"decision\":\"APPROVE\"}"));
    history.add(snapshot(97003L, "productArchitecture", "BLOCKED", "{\"decision\":\"ADJUST\"}"));
    assertThat(gate.requiresFreshExecution(process, activity("marketStrategy"), product, reference))
        .isTrue();
    assertThat(gate.requiresFreshExecution(process, activity("economics"), product, reference))
        .isTrue();
    assertThat(gate.readiness(process, activity("economics"), product, reference).ready())
        .isFalse();
    assertThat(gate.readiness(process, activity("productArchitecture"), product, reference).ready())
        .isFalse();
    assertThat(
            gate.requiresFreshExecution(
                process, activity("productArchitecture"), product, reference))
        .isTrue();

    history.add(snapshot(97004L, "marketStrategy", "PENDING", null));
    assertThat(gate.requiresFreshExecution(process, activity("marketStrategy"), product, reference))
        .isFalse();
    history.set(
        history.size() - 1,
        snapshot(
            97004L,
            "marketStrategy",
            "COMPLETED",
            strategy(inputs.resolve(cycle).orElseThrow().path("minimumCustomerInput").asText())));
    assertThat(gate.requiresFreshExecution(process, activity("marketStrategy"), product, reference))
        .isFalse();
    assertThat(gate.readiness(process, activity("economics"), product, reference).ready()).isTrue();
    assertThat(gate.requiresFreshExecution(process, activity("economics"), product, reference))
        .isTrue();
    assertThat(gate.readiness(process, activity("productArchitecture"), product, reference).ready())
        .isFalse();

    history.add(snapshot(97005L, "economics", "IN_PROGRESS", null));
    assertThat(gate.requiresFreshExecution(process, activity("economics"), product, reference))
        .isFalse();
    history.set(
        history.size() - 1,
        snapshot(97005L, "economics", "COMPLETED", "{\"decision\":\"APPROVE\"}"));
    assertThat(gate.readiness(process, activity("productArchitecture"), product, reference).ready())
        .isTrue();
    history.add(snapshot(97006L, "productArchitecture", "COMPLETED", "{\"decision\":\"APPROVE\"}"));
    for (String activity : List.of("marketStrategy", "economics", "productArchitecture")) {
      assertThat(gate.requiresFreshExecution(process, activity(activity), product, reference))
          .isFalse();
    }
    assertThat(history.getFirst().resultJson()).contains("Quatro escolhas antigas");
    assertThat(history.get(2).status()).isEqualTo("BLOCKED");
    verify(tasks, atLeastOnce())
        .findFunctionalSnapshotsByProcessSince(process.getId(), reference, cycle.getCreatedAt());
  }

  /** Não transforma rejeição atual em retentativa automática ou novo consumo sem causa alterada. */
  @Test
  void preservesBlockedCurrentStrategyAndArchitecture() throws Exception {
    var product =
        setup(95004L, "metodo-musa-7-dias", "musa-pde-entry-v14-primeiro-ajuste-aplicavel");
    var cycle = cycles.findByExperimentId(95204L).orElseThrow();
    String reference = "experiment:95204";
    history.add(snapshot(99001L, "marketStrategy", "BLOCKED", "{\"decision\":\"ADJUST\"}"));
    assertThat(gate.requiresFreshExecution(process, activity("marketStrategy"), product, reference))
        .isFalse();
    history.set(
        0,
        snapshot(
            99001L,
            "marketStrategy",
            "COMPLETED",
            strategy(inputs.resolve(cycle).orElseThrow().path("minimumCustomerInput").asText())));
    history.add(snapshot(99002L, "economics", "COMPLETED", "{\"decision\":\"APPROVE\"}"));
    history.add(snapshot(99003L, "productArchitecture", "BLOCKED", "{\"decision\":\"ADJUST\"}"));
    assertThat(
            gate.requiresFreshExecution(
                process, activity("productArchitecture"), product, reference))
        .isFalse();
    assertThat(gate.readiness(process, activity("productArchitecture"), product, reference).ready())
        .isTrue();
  }

  /** Preserva prova válida, histórico encerrado, outra identidade e ausência de implementação. */
  @Test
  void preservesCompatiblePathAndNeverReopensClosedOrDifferentCycle() throws Exception {
    var product =
        setup(93004L, "metodo-musa-7-dias", "musa-pde-entry-v12-primeiro-ajuste-aplicavel");
    var cycle = cycles.findByExperimentId(93204L).orElseThrow();
    String reference = "experiment:93204";
    history.add(
        snapshot(
            98001L,
            "marketStrategy",
            "COMPLETED",
            strategy(inputs.resolve(cycle).orElseThrow().path("minimumCustomerInput").asText())));
    history.add(snapshot(98002L, "economics", "COMPLETED", "{\"decision\":\"APPROVE\"}"));
    history.add(snapshot(98003L, "productArchitecture", "COMPLETED", "{\"decision\":\"APPROVE\"}"));
    for (String activity : List.of("marketStrategy", "economics", "productArchitecture"))
      assertThat(gate.requiresFreshExecution(process, activity(activity), product, reference))
          .isFalse();
    cycle.setStatus("CLOSED");
    history.set(
        0,
        snapshot(
            98001L, "marketStrategy", "COMPLETED", strategy("Descrição histórica preservada.")));
    assertThat(gate.requiresFreshExecution(process, activity("marketStrategy"), product, reference))
        .isFalse();
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    assertThat(gate.requiresFreshExecution(process, activity("marketStrategy"), product, reference))
        .isFalse();
    cycle.setStage("PLANNING");
    assertThat(
            gate.requiresFreshExecution(
                process,
                activity("marketStrategy"),
                Product.builder().id(94004L).build(),
                reference))
        .isFalse();
    cycle.setProductVersion("unsupported-v999");
    assertThat(gate.requiresFreshExecution(process, activity("marketStrategy"), product, reference))
        .isFalse();
    assertThat(
            gate.requiresFreshExecution(
                process, activity("marketStrategy"), product, "product:93004@legacy"))
        .isFalse();
  }

  /** Prepara catálogo e histórico sintéticos, sem criar tarefa paga ou alterar o banco. */
  private Product setup(Long productId, String slug, String version) {
    var product = Product.builder().id(productId).slug(slug).build();
    var cycle = new LearningSalesCycle();
    cycle.setId(productId + 100L);
    cycle.setProductId(productId);
    cycle.setExperimentId(productId + 200L);
    cycle.setProductVersion(version);
    cycle.setStage("PLANNING");
    cycle.setStatus("OPEN");
    cycle.setCreatedAt(Instant.parse("2026-10-10T00:00:00Z"));
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(cycles.findByExperimentId(cycle.getExperimentId())).thenReturn(Optional.of(cycle));
    when(tasks.findFunctionalSnapshotsByProcessSince(
            process.getId(), "experiment:" + cycle.getExperimentId(), cycle.getCreatedAt()))
        .thenAnswer(ignored -> List.copyOf(history));
    return product;
  }

  /** Transporta uma resposta de estratégia simulada no mesmo contrato validado pelo executor. */
  private String strategy(String minimum) throws Exception {
    var value = json.createObjectNode().put("decision", "APPROVE");
    value
        .putObject("marketStrategicContract")
        .putObject("agentValidationPlan")
        .putObject("customerValueDelivery")
        .put("minimumCustomerInput", minimum);
    return value.toString();
  }

  /** Cria prova funcional compacta como a consulta oficial que filtra definição e referência. */
  private AgentTaskFunctionalSnapshot snapshot(
      Long id, String activity, String status, String result) {
    return new AgentTaskFunctionalSnapshot(
        id,
        process.getId(),
        process.getProcessCode(),
        activity,
        "fixture-agent",
        status,
        Instant.parse("2026-10-10T01:00:00Z"),
        "COMPLETED".equals(status) ? Instant.parse("2026-10-10T01:01:00Z") : null,
        result);
  }

  /** Prepara a definição publicada que usa a sequência canônica de três agentes. */
  private BusinessProcessDefinition process() {
    var value = new BusinessProcessDefinition();
    value.setId(91116L);
    value.setProcessCode("pde-commercial-plan-offer");
    value.setVersionNumber(12);
    return value;
  }

  /** Identifica a atividade a conferir sem alterar o grafo ou o responsável existente. */
  private BusinessProcessActivityDefinition activity(String code) {
    var value = new BusinessProcessActivityDefinition();
    value.setActivityId(code);
    return value;
  }
}
