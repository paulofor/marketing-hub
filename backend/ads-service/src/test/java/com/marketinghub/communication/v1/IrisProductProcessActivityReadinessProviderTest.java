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

  /** Reconhece apenas a primeira atividade do processo comercial governado por Íris. */
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

  /** Cria a versão publicada mínima do processo de comunicação. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    return process;
  }

  /** Cria a atividade mínima de materialização do contrato. */
  private BusinessProcessActivityDefinition activity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("communicationContract");
    return activity;
  }
}
