package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.pending.RecordRoutineResearchOrchestratorPending;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.runNext.RecordRoutineResearchOrchestratorResult;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsável por selecionar o próximo nicho CNAE com score alto e abrir o ciclo de pesquisa de rotina. */
@Service
public class BackendRoutineResearchOrchestratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendRoutineResearchOrchestratorService.class);
    private static final String ROUTINE_STATUS_PENDING = "PENDING";
    private static final String ROUTINE_STATUS_RUNNING = "RESEARCH_RUNNING";
    private static final String CYCLE_STATUS_RUNNING = "RUNNING";
    private static final String TRIGGER_SOURCE_AUTO_SCORE_QUEUE = "AUTO_SCORE_QUEUE";

    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;

    /** Inicializa o serviço com os repositórios canônicos usados pela etapa zero do pipeline. */
    public BackendRoutineResearchOrchestratorService(
            OprmNicheCandidateRepository nicheCandidateRepository,
            OprmRoutineResearchCycleRepository routineResearchCycleRepository) {
        this.nicheCandidateRepository = nicheCandidateRepository;
        this.routineResearchCycleRepository = routineResearchCycleRepository;
    }

    /** Lista o próximo nicho CNAE pendente que seria selecionado pela etapa zero. */
    @Transactional(readOnly = true)
    public List<RecordRoutineResearchOrchestratorPending> listPending() {
        return nicheCandidateRepository.findNextPendingRoutineResearchCandidate(PageRequest.of(0, 1)).stream()
                .map(this::toPending)
                .toList();
    }

    /** Executa a etapa zero, marcando o nicho selecionado como em pesquisa e criando o ciclo pai. */
    @Transactional
    public RecordRoutineResearchOrchestratorResult runNext() {
        List<OprmNicheCandidate> candidates = nicheCandidateRepository.findNextPendingRoutineResearchCandidate(
                PageRequest.of(0, 1));
        if (candidates.isEmpty()) {
            return new RecordRoutineResearchOrchestratorResult(
                    false, null, null, null, null, null, null, null, null, ROUTINE_STATUS_PENDING,
                    "Nenhum nicho CNAE pendente com score disponível para pesquisa de rotina.");
        }

        OprmNicheCandidate candidate = candidates.getFirst();
        Instant now = Instant.now();
        try {
            OprmRoutineResearchCycle cycle = createCycle(candidate, now);
            OprmRoutineResearchCycle savedCycle = routineResearchCycleRepository.save(cycle);
            candidate.setRoutineResearchStatus(ROUTINE_STATUS_RUNNING);
            candidate.setLastRoutineResearchCycleId(savedCycle.getId());
            candidate.setUpdatedAt(now);
            nicheCandidateRepository.save(candidate);
            return toStartedResult(candidate, savedCycle);
        } catch (RuntimeException ex) {
            LOGGER.error(
                    "Erro ao executar etapa zero do OPRM nichocnae (sourceNicheId={}, cnaeCode={}, nicheName={}, score={})",
                    candidate.getId(),
                    candidate.getCnaeCode(),
                    candidate.getCandidateNicheName(),
                    candidate.getOpportunityScore(),
                    ex);
            throw ex;
        }
    }

    /** Cria a entidade do ciclo pai de pesquisa de rotina a partir do nicho CNAE selecionado. */
    private OprmRoutineResearchCycle createCycle(OprmNicheCandidate candidate, Instant now) {
        OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
        cycle.setSourceNicheId(candidate.getId());
        cycle.setCnaeCode(candidate.getCnaeCode());
        cycle.setCnaeDescription(candidate.getCnaeDescription());
        cycle.setNicheName(candidate.getCandidateNicheName());
        cycle.setSourceScore(candidate.getOpportunityScore());
        cycle.setTriggerSource(TRIGGER_SOURCE_AUTO_SCORE_QUEUE);
        cycle.setStatus(CYCLE_STATUS_RUNNING);
        cycle.setTotalQueries(0);
        cycle.setTotalSourceCandidates(0);
        cycle.setTotalSourceSnapshots(0);
        cycle.setTotalExtractedSignals(0);
        cycle.setStartedAt(now);
        cycle.setCreatedAt(now);
        cycle.setUpdatedAt(now);
        return cycle;
    }

    /** Converte o candidato selecionável para o contrato de pendência da etapa zero. */
    private RecordRoutineResearchOrchestratorPending toPending(OprmNicheCandidate candidate) {
        return new RecordRoutineResearchOrchestratorPending(
                candidate.getId(),
                candidate.getCnaeCode(),
                candidate.getCnaeDescription(),
                candidate.getCandidateNicheName(),
                candidate.getOpportunityScore(),
                candidate.getRoutineResearchStatus(),
                candidate.getLastRoutineResearchCycleId(),
                candidate.getCreatedAt());
    }

    /** Converte o ciclo criado e o nicho atualizado para o contrato de resultado da etapa zero. */
    private RecordRoutineResearchOrchestratorResult toStartedResult(
            OprmNicheCandidate candidate, OprmRoutineResearchCycle savedCycle) {
        return new RecordRoutineResearchOrchestratorResult(
                true,
                savedCycle.getId(),
                candidate.getId(),
                candidate.getCnaeCode(),
                candidate.getCnaeDescription(),
                candidate.getCandidateNicheName(),
                candidate.getOpportunityScore(),
                savedCycle.getTriggerSource(),
                savedCycle.getStatus(),
                candidate.getRoutineResearchStatus(),
                "Pesquisa de rotina iniciada para o próximo nicho CNAE com maior score pendente.");
    }
}
