package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.runNext.RecordRoutineResearchOrchestratorResult;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Valida a etapa zero que seleciona o próximo nicho CNAE e cria o ciclo de pesquisa de rotina. */
@ExtendWith(MockitoExtension.class)
class BackendRoutineResearchOrchestratorServiceTest {
    @Mock private OprmNicheCandidateRepository nicheCandidateRepository;

    @Mock private OprmRoutineResearchCycleRepository routineResearchCycleRepository;

    @InjectMocks private BackendRoutineResearchOrchestratorService service;

    /** Deve criar ciclo e marcar o candidato como em pesquisa quando existir nicho pendente. */
    @Test
    void runNextCreatesCycleAndMarksCandidateRunning() {
        OprmNicheCandidate candidate = candidate();
        when(nicheCandidateRepository.findNextPendingRoutineResearchCandidate(any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(routineResearchCycleRepository.save(any(OprmRoutineResearchCycle.class)))
                .thenAnswer(invocation -> {
                    OprmRoutineResearchCycle cycle = invocation.getArgument(0);
                    cycle.setId(321L);
                    return cycle;
                });

        RecordRoutineResearchOrchestratorResult result = service.runNext();

        assertThat(result.started()).isTrue();
        assertThat(result.researchCycleId()).isEqualTo(321L);
        assertThat(result.sourceNicheId()).isEqualTo(55L);
        assertThat(result.triggerSource()).isEqualTo("AUTO_SCORE_QUEUE");
        assertThat(result.cycleStatus()).isEqualTo("RUNNING");
        assertThat(result.routineResearchStatus()).isEqualTo("RESEARCH_RUNNING");

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
        verify(routineResearchCycleRepository).save(cycleCaptor.capture());
        OprmRoutineResearchCycle savedCycle = cycleCaptor.getValue();
        assertThat(savedCycle.getSourceNicheId()).isEqualTo(55L);
        assertThat(savedCycle.getCnaeCode()).isEqualTo("9602501");
        assertThat(savedCycle.getNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(savedCycle.getSourceScore()).isEqualByComparingTo("92.50");
        assertThat(savedCycle.getTotalQueries()).isZero();
        assertThat(savedCycle.getTotalSourceCandidates()).isZero();
        assertThat(savedCycle.getTotalSourceSnapshots()).isZero();
        assertThat(savedCycle.getTotalExtractedSignals()).isZero();

        ArgumentCaptor<OprmNicheCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmNicheCandidate.class);
        verify(nicheCandidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getRoutineResearchStatus()).isEqualTo("RESEARCH_RUNNING");
        assertThat(candidateCaptor.getValue().getLastRoutineResearchCycleId()).isEqualTo(321L);
    }

    /** Deve retornar resultado sem início quando não houver nicho pendente com score. */
    @Test
    void runNextReturnsEmptyResultWhenNoCandidateExists() {
        when(nicheCandidateRepository.findNextPendingRoutineResearchCandidate(any(Pageable.class))).thenReturn(List.of());

        RecordRoutineResearchOrchestratorResult result = service.runNext();

        assertThat(result.started()).isFalse();
        assertThat(result.researchCycleId()).isNull();
        assertThat(result.message()).contains("Nenhum nicho CNAE pendente");
    }

    /** Monta um candidato de nicho CNAE com score para a etapa zero do pipeline. */
    private OprmNicheCandidate candidate() {
        OprmNicheCandidate candidate = new OprmNicheCandidate();
        candidate.setId(55L);
        candidate.setCnaeCode("9602501");
        candidate.setCnaeDescription("Cabeleireiros, manicure e pedicure");
        candidate.setCandidateNicheName("Cabeleireiros e manicures");
        candidate.setOpportunityScore(new BigDecimal("92.50"));
        candidate.setRoutineResearchStatus("PENDING");
        candidate.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        candidate.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        return candidate;
    }
}
