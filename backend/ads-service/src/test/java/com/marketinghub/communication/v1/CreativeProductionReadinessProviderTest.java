package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.service.predecessor.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Responsabilidade: garantir retorno à produção sem reusar revisões de uma imagem anterior. */
class CreativeProductionReadinessProviderTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final CreativeProductionReadinessProvider provider =
      new CreativeProductionReadinessProvider(tasks, predecessors, new ObjectMapper());

  /** Reabre briefing histórico e revisão anterior; parecer atual de ajuste exige nova peça. */
  @Test
  void reopensBriefAndSupersededReviewsWithFunctionalRework() {
    var process = new BusinessProcessDefinition();
    process.setId(91064L);
    process.setProcessCode("creative-production-approval");
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("nonAudiovisual");
    var product = Product.builder().id(91004L).build();
    when(tasks.findFunctionalSnapshotsByProcessSince(91064L, "experiment:91092", null))
        .thenReturn(List.of(snapshot(10, "nonAudiovisual", "COMPLETED", "{}")));
    assertThat(provider.requiresFreshExecution(process, activity, product, "experiment:91092"))
        .isTrue();
    String rendered = "{\"functionalOutput\":{\"staticAssets\":[{}],\"renderedAssets\":[{}]}}";
    when(tasks.findFunctionalSnapshotsByProcessSince(91064L, "experiment:91092", null))
        .thenReturn(
            List.of(
                snapshot(10, "nonAudiovisual", "COMPLETED", rendered),
                snapshot(11, "customer", "BLOCKED", "{\"decision\":\"ADJUST\"}")));
    assertThat(provider.requiresFreshExecution(process, activity, product, "experiment:91092"))
        .isTrue();
    when(tasks.findFunctionalSnapshotsByProcessSince(91064L, "experiment:91092", null))
        .thenReturn(
            List.of(
                snapshot(12, "nonAudiovisual", "COMPLETED", rendered),
                snapshot(11, "customer", "COMPLETED", "{\"decision\":\"APPROVED\"}")));
    assertThat(provider.requiresFreshExecution(process, activity, product, "experiment:91092"))
        .isFalse();
    activity.setActivityId("customer");
    assertThat(provider.requiresFreshExecution(process, activity, product, "experiment:91092"))
        .isTrue();
  }

  /** Falha técnica sem nova prova permanece visível; não vira repetição cega da produção. */
  @Test
  void technicalFailureDoesNotLoopProduction() {
    var process = new BusinessProcessDefinition();
    process.setId(91064L);
    process.setProcessCode("creative-production-approval");
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("nonAudiovisual");
    when(tasks.findFunctionalSnapshotsByProcessSince(91064L, "experiment:91092", null))
        .thenReturn(
            List.of(
                snapshot(
                    10,
                    "nonAudiovisual",
                    "COMPLETED",
                    "{\"functionalOutput\":{\"staticAssets\":[{}],\"renderedAssets\":[{}]}}"),
                snapshot(11, "customer", "BLOCKED", "{\"decision\":\"BLOCKED\"}")));
    assertThat(
            provider.requiresFreshExecution(
                process, activity, Product.builder().id(91004L).build(), "experiment:91092"))
        .isFalse();
  }

  /** Cria somente os dados funcionais necessários à decisão, sem auditoria pesada. */
  private AgentTaskFunctionalSnapshot snapshot(
      long id, String activity, String status, String result) {
    return new AgentTaskFunctionalSnapshot(
        id,
        91064L,
        "creative-production-approval",
        activity,
        "communication-director",
        status,
        null,
        null,
        result);
  }
}
