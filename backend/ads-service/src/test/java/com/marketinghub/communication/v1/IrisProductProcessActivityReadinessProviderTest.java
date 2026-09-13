package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agenttask.CommunicationMaterializationContextProvider;
import com.marketinghub.agenttask.MarketStrategicContextProvider;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar que a tela e Íris compartilham o mesmo gate de entrada. */
class IrisProductProcessActivityReadinessProviderTest {

  /** Libera a atividade somente com estratégia e contexto funcional completos. */
  @Test
  void shouldAllowIrisWhenAllContractsAreReady() {
    MarketStrategicContextProvider strategy = mock(MarketStrategicContextProvider.class);
    CommunicationMaterializationContextProvider communication =
        mock(CommunicationMaterializationContextProvider.class);
    when(strategy.resolve("experiment:89"))
        .thenReturn(
            Optional.of(
                Map.of(
                    "availability",
                    "AVAILABLE",
                    "contractVersion",
                    "MARKET_STRATEGY_V2",
                    "contentHash",
                    "abc123")));
    when(communication.resolve("experiment:89"))
        .thenReturn(Optional.of(Map.of("availability", "AVAILABLE", "inputReadiness", "READY")));
    IrisProductProcessActivityReadinessProvider provider =
        new IrisProductProcessActivityReadinessProvider(strategy, communication);

    var readiness = provider.readiness(process(), activity(), null, "experiment:89");

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason()).contains("prontos para Íris");
  }

  /** Expõe o predecessor exato antes de criar uma tarefa que o worker bloquearia. */
  @Test
  void shouldBlockIrisWithExactMissingPredecessor() {
    MarketStrategicContextProvider strategy = mock(MarketStrategicContextProvider.class);
    CommunicationMaterializationContextProvider communication =
        mock(CommunicationMaterializationContextProvider.class);
    when(strategy.resolve("experiment:89"))
        .thenReturn(
            Optional.of(
                Map.of(
                    "availability",
                    "AVAILABLE",
                    "contractVersion",
                    "MARKET_STRATEGY_V2",
                    "contentHash",
                    "abc123")));
    when(communication.resolve("experiment:89"))
        .thenReturn(
            Optional.of(
                Map.of(
                    "availability",
                    "AVAILABLE",
                    "inputReadiness",
                    "BLOCKED",
                    "missingRequiredPredecessors",
                    List.of("Parecer econômico concluído de Plutus"))));
    IrisProductProcessActivityReadinessProvider provider =
        new IrisProductProcessActivityReadinessProvider(strategy, communication);

    var readiness = provider.readiness(process(), activity(), null, "experiment:89");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("Plutus").doesNotContain("Dédalo");
  }

  /** Reconhece a comunicação e a landing, sem assumir a produção de criativos. */
  @Test
  void shouldSupportOnlyIrisCommunicationContract() {
    IrisProductProcessActivityReadinessProvider provider =
        new IrisProductProcessActivityReadinessProvider(
            MarketStrategicContextProvider.empty(),
            CommunicationMaterializationContextProvider.empty());

    assertThat(provider.supports(process(), activity())).isTrue();
    BusinessProcessActivityDefinition anotherActivity = activity();
    anotherActivity.setActivityId("creatives");
    assertThat(provider.supports(process(), anotherActivity)).isFalse();
  }

  /** Explica o destino privado antes de criar uma tarefa de landing que não pertence ao ciclo. */
  @Test
  void blocksSeparateLandingForApprovedPrivateDestination() {
    var communication = mock(CommunicationMaterializationContextProvider.class);
    when(communication.resolve("experiment:92"))
        .thenReturn(
            Optional.of(
                Map.of(
                    "mode",
                    IrisLearningCycleContext.MODE,
                    "availability",
                    "AVAILABLE",
                    "inputReadiness",
                    "READY")));
    var provider =
        new IrisProductProcessActivityReadinessProvider(
            MarketStrategicContextProvider.empty(), communication);
    var landing = process();
    landing.setProcessCode("landing-page-generation");
    for (String code : List.of("select", "strategy", "compose", "html")) {
      var activity = activity();
      activity.setActivityId(code);
      assertThat(provider.supports(landing, activity)).isTrue();
      var readiness = provider.readiness(landing, activity, null, "experiment:92");
      assertThat(readiness.ready()).isFalse();
      assertThat(readiness.reason())
          .contains("experiência privada", "Retome o processo de comunicação");
    }
  }

  /** Cria a versão publicada mínima do processo de comunicação. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    return process;
  }

  /** Reabre comunicação superada sem repetir uma tarefa que ainda conserva o gate vigente. */
  @Test
  void refreshesPrivateCommunicationWhenApprovedContextChanges() {
    String reference = "product:900010@agent-validation-v1";
    var communication = mock(CommunicationMaterializationContextProvider.class);
    var repository = mock(com.marketinghub.repository.jpa.agenttask.AgentTaskRepository.class);
    var provider =
        new IrisProductProcessActivityReadinessProvider(
            MarketStrategicContextProvider.empty(), communication);
    org.springframework.test.util.ReflectionTestUtils.setField(provider, "tasks", repository);
    var process = process();
    process.setId(900063L);
    when(repository.findFunctionalSnapshotsByProcessSince(process.getId(), reference, null))
        .thenReturn(
            List.of(
                new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
                    900402L,
                    process.getId(),
                    process.getProcessCode(),
                    "communicationContract",
                    "communication-director",
                    "COMPLETED",
                    null,
                    null,
                    "{}")));
    when(communication.resolve(reference))
        .thenReturn(
            Optional.of(
                Map.of(
                    "inputReadiness",
                    "READY",
                    "communicationArtifacts",
                    List.of(Map.of("taskId", 900402L)))));
    assertThat(provider.requiresFreshExecution(process, activity(), null, reference)).isFalse();
    when(communication.resolve(reference))
        .thenReturn(
            Optional.of(Map.of("inputReadiness", "READY", "communicationArtifacts", List.of())));
    assertThat(provider.requiresFreshExecution(process, activity(), null, reference)).isTrue();
    when(communication.resolve(reference))
        .thenReturn(Optional.of(Map.of("inputReadiness", "BLOCKED")));
    assertThat(provider.requiresFreshExecution(process, activity(), null, reference)).isTrue();
  }

  /** Cria a atividade mínima de materialização do contrato. */
  private BusinessProcessActivityDefinition activity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("communicationContract");
    return activity;
  }
}
