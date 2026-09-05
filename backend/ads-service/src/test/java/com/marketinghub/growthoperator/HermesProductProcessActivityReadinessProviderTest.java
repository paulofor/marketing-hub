package com.marketinghub.growthoperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.agenttask.MarketStrategicContextProvider;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar que tela e worker aplicam o mesmo gate estratégico de Hermes. */
class HermesProductProcessActivityReadinessProviderTest {

  /** Libera a tentativa somente com o contrato integral aceito pelo executor. */
  @Test
  void shouldAllowHermesWithReadyMarketStrategyV2() {
    MarketStrategicContextProvider strategy = mock(MarketStrategicContextProvider.class);
    when(strategy.resolve("experiment:91")).thenReturn(Optional.of(readyContract()));
    HermesProductProcessActivityReadinessProvider provider =
        new HermesProductProcessActivityReadinessProvider(strategy);

    var readiness = provider.readiness(process(), activity("task-1"), null, "experiment:91");

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason()).contains("pronto para Hermes");
  }

  /** Impede nova tarefa quando o plano possui apenas um parecer histórico sem contrato v2. */
  @Test
  void shouldBlockHermesBeforeCreatingAnotherTaskForLegacyStrategy() {
    MarketStrategicContextProvider strategy = mock(MarketStrategicContextProvider.class);
    when(strategy.resolve("experiment:91"))
        .thenReturn(
            Optional.of(
                Map.of(
                    "availability",
                    "MISSING",
                    "reason",
                    "A última pesquisa de Atena é histórica e não contém contrato v2.")));
    HermesProductProcessActivityReadinessProvider provider =
        new HermesProductProcessActivityReadinessProvider(strategy);

    var readiness = provider.readiness(process(), activity("task-1"), null, "experiment:91");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason())
        .contains("Operação avançada dos especialistas", "novo parecer de Atena", "histórica");
  }

  /** Recusa cada divergência que também faria o worker bloquear antes do modelo. */
  @Test
  void shouldBlockHermesWhenReadyContractHasInvalidIdentityOrBoundary() {
    MarketStrategicContextProvider strategy = mock(MarketStrategicContextProvider.class);
    HermesProductProcessActivityReadinessProvider provider =
        new HermesProductProcessActivityReadinessProvider(strategy);
    Map<String, Object> invalidHash = new LinkedHashMap<>(readyContract());
    invalidHash.put("contentHash", "sha-invalido");
    when(strategy.resolve("experiment:91")).thenReturn(Optional.of(invalidHash));

    assertThat(provider.readiness(process(), activity("task-1"), null, "experiment:91").ready())
        .isFalse();

    Map<String, Object> invalidBoundary = new LinkedHashMap<>(readyContract());
    invalidBoundary.put(
        "contract",
        Map.of(
            "contractVersion",
            "MARKET_STRATEGY_V2",
            "status",
            "READY_FOR_OPERATION",
            "operatorBoundary",
            "STRATEGIST_RECOMMENDS_OPERATOR_EXECUTES"));
    when(strategy.resolve("experiment:91")).thenReturn(Optional.of(invalidBoundary));

    assertThat(provider.readiness(process(), activity("task-1"), null, "experiment:91").ready())
        .isFalse();
  }

  /** Governa todas e somente as atividades atribuídas a Hermes no processo publicado. */
  @Test
  void shouldSupportOnlyHermesActivitiesFromExperimentOptimization() {
    HermesProductProcessActivityReadinessProvider provider =
        new HermesProductProcessActivityReadinessProvider(MarketStrategicContextProvider.empty());

    assertThat(provider.supports(process(), activity("task-1"))).isTrue();
    assertThat(provider.supports(process(), activity("task-2"))).isTrue();
    assertThat(provider.supports(process(), activity("task-3"))).isTrue();
    assertThat(provider.supports(process(), activity("task-4"))).isTrue();
    assertThat(provider.supports(process(), activity("task-10"))).isTrue();
    assertThat(provider.supports(process(), activity("task-5"))).isFalse();

    BusinessProcessDefinition anotherProcess = process();
    anotherProcess.setProcessCode("pde-communication-sales-journey");
    assertThat(provider.supports(anotherProcess, activity("task-1"))).isFalse();
  }

  /** Cria a versão publicada mínima do processo operacional. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("operacao-otimizacao-experimento");
    return process;
  }

  /** Cria uma atividade mínima para avaliar o roteamento do gate. */
  private BusinessProcessActivityDefinition activity(String activityId) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId(activityId);
    return activity;
  }

  /** Monta o contrato estratégico íntegro exigido por Hermes. */
  private Map<String, Object> readyContract() {
    return Map.of(
        "availability",
        "AVAILABLE",
        "contractVersion",
        "MARKET_STRATEGY_V2",
        "contentHash",
        "a".repeat(64),
        "contract",
        Map.of(
            "contractVersion",
            "MARKET_STRATEGY_V2",
            "status",
            "READY_FOR_OPERATION",
            "operatorBoundary",
            "ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH"));
  }
}
