package com.marketinghub.oprm.nichocnae.routineresearchcycle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Responsabilidade: validar a proteção contra ciclos OPRM NichoCNAE parados sem progresso. */
@ExtendWith(MockitoExtension.class)
class BackendRoutineResearchCycleStallGuardServiceTest {
  @Mock private OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  @Mock private OprmNicheCandidateRepository nicheCandidateRepository;

  @InjectMocks private BackendRoutineResearchCycleStallGuardService service;

  /** Deve marcar ciclo RUNNING antigo sem progresso como STALLED e atualizar o candidato vinculado. */
  @Test
  void markRunningCyclesWithoutProgressAsStalledUpdatesCycleAndCandidate() {
    Instant now = Instant.parse("2026-06-12T12:00:00Z");
    OprmRoutineResearchCycle cycle = cycle();
    OprmNicheCandidate candidate = candidate();
    when(routineResearchCycleRepository.findRunningCyclesWithoutProgressBefore(
            eq("RUNNING"), eq(Instant.parse("2026-06-12T06:00:00Z")), any(Pageable.class)))
        .thenReturn(List.of(cycle));
    when(nicheCandidateRepository.findById(77L)).thenReturn(Optional.of(candidate));

    int stalledCount = service.markRunningCyclesWithoutProgressAsStalled(now);

    assertThat(stalledCount).isEqualTo(1);
    ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
    verify(routineResearchCycleRepository).save(cycleCaptor.capture());
    assertThat(cycleCaptor.getValue().getStatus()).isEqualTo("STALLED");
    assertThat(cycleCaptor.getValue().getFinishedAt()).isEqualTo(now);
    assertThat(cycleCaptor.getValue().getErrorMessage()).contains("oprm-coletor-mei").contains("researchCycleId=1001");

    ArgumentCaptor<OprmNicheCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmNicheCandidate.class);
    verify(nicheCandidateRepository).save(candidateCaptor.capture());
    assertThat(candidateCaptor.getValue().getRoutineResearchStatus()).isEqualTo("RESEARCH_STALLED");
    assertThat(candidateCaptor.getValue().getLastRoutineResearchCycleId()).isEqualTo(1001L);
  }

  /** Deve retornar zero quando não houver ciclos antigos sem progresso para proteger. */
  @Test
  void markRunningCyclesWithoutProgressAsStalledDoesNothingWhenRepositoryReturnsEmptyList() {
    Instant now = Instant.parse("2026-06-12T12:00:00Z");
    when(routineResearchCycleRepository.findRunningCyclesWithoutProgressBefore(
            eq("RUNNING"), eq(Instant.parse("2026-06-12T06:00:00Z")), any(Pageable.class)))
        .thenReturn(List.of());

    int stalledCount = service.markRunningCyclesWithoutProgressAsStalled(now);

    assertThat(stalledCount).isZero();
  }

  /** Monta um ciclo RUNNING sem nenhum contador de progresso. */
  private OprmRoutineResearchCycle cycle() {
    OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
    cycle.setId(1001L);
    cycle.setSourceNicheId(77L);
    cycle.setCnaeCode("9602501");
    cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
    cycle.setNicheName("Cabeleireiros, manicures e pedicures");
    cycle.setOriginalNicheName("Cabeleireiros, manicures e pedicures");
    cycle.setNeutralNicheName("Cabeleireiros, manicures e pedicures");
    cycle.setResearchMode("ROUTINE_REALITY_RESEARCH");
    cycle.setSolutionLanguageRiskScore(BigDecimal.ZERO);
    cycle.setSourceScore(new BigDecimal("90.00"));
    cycle.setTriggerSource("AUTO_SCORE_QUEUE");
    cycle.setStatus("RUNNING");
    cycle.setTotalQueries(0);
    cycle.setTotalSourceCandidates(0);
    cycle.setTotalSourceSnapshots(0);
    cycle.setTotalExtractedSignals(0);
    cycle.setStartedAt(Instant.parse("2026-06-12T04:30:00Z"));
    cycle.setCreatedAt(Instant.parse("2026-06-12T04:30:00Z"));
    cycle.setUpdatedAt(Instant.parse("2026-06-12T04:30:00Z"));
    return cycle;
  }

  /** Monta um candidato vinculado ao ciclo parado. */
  private OprmNicheCandidate candidate() {
    OprmNicheCandidate candidate = new OprmNicheCandidate();
    candidate.setId(77L);
    candidate.setRoutineResearchStatus("RESEARCH_RUNNING");
    candidate.setLastRoutineResearchCycleId(1001L);
    candidate.setUpdatedAt(Instant.parse("2026-06-12T04:30:00Z"));
    return candidate;
  }
}
