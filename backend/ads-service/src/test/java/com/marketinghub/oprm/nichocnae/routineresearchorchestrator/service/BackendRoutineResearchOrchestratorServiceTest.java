package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.pending.RecordRoutineResearchOrchestratorPending;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.recent.RecordRoutineResearchOrchestratorRecent;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.runNext.RecordRoutineResearchOrchestratorResult;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
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

/** Valida a etapa zero que seleciona o próximo nicho CNAE e cria o ciclo de pesquisa de rotina. */
@ExtendWith(MockitoExtension.class)
class BackendRoutineResearchOrchestratorServiceTest {
    @Mock private OprmNicheCandidateRepository nicheCandidateRepository;

    @Mock private OprmRoutineResearchCycleRepository routineResearchCycleRepository;

    @Mock private OprmMeiAudienceProfileRepository meiAudienceProfileRepository;

    @Mock private OprmNicheRoutineCardRepository routineCardRepository;

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
        verify(routineResearchCycleRepository, never())
                .findAllByOrderByStartedAtDesc(any(Pageable.class));
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
        assertThat(result.getFirst().existingMarketNicheId()).isNull();
        assertThat(result.getFirst().alreadyMaterialized()).isFalse();
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
    }

    /** Deve indicar o nicho existente quando o candidato já foi materializado em MarketNiche. */
    @Test
    void listRecentProcessedReturnsExistingMarketNicheAssociation() {
        OprmRoutineResearchCycle cycle = cycle(321L, 55L, "Cabeleireiros e manicures", "2026-06-03T01:00:00Z");
        OprmNicheCandidate candidate = candidate();
        candidate.setMarketNicheId(987L);
        when(routineResearchCycleRepository.findAllByOrderByStartedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(cycle));
        when(nicheCandidateRepository.findById(55L)).thenReturn(Optional.of(candidate));

        List<RecordRoutineResearchOrchestratorRecent> result = service.listRecentProcessed(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().existingMarketNicheId()).isEqualTo(987L);
        assertThat(result.getFirst().alreadyMaterialized()).isTrue();
    }

    /** Deve indicar o nicho existente pelo perfil enriquecido quando o candidato não tem vínculo direto. */
    @Test
    void listRecentProcessedReturnsMarketNicheAssociationFromEnrichmentProfile() {
        OprmRoutineResearchCycle cycle = cycle(321L, 55L, "Cabeleireiros e manicures", "2026-06-03T01:00:00Z");
        when(routineResearchCycleRepository.findAllByOrderByStartedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(cycle));
        when(nicheCandidateRepository.findById(55L)).thenReturn(Optional.empty());
        when(routineResearchCycleRepository.findLatestMaterializedMarketNicheIdByResearchCycleId(
                org.mockito.ArgumentMatchers.eq(321L), any(Pageable.class)))
                .thenReturn(List.of(654L));

        List<RecordRoutineResearchOrchestratorRecent> result = service.listRecentProcessed(10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().existingMarketNicheId()).isEqualTo(654L);
        assertThat(result.getFirst().alreadyMaterialized()).isTrue();
    }

    /** Deve criar novo ciclo imediatamente ao reprocessar um ciclo falho. */
    @Test
    void reprocessFailedCycleCreatesNewCycleImmediately() {
        OprmRoutineResearchCycle failedCycle = cycle(321L, 55L, "Cabeleireiros e manicures", "2026-06-03T01:00:00Z");
        failedCycle.setStatus("FAILED");
        OprmNicheCandidate candidate = candidate();
        candidate.setRoutineResearchStatus("RESEARCH_RUNNING");
        candidate.setLastRoutineResearchCycleId(321L);
        when(routineResearchCycleRepository.findById(321L)).thenReturn(Optional.of(failedCycle));
        when(nicheCandidateRepository.findById(55L)).thenReturn(Optional.of(candidate));
        when(routineResearchCycleRepository.save(any(OprmRoutineResearchCycle.class)))
                .thenAnswer(invocation -> {
                    OprmRoutineResearchCycle newCycle = invocation.getArgument(0);
                    newCycle.setId(322L);
                    return newCycle;
                });
        when(nicheCandidateRepository.save(any(OprmNicheCandidate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reprocessCycle(321L);

        assertThat(result.researchCycleId()).isEqualTo(322L);
        assertThat(result.sourceNicheId()).isEqualTo(55L);
        assertThat(result.previousCycleStatus()).isEqualTo("FAILED");
        assertThat(result.previousRoutineResearchStatus()).isEqualTo("RESEARCH_RUNNING");
        assertThat(result.routineResearchStatus()).isEqualTo("RESEARCH_RUNNING");
        assertThat(result.lastRoutineResearchCycleId()).isEqualTo(322L);
        assertThat(result.message()).contains("CNAE com falha");

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor =
                ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
        verify(routineResearchCycleRepository).save(cycleCaptor.capture());
        assertThat(cycleCaptor.getValue().getSourceNicheId()).isEqualTo(55L);
        assertThat(cycleCaptor.getValue().getStatus()).isEqualTo("RUNNING");
        assertThat(cycleCaptor.getValue().getTriggerSource()).isEqualTo("MANUAL_REPROCESS");

        ArgumentCaptor<OprmNicheCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmNicheCandidate.class);
        verify(nicheCandidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getRoutineResearchStatus()).isEqualTo("RESEARCH_RUNNING");
        assertThat(candidateCaptor.getValue().getLastRoutineResearchCycleId()).isEqualTo(322L);
    }

    /** Deve criar novo ciclo imediatamente ao reprocessar um ciclo parado sem progresso. */
    @Test
    void reprocessStalledCycleCreatesNewCycleImmediately() {
        OprmRoutineResearchCycle stalledCycle = cycle(321L, 55L, "Cabeleireiros e manicures", "2026-06-03T01:00:00Z");
        stalledCycle.setStatus("STALLED");
        OprmNicheCandidate candidate = candidate();
        candidate.setRoutineResearchStatus("RESEARCH_STALLED");
        candidate.setLastRoutineResearchCycleId(321L);
        when(routineResearchCycleRepository.findById(321L)).thenReturn(Optional.of(stalledCycle));
        when(nicheCandidateRepository.findById(55L)).thenReturn(Optional.of(candidate));
        when(routineResearchCycleRepository.save(any(OprmRoutineResearchCycle.class)))
                .thenAnswer(invocation -> {
                    OprmRoutineResearchCycle newCycle = invocation.getArgument(0);
                    newCycle.setId(322L);
                    return newCycle;
                });
        when(nicheCandidateRepository.save(any(OprmNicheCandidate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reprocessCycle(321L);

        assertThat(result.researchCycleId()).isEqualTo(322L);
        assertThat(result.previousCycleStatus()).isEqualTo("STALLED");
        assertThat(result.previousRoutineResearchStatus()).isEqualTo("RESEARCH_STALLED");
        assertThat(result.routineResearchStatus()).isEqualTo("RESEARCH_RUNNING");
        assertThat(result.message()).contains("pipeline parado");
    }

    /** Deve criar novo ciclo imediato quando a pesquisa chegou fraca e precisa de mais evidência. */
    @Test
    void reprocessNeedsMoreResearchCycleCreatesNewCycleImmediately() {
        OprmRoutineResearchCycle weakCycle = cycle(321L, 55L, "Cabeleireiros e manicures", "2026-06-03T01:00:00Z");
        weakCycle.setStatus("NEEDS_MORE_RESEARCH");
        weakCycle.setFinishedAt(null);
        weakCycle.setErrorMessage(null);
        OprmNicheCandidate candidate = candidate();
        candidate.setRoutineResearchStatus("RESEARCH_RUNNING");
        candidate.setLastRoutineResearchCycleId(321L);
        when(routineResearchCycleRepository.findById(321L)).thenReturn(Optional.of(weakCycle));
        when(nicheCandidateRepository.findById(55L)).thenReturn(Optional.of(candidate));
        when(routineResearchCycleRepository.save(any(OprmRoutineResearchCycle.class)))
                .thenAnswer(invocation -> {
                    OprmRoutineResearchCycle newCycle = invocation.getArgument(0);
                    newCycle.setId(322L);
                    return newCycle;
                });
        when(nicheCandidateRepository.save(any(OprmNicheCandidate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reprocessCycle(321L);

        assertThat(result.researchCycleId()).isEqualTo(322L);
        assertThat(result.previousCycleStatus()).isEqualTo("NEEDS_MORE_RESEARCH");
        assertThat(result.message()).contains("precisava de mais pesquisa");

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor =
                ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
        verify(routineResearchCycleRepository).save(cycleCaptor.capture());
        assertThat(cycleCaptor.getValue().getStatus()).isEqualTo("RUNNING");
        assertThat(cycleCaptor.getValue().getTriggerSource()).isEqualTo("MANUAL_REPROCESS");

        ArgumentCaptor<OprmNicheCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmNicheCandidate.class);
        verify(nicheCandidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getRoutineResearchStatus()).isEqualTo("RESEARCH_RUNNING");
        assertThat(candidateCaptor.getValue().getLastRoutineResearchCycleId()).isEqualTo(322L);
    }

    /** Deve criar novo ciclo via front-end quando o card aprovado falhou na materialização final. */
    @Test
    void reprocessEnrichedNicheFailedCycleCreatesNewCycleImmediately() {
        OprmRoutineResearchCycle failedMaterializationCycle = cycle(
                321L, 55L, "Cabeleireiros e manicures", "2026-06-03T01:00:00Z");
        failedMaterializationCycle.setStatus("ENRICHED_NICHE_FAILED");
        failedMaterializationCycle.setErrorMessage("NullPointerException");
        OprmNicheCandidate candidate = candidate();
        candidate.setRoutineResearchStatus("RESEARCH_RUNNING");
        candidate.setLastRoutineResearchCycleId(321L);
        when(routineResearchCycleRepository.findById(321L)).thenReturn(Optional.of(failedMaterializationCycle));
        when(nicheCandidateRepository.findById(55L)).thenReturn(Optional.of(candidate));
        when(routineResearchCycleRepository.save(any(OprmRoutineResearchCycle.class)))
                .thenAnswer(invocation -> {
                    OprmRoutineResearchCycle newCycle = invocation.getArgument(0);
                    newCycle.setId(322L);
                    return newCycle;
                });
        when(nicheCandidateRepository.save(any(OprmNicheCandidate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reprocessCycle(321L);

        assertThat(result.researchCycleId()).isEqualTo(322L);
        assertThat(result.previousCycleStatus()).isEqualTo("ENRICHED_NICHE_FAILED");
        assertThat(result.message()).contains("refazer pelo front-end");

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor =
                ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
        verify(routineResearchCycleRepository).save(cycleCaptor.capture());
        assertThat(cycleCaptor.getValue().getStatus()).isEqualTo("RUNNING");
        assertThat(cycleCaptor.getValue().getTriggerSource()).isEqualTo("MANUAL_REPROCESS");

        ArgumentCaptor<OprmNicheCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmNicheCandidate.class);
        verify(nicheCandidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getRoutineResearchStatus()).isEqualTo("RESEARCH_RUNNING");
        assertThat(candidateCaptor.getValue().getLastRoutineResearchCycleId()).isEqualTo(322L);
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

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor =
                ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
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

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor =
                ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
        verify(routineResearchCycleRepository).save(cycleCaptor.capture());
        OprmRoutineResearchCycle savedCycle = cycleCaptor.getValue();
        assertThat(savedCycle.getNicheName()).isEqualTo("Cabeleireiros, manicure e pedicure");
        assertThat(savedCycle.getOriginalNicheName()).isEqualTo("IA para crescimento de Cabeleireiros, manicure e pedicure");
        assertThat(savedCycle.getNeutralNicheName()).isEqualTo("Cabeleireiros, manicure e pedicure");
        assertThat(savedCycle.getResearchMode()).isEqualTo("ROUTINE_REALITY_RESEARCH");
        assertThat(savedCycle.getSolutionLanguageRiskScore()).isEqualByComparingTo("100.00");
    }

    /** Deve criar ciclo manual para o CNAE escolhido na tela de detalhe. */
    @Test
    void runForCnaeCreatesManualCycleForSelectedCnae() {
        OprmNicheCandidate candidate = candidate();
        when(nicheCandidateRepository.findManualRoutineResearchCandidateByCnaeCode(
                        any(String.class), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(routineResearchCycleRepository.save(any(OprmRoutineResearchCycle.class)))
                .thenAnswer(invocation -> {
                    OprmRoutineResearchCycle cycle = invocation.getArgument(0);
                    cycle.setId(323L);
                    return cycle;
                });

        RecordRoutineResearchOrchestratorResult result = service.runForCnae("9602501");

        assertThat(result.started()).isTrue();
        assertThat(result.researchCycleId()).isEqualTo(323L);
        assertThat(result.cnaeCode()).isEqualTo("9602501");
        assertThat(result.triggerSource()).isEqualTo("MANUAL_CNAE_DETAIL");
        assertThat(result.routineResearchStatus()).isEqualTo("RESEARCH_RUNNING");

        ArgumentCaptor<OprmRoutineResearchCycle> cycleCaptor =
                ArgumentCaptor.forClass(OprmRoutineResearchCycle.class);
        verify(routineResearchCycleRepository).save(cycleCaptor.capture());
        assertThat(cycleCaptor.getValue().getTriggerSource()).isEqualTo("MANUAL_CNAE_DETAIL");

        ArgumentCaptor<OprmNicheCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmNicheCandidate.class);
        verify(nicheCandidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getLastRoutineResearchCycleId()).isEqualTo(323L);
    }

    /** Deve encerrar ciclos abertos do CNAE antes de criar um ciclo manual completamente novo. */
    @Test
    void runForCnaeFinishesOpenCyclesBeforeCreatingFreshManualCycle() {
        OprmNicheCandidate candidate = candidate();
        candidate.setRoutineResearchStatus("RESEARCH_RUNNING");
        candidate.setLastRoutineResearchCycleId(321L);
        OprmRoutineResearchCycle openCycle = cycle(321L, 55L, "Cabeleireiros e manicures", "2026-06-03T01:00:00Z");
        openCycle.setStatus("ROUTINE_SYNTHESIZED");
        openCycle.setFinishedAt(null);
        openCycle.setErrorMessage(null);
        when(nicheCandidateRepository.findManualRoutineResearchCandidateByCnaeCode(
                        any(String.class), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(routineResearchCycleRepository.findOpenCyclesByCnaeCodeForUpdate("9602501"))
                .thenReturn(List.of(openCycle));
        when(routineResearchCycleRepository.save(any(OprmRoutineResearchCycle.class)))
                .thenAnswer(invocation -> {
                    OprmRoutineResearchCycle cycle = invocation.getArgument(0);
                    cycle.setId(323L);
                    return cycle;
                });

        RecordRoutineResearchOrchestratorResult result = service.runForCnae("9602501");

        assertThat(result.started()).isTrue();
        assertThat(result.researchCycleId()).isEqualTo(323L);
        assertThat(result.routineResearchStatus()).isEqualTo("RESEARCH_RUNNING");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<OprmRoutineResearchCycle>> openCyclesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(routineResearchCycleRepository).saveAll(openCyclesCaptor.capture());
        OprmRoutineResearchCycle cancelledCycle = openCyclesCaptor.getValue().iterator().next();
        assertThat(cancelledCycle.getStatus()).isEqualTo("CANCELLED_BY_MANUAL_RESTART");
        assertThat(cancelledCycle.getFinishedAt()).isNotNull();
        assertThat(cancelledCycle.getErrorMessage()).contains("reinício manual completo do CNAE 9602501");

        ArgumentCaptor<OprmNicheCandidate> candidateCaptor = ArgumentCaptor.forClass(OprmNicheCandidate.class);
        verify(nicheCandidateRepository).save(candidateCaptor.capture());
        assertThat(candidateCaptor.getValue().getLastRoutineResearchCycleId()).isEqualTo(323L);
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
