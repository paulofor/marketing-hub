package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.pending.RecordRoutineResearchOrchestratorPending;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.recent.RecordRoutineResearchOrchestratorRecent;
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

    /** Deve listar o próximo candidato pendente sem usar a consulta com bloqueio pessimista. */
    @Test
    void listPendingUsesPreviewQueryWithoutPessimisticLock() {
        OprmNicheCandidate candidate = candidate();
        when(nicheCandidateRepository.findNextPendingRoutineResearchCandidatePreview(any(Pageable.class)))
                .thenReturn(List.of(candidate));

        List<RecordRoutineResearchOrchestratorPending> result = service.listPending();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().sourceNicheId()).isEqualTo(55L);
        assertThat(result.getFirst().cnaeCode()).isEqualTo("9602501");
        assertThat(result.getFirst().routineResearchStatus()).isEqualTo("PENDING");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(nicheCandidateRepository).findNextPendingRoutineResearchCandidatePreview(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
        verifyNoInteractions(routineResearchCycleRepository);
    }

    /** Deve listar os últimos nichos processados pela etapa zero com horário do ciclo criado. */
    @Test
    void listRecentProcessedReturnsLatestCyclesWithProcessedAt() {
        OprmRoutineResearchCycle firstCycle = cycle(321L, 55L, "Cabeleireiros e manicures", "2026-06-03T01:00:00Z");
        OprmRoutineResearchCycle secondCycle = cycle(320L, 54L, "Lojas de roupas", "2026-06-02T22:00:00Z");
        when(routineResearchCycleRepository.findAllByOrderByStartedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(firstCycle, secondCycle));

        List<RecordRoutineResearchOrchestratorRecent> result = service.listRecentProcessed(25);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().researchCycleId()).isEqualTo(321L);
        assertThat(result.getFirst().sourceNicheId()).isEqualTo(55L);
        assertThat(result.getFirst().nicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(result.getFirst().originalNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(result.getFirst().neutralNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(result.getFirst().researchMode()).isEqualTo("ROUTINE_REALITY_RESEARCH");
        assertThat(result.getFirst().solutionLanguageRiskScore()).isEqualByComparingTo("0.00");
        assertThat(result.getFirst().sourceScore()).isEqualByComparingTo("92.50");
        assertThat(result.getFirst().processedAt()).isEqualTo(Instant.parse("2026-06-03T01:00:00Z"));
        assertThat(result.getFirst().finishedAt()).isEqualTo(Instant.parse("2026-06-03T02:00:00Z"));
        assertThat(result.getFirst().errorMessage()).isEqualTo("nicheName is required");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(routineResearchCycleRepository).findAllByOrderByStartedAtDesc(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        verifyNoInteractions(nicheCandidateRepository);
    }

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
        assertThat(result.originalNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(result.neutralNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(result.researchMode()).isEqualTo("ROUTINE_REALITY_RESEARCH");

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
        verify(routineResearchCycleRepository).save(cycleCaptor.capture());
        OprmRoutineResearchCycle savedCycle = cycleCaptor.getValue();
        assertThat(savedCycle.getSourceNicheId()).isEqualTo(55L);
        assertThat(savedCycle.getCnaeCode()).isEqualTo("9602501");
        assertThat(savedCycle.getNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(savedCycle.getOriginalNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(savedCycle.getNeutralNicheName()).isEqualTo("Cabeleireiros e manicures");
        assertThat(savedCycle.getResearchMode()).isEqualTo("ROUTINE_REALITY_RESEARCH");
        assertThat(savedCycle.getSolutionLanguageRiskScore()).isEqualByComparingTo("0.00");
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


    /** Deve neutralizar nome contaminado e preservar o original para auditoria do ciclo. */
    @Test
    void runNextNeutralizesSolutionLanguageAndKeepsOriginalName() {
        OprmNicheCandidate candidate = candidate();
        candidate.setCandidateNicheName("IA para crescimento de Cabeleireiros, manicure e pedicure");
        when(nicheCandidateRepository.findNextPendingRoutineResearchCandidate(any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(routineResearchCycleRepository.save(any(OprmRoutineResearchCycle.class)))
                .thenAnswer(invocation -> {
                    OprmRoutineResearchCycle cycle = invocation.getArgument(0);
                    cycle.setId(322L);
                    return cycle;
                });

        RecordRoutineResearchOrchestratorResult result = service.runNext();

        assertThat(result.started()).isTrue();
        assertThat(result.nicheName()).isEqualTo("Cabeleireiros, manicure e pedicure");
        assertThat(result.originalNicheName()).isEqualTo("IA para crescimento de Cabeleireiros, manicure e pedicure");
        assertThat(result.neutralNicheName()).isEqualTo("Cabeleireiros, manicure e pedicure");
        assertThat(result.researchMode()).isEqualTo("ROUTINE_REALITY_RESEARCH");
        assertThat(result.solutionLanguageRiskScore()).isEqualByComparingTo("100.00");

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor = ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
        verify(routineResearchCycleRepository).save(cycleCaptor.capture());
        OprmRoutineResearchCycle savedCycle = cycleCaptor.getValue();
        assertThat(savedCycle.getNicheName()).isEqualTo("Cabeleireiros, manicure e pedicure");
        assertThat(savedCycle.getOriginalNicheName()).isEqualTo("IA para crescimento de Cabeleireiros, manicure e pedicure");
        assertThat(savedCycle.getNeutralNicheName()).isEqualTo("Cabeleireiros, manicure e pedicure");
        assertThat(savedCycle.getResearchMode()).isEqualTo("ROUTINE_REALITY_RESEARCH");
        assertThat(savedCycle.getSolutionLanguageRiskScore()).isEqualByComparingTo("100.00");
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

    /** Monta um ciclo de pesquisa de rotina criado pela etapa zero do pipeline. */
    private OprmRoutineResearchCycle cycle(Long id, Long sourceNicheId, String nicheName, String startedAt) {
        OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
        cycle.setId(id);
        cycle.setSourceNicheId(sourceNicheId);
        cycle.setCnaeCode("9602501");
        cycle.setCnaeDescription("Cabeleireiros, manicure e pedicure");
        cycle.setNicheName(nicheName);
        cycle.setOriginalNicheName(nicheName);
        cycle.setNeutralNicheName(nicheName);
        cycle.setResearchMode("ROUTINE_REALITY_RESEARCH");
        cycle.setSolutionLanguageRiskScore(BigDecimal.ZERO);
        cycle.setSourceScore(new BigDecimal("92.50"));
        cycle.setTriggerSource("AUTO_SCORE_QUEUE");
        cycle.setStatus("RUNNING");
        cycle.setTotalQueries(0);
        cycle.setTotalSourceCandidates(0);
        cycle.setTotalSourceSnapshots(0);
        cycle.setTotalExtractedSignals(0);
        cycle.setStartedAt(Instant.parse(startedAt));
        cycle.setFinishedAt(Instant.parse("2026-06-03T02:00:00Z"));
        cycle.setErrorMessage("nicheName is required");
        cycle.setCreatedAt(Instant.parse(startedAt));
        cycle.setUpdatedAt(Instant.parse(startedAt));
        return cycle;
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
