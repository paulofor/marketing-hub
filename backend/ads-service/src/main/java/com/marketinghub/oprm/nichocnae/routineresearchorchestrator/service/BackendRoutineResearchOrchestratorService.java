package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.RoutineResearchNicheNameNormalizer;
import com.marketinghub.oprm.nichocnae.RoutineResearchNicheNameNormalizer.NormalizedNicheName;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.pending.RecordRoutineResearchOrchestratorPending;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.recent.RecordRoutineResearchOrchestratorRecent;
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
    private static final String RESEARCH_MODE_ROUTINE_REALITY = "ROUTINE_REALITY_RESEARCH";

    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
    private final RoutineResearchNicheNameNormalizer nicheNameNormalizer = new RoutineResearchNicheNameNormalizer();

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
        LOGGER.info("Listando prévia de pendência da etapa zero OPRM nichocnae (limit=1)");
        List<RecordRoutineResearchOrchestratorPending> pendingCandidates = nicheCandidateRepository
                .findNextPendingRoutineResearchCandidatePreview(PageRequest.of(0, 1))
                .stream()
                .map(this::toPending)
                .toList();
        LOGGER.info("Prévia de pendência da etapa zero OPRM nichocnae retornada (count={})", pendingCandidates.size());
        return pendingCandidates;
    }

    /** Lista os últimos nichos processados pela etapa zero para acompanhamento no card operacional. */
    @Transactional(readOnly = true)
    public List<RecordRoutineResearchOrchestratorRecent> listRecentProcessed(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        LOGGER.info("Listando ciclos recentes da etapa zero OPRM nichocnae (requestedLimit={}, safeLimit={})", limit, safeLimit);
        List<RecordRoutineResearchOrchestratorRecent> recentCycles = routineResearchCycleRepository
                .findAllByOrderByStartedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toRecentProcessed)
                .toList();
        LOGGER.info("Ciclos recentes da etapa zero OPRM nichocnae retornados (count={})", recentCycles.size());
        return recentCycles;
    }

    /** Executa a etapa zero, marcando o nicho selecionado como em pesquisa e criando o ciclo pai. */
    @Transactional
    public RecordRoutineResearchOrchestratorResult runNext() {
        LOGGER.info("Iniciando etapa zero OPRM nichocnae no backend: buscando próximo candidato pendente com bloqueio pessimista (limit=1)");
        List<OprmNicheCandidate> candidates = nicheCandidateRepository.findNextPendingRoutineResearchCandidate(
                PageRequest.of(0, 1));
        LOGGER.info("Busca de candidato pendente da etapa zero OPRM nichocnae concluída (count={})", candidates.size());
        if (candidates.isEmpty()) {
            LOGGER.info("Nenhum candidato pendente encontrado para etapa zero OPRM nichocnae; ciclo não será criado.");
            return new RecordRoutineResearchOrchestratorResult(
                    false, null, null, null, null, null, null, null, null, null, null, null, null, ROUTINE_STATUS_PENDING,
                    "Nenhum nicho CNAE pendente com score disponível para pesquisa de rotina.");
        }

        OprmNicheCandidate candidate = candidates.getFirst();
        LOGGER.info(
                "Candidato selecionado para etapa zero OPRM nichocnae (sourceNicheId={}, cnaeCode={}, nicheName={}, score={}, routineStatus={}, lastRoutineResearchCycleId={})",
                candidate.getId(),
                candidate.getCnaeCode(),
                candidate.getCandidateNicheName(),
                candidate.getOpportunityScore(),
                candidate.getRoutineResearchStatus(),
                candidate.getLastRoutineResearchCycleId());
        Instant now = Instant.now();
        try {
            OprmRoutineResearchCycle cycle = createCycle(candidate, now);
            OprmRoutineResearchCycle savedCycle = routineResearchCycleRepository.save(cycle);
            LOGGER.info(
                    "Ciclo de pesquisa de rotina criado pela etapa zero OPRM nichocnae (researchCycleId={}, sourceNicheId={}, cnaeCode={}, originalNicheName={}, neutralNicheName={}, researchMode={}, solutionLanguageRiskScore={}, cycleStatus={}, triggerSource={}, startedAt={})",
                    savedCycle.getId(),
                    savedCycle.getSourceNicheId(),
                    savedCycle.getCnaeCode(),
                    savedCycle.getOriginalNicheName(),
                    savedCycle.getNeutralNicheName(),
                    savedCycle.getResearchMode(),
                    savedCycle.getSolutionLanguageRiskScore(),
                    savedCycle.getStatus(),
                    savedCycle.getTriggerSource(),
                    savedCycle.getStartedAt());
            candidate.setRoutineResearchStatus(ROUTINE_STATUS_RUNNING);
            candidate.setLastRoutineResearchCycleId(savedCycle.getId());
            candidate.setUpdatedAt(now);
            nicheCandidateRepository.save(candidate);
            LOGGER.info(
                    "Candidato atualizado após criação do ciclo da etapa zero OPRM nichocnae (sourceNicheId={}, routineStatus={}, lastRoutineResearchCycleId={}, updatedAt={})",
                    candidate.getId(),
                    candidate.getRoutineResearchStatus(),
                    candidate.getLastRoutineResearchCycleId(),
                    candidate.getUpdatedAt());
            RecordRoutineResearchOrchestratorResult result = toStartedResult(candidate, savedCycle);
            LOGGER.info(
                    "Etapa zero OPRM nichocnae concluída no backend (started={}, researchCycleId={}, sourceNicheId={}, originalNicheName={}, neutralNicheName={}, researchMode={}, routineStatus={}, cycleStatus={})",
                    result.started(),
                    result.researchCycleId(),
                    result.sourceNicheId(),
                    savedCycle.getOriginalNicheName(),
                    savedCycle.getNeutralNicheName(),
                    savedCycle.getResearchMode(),
                    result.routineResearchStatus(),
                    result.cycleStatus());
            return result;
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

    /** Cria a entidade do ciclo pai usando nome neutro para impedir pesquisa enviesada por solução. */
    private OprmRoutineResearchCycle createCycle(OprmNicheCandidate candidate, Instant now) {
        NormalizedNicheName normalizedName = nicheNameNormalizer.normalize(
                candidate.getCandidateNicheName(), candidate.getCnaeDescription());
        OprmRoutineResearchCycle cycle = new OprmRoutineResearchCycle();
        cycle.setSourceNicheId(candidate.getId());
        cycle.setCnaeCode(candidate.getCnaeCode());
        cycle.setCnaeDescription(candidate.getCnaeDescription());
        cycle.setNicheName(normalizedName.neutralNicheName());
        cycle.setOriginalNicheName(normalizedName.originalNicheName());
        cycle.setNeutralNicheName(normalizedName.neutralNicheName());
        cycle.setResearchMode(RESEARCH_MODE_ROUTINE_REALITY);
        cycle.setSolutionLanguageRiskScore(normalizedName.solutionLanguageRiskScore());
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

    /** Converte o ciclo criado pela etapa zero para o contrato de histórico recente. */
    private RecordRoutineResearchOrchestratorRecent toRecentProcessed(OprmRoutineResearchCycle cycle) {
        return new RecordRoutineResearchOrchestratorRecent(
                cycle.getId(),
                cycle.getSourceNicheId(),
                cycle.getCnaeCode(),
                cycle.getCnaeDescription(),
                cycle.getNicheName(),
                cycle.getOriginalNicheName(),
                cycle.getNeutralNicheName(),
                cycle.getResearchMode(),
                cycle.getSolutionLanguageRiskScore(),
                cycle.getSourceScore(),
                cycle.getTriggerSource(),
                cycle.getStatus(),
                cycle.getStartedAt(),
                cycle.getFinishedAt(),
                cycle.getErrorMessage());
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
                savedCycle.getNicheName(),
                candidate.getOpportunityScore(),
                savedCycle.getTriggerSource(),
                savedCycle.getStatus(),
                savedCycle.getOriginalNicheName(),
                savedCycle.getNeutralNicheName(),
                savedCycle.getResearchMode(),
                savedCycle.getSolutionLanguageRiskScore(),
                candidate.getRoutineResearchStatus(),
                "Pesquisa de rotina iniciada para o próximo nicho CNAE com maior score pendente.");
    }
}
