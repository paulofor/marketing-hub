package com.marketinghub.experimentstrategist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistBehavioralSnapshotRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: prevenir pesquisa duplicada e separar propostas entre versões comerciais. */
class CommercialAssumptionProposalServiceTest {
  /**
   * Reutiliza trabalho ativo, bloqueia falha sem causa alterada e preserva as entradas originais.
   */
  @Test
  void reusesActiveProposalAndBlocksUnchangedFailure() {
    for (var status :
        new ExperimentStrategistExecutionStatus[] {
          ExperimentStrategistExecutionStatus.PENDING,
          ExperimentStrategistExecutionStatus.RUNNING,
          ExperimentStrategistExecutionStatus.FAILED
        }) {
      var repository = mock(ExperimentStrategistExecutionRepository.class);
      var plan = new CommercialPlan();
      plan.setId(804L);
      var plans = mock(CommercialPlanService.class);
      when(plans.getPlanForUpdate(plan.getId())).thenReturn(plan);
      var versions = mock(CommercialPlanVersionService.class);
      when(versions.current(plan.getId()))
          .thenReturn(
              new CommercialPlanVersionDto(
                  805L,
                  plan.getId(),
                  3,
                  "{}",
                  "QA",
                  "Condições atuais",
                  Instant.now().minusSeconds(120)));
      var existing = new ExperimentStrategistExecution();
      existing.setId(901L);
      existing.setCommercialPlan(plan);
      existing.setStatus(status);
      existing.setCreatedAt(Instant.now());
      existing.setEvidenceSnapshot("{\"preserved\":true}");
      when(repository.findFirstByCommercialPlanIdAndAuthorityModeOrderByCreatedAtDescIdDesc(
              plan.getId(), "COMMERCIAL_ASSUMPTIONS_PROPOSAL"))
          .thenReturn(Optional.of(existing));
      var service =
          new ExperimentStrategistExecutionService(
              repository,
              mock(ExperimentStrategistBehavioralSnapshotRepository.class),
              plans,
              mock(ExperimentStrategistContextService.class),
              new ObjectMapper().registerModule(new JavaTimeModule()),
              event -> {},
              versions);
      if (status == ExperimentStrategistExecutionStatus.FAILED)
        assertThatThrownBy(() -> service.startCommercialAssumptions(plan.getId()))
            .hasMessageContaining("corrigir a causa");
      else
        assertThat(service.startCommercialAssumptions(plan.getId()).id())
            .isEqualTo(existing.getId());
      verify(repository, never()).save(any());
      assertThat(existing.getEvidenceSnapshot()).isEqualTo("{\"preserved\":true}");
    }
  }

  /** Uma mudança de contexto abre proposta própria sem transportar o parecer anterior. */
  @Test
  void changedVersionCreatesItsOwnProposal() {
    var repository = mock(ExperimentStrategistExecutionRepository.class);
    var plan = new CommercialPlan();
    plan.setId(502L);
    plan.setName("QA de contexto novo");
    var plans = mock(CommercialPlanService.class);
    when(plans.getPlanForUpdate(plan.getId())).thenReturn(plan);
    var versions = mock(CommercialPlanVersionService.class);
    when(versions.current(plan.getId()))
        .thenReturn(
            new CommercialPlanVersionDto(
                503L, plan.getId(), 4, "{}", "QA", "Mudança aprovada", Instant.now()));
    var existing = new ExperimentStrategistExecution();
    existing.setId(608L);
    existing.setCommercialPlan(plan);
    existing.setStatus(ExperimentStrategistExecutionStatus.COMPLETED);
    existing.setEvidenceSnapshot("{}");
    existing.setCreatedAt(Instant.now().minusSeconds(120));
    when(repository.findFirstByCommercialPlanIdAndAuthorityModeOrderByCreatedAtDescIdDesc(
            plan.getId(), "COMMERCIAL_ASSUMPTIONS_PROPOSAL"))
        .thenReturn(Optional.of(existing));
    var contexts = mock(ExperimentStrategistContextService.class);
    when(contexts.researchContext(plan.getId()))
        .thenReturn(Map.of("commercialPlan", Map.of("id", plan.getId(), "version", 4)));
    when(repository.save(any()))
        .thenAnswer(
            invocation -> {
              var created = invocation.<ExperimentStrategistExecution>getArgument(0);
              created.setId(709L);
              return created;
            });
    var service =
        new ExperimentStrategistExecutionService(
            repository,
            mock(ExperimentStrategistBehavioralSnapshotRepository.class),
            plans,
            contexts,
            new ObjectMapper(),
            event -> {},
            versions);
    var created = service.startCommercialAssumptions(plan.getId());
    assertThat(created.id()).isEqualTo(709L);
    assertThat(created.evidenceSnapshot()).contains("\"version\":4");
    assertThat(existing.getStatus()).isEqualTo(ExperimentStrategistExecutionStatus.COMPLETED);
  }
}
