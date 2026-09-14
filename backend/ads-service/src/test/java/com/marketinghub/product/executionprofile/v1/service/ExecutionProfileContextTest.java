package com.marketinghub.product.executionprofile.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.planning.CommercialPlanVersion;
import com.marketinghub.product.executionprofile.v1.*;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.ProfileContract.Capability;
import com.marketinghub.repository.jpa.planning.CommercialPlanVersionRepository;
import com.marketinghub.repository.jpa.productexecution.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Responsabilidade: preservar identidade, gates financeiros e leitura do aprendizado na ficha. */
class ExecutionProfileContextTest {
  /**
   * Mantém gates nos subprocessos e permite conciliar perdas mesmo quando a operação está
   * bloqueada.
   */
  @Test
  void gatesSubprocessesWithoutBlockingLearning() {
    var context = new ExecutionProfileContext(null, null, null, null, new ObjectMapper());
    assertThat(context.checkpoint("creative-production-approval", "communication"))
        .isEqualTo("DELIVERY_DESIGN");
    assertThat(context.checkpoint("experiment-homologation-activation", "approval"))
        .isEqualTo("HOMOLOGATION");
    assertThat(context.checkpoint("pde-sales-delivery-learning", "optimization"))
        .isEqualTo("OPERATION");
    assertThat(context.checkpoint("pde-sales-delivery-learning", "consolidate")).isNull();
    assertThat(context.checkpoint("pde-sales-delivery-learning", "learningCycle")).isNull();
  }

  /** Uma decisão posterior revoga o checkpoint e outra cadeia ou ciclo não pode usar o vínculo. */
  @Test
  void preservesScopeAndLatestFinancialDecision() throws Exception {
    var profiles = mock(ExecutionProfileRepository.class);
    var bindings = mock(ExecutionProfileBindingRepository.class);
    var reviews = mock(ExecutionProfileReviewRepository.class);
    var plans = mock(CommercialPlanVersionRepository.class);
    var json = new ObjectMapper();
    var profile = new ExecutionProfile();
    profile.setId(1L);
    profile.setProductId(7L);
    profile.setChainId(14L);
    profile.setCommercialPlanId(8L);
    profile.setCommercialPlanVersion(1);
    profile.setContractJson(
        json.writeValueAsString(
            ExecutionProfileRulesTest.contract(Capability.PERSONALIZED_IMAGES)));
    var binding = new ExecutionProfileBinding();
    binding.setId(2L);
    binding.setProductId(7L);
    binding.setProfileId(1L);
    binding.setLearningCycleId(3L);
    when(bindings.findByProductIdAndSourceReference(7L, "experiment:9"))
        .thenReturn(Optional.of(binding));
    when(profiles.findByIdAndProductId(1L, 7L)).thenReturn(Optional.of(profile));
    var plan = new CommercialPlanVersion();
    plan.setVersionNumber(1);
    when(plans.findTopByPlanIdOrderByVersionNumberDesc(8L)).thenReturn(Optional.of(plan));
    var approved = new ExecutionProfileReview();
    approved.setCheckpoint("OFFER");
    approved.setApproved(true);
    var denied = new ExecutionProfileReview();
    denied.setCheckpoint("OFFER");
    denied.setApproved(false);
    when(reviews.findByProfileIdOrderByIdAsc(1L)).thenReturn(List.of(approved));
    var context = new ExecutionProfileContext(profiles, bindings, reviews, plans, json);
    assertThat(context.financialBlocker(profile, "OFFER")).isNull();
    context.requireScope(7L, "experiment:9", 14L, 3L);
    profile.setCompositionJson(
        "[{\"id\":66,\"code\":\"pde-construction-approval\",\"version\":1,\"name\":\"Construção\",\"parentCode\":null}]");
    var process = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    process.setId(66L);
    var policy =
        new ExecutionProfileActivityPolicy(
            context,
            mock(
                com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository
                    .class));
    policy.requireReference(7L, "experiment:9", process, 14L, 3L);
    assertThatThrownBy(() -> policy.requireReference(7L, "experiment:9", process, 14L, null))
        .hasMessageContaining("outra cadeia ou ciclo");
    process.setId(99L);
    assertThatThrownBy(() -> policy.requireReference(7L, "experiment:9", process, 14L, 3L))
        .hasMessageContaining("não pertence à ficha");
    assertThatThrownBy(() -> context.requireScope(7L, "experiment:9", 15L, 3L))
        .hasMessageContaining("outra cadeia ou ciclo");
    assertThatThrownBy(() -> context.requireScope(7L, "experiment:9", 14L, 4L))
        .hasMessageContaining("outra cadeia ou ciclo");
    when(reviews.findByProfileIdOrderByIdAsc(1L)).thenReturn(List.of(approved, denied));
    assertThat(context.financialBlocker(profile, "OFFER")).contains("pendente ou reprovado");
    plan.setVersionNumber(2);
    assertThat(context.financialBlocker(profile, "OFFER")).contains("plano comercial mudou");
  }
}
