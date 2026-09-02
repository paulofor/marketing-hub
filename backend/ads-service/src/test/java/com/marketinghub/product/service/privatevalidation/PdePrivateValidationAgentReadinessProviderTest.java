package com.marketinghub.product.service.privatevalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar que agentes não saltam etapas na validação privada. */
class PdePrivateValidationAgentReadinessProviderTest {
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final PdePrivateValidationAgentReadinessProvider provider =
      new PdePrivateValidationAgentReadinessProvider(predecessors);

  /** Bloqueia Psique enquanto a segunda leitura ainda não atingiu o objetivo. */
  @Test
  void blocksAgentBeforePrivateReadings() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process, "humanExperienceReview");
    when(predecessors.readiness(process, activity, "product:9@private-validation-v1"))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                false, "Conclua primeiro a segunda leitura privada."));

    var readiness =
        provider.readiness(
            process, activity, Product.builder().id(9L).build(), "product:9@private-validation-v1");

    assertThat(provider.supports(process, activity)).isTrue();
    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("segunda leitura");
  }

  /** Monta o processo privado publicado usado pelo gate. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(66L);
    process.setProcessCode("pde-construction-approval");
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Monta uma atividade de agente do processo privado. */
  private BusinessProcessActivityDefinition activity(
      BusinessProcessDefinition process, String activityId) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(607L);
    activity.setProcessDefinition(process);
    activity.setActivityId(activityId);
    activity.setOwnerName("Psique");
    return activity;
  }
}
