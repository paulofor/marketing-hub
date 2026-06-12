package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.oprm.nichocnae.RoutineResearchNicheNameNormalizer;
import com.marketinghub.oprm.nichocnae.RoutineResearchNicheNameNormalizer.NormalizedNicheName;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.pending.RecordRoutineResearchOrchestratorPending;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.recent.RecordRoutineResearchOrchestratorRecent;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.reprocess.RecordRoutineResearchOrchestratorReprocessResult;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.runNext.RecordRoutineResearchOrchestratorResult;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsável por selecionar o próximo nicho CNAE com score alto e abrir o ciclo de pesquisa de rotina. */
@Service
public class BackendRoutineResearchOrchestratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendRoutineResearchOrchestratorService.class);
    private static final String ROUTINE_STATUS_PENDING = "PENDING";
    private static final String ROUTINE_STATUS_RUNNING = "RESEARCH_RUNNING";
    private static final String CYCLE_STATUS_RUNNING = "RUNNING";
    private static final String CYCLE_STATUS_FAILED = "FAILED";
    private static final String CYCLE_STATUS_NEEDS_MORE_RESEARCH = "NEEDS_MORE_RESEARCH";
    private static final String CYCLE_STATUS_NEEDS_MORE_MEI_RESEARCH = "NEEDS_MORE_MEI_RESEARCH";
    private static final String CYCLE_STATUS_OUTDATED_SOURCES = "OUTDATED_SOURCES";
    private static final String CYCLE_STATUS_TOO_CORPORATE = "TOO_CORPORATE";
    private static final String CYCLE_STATUS_SOLUTION_CONTAMINATED = "SOLUTION_CONTAMINATED";
    private static final String CYCLE_STATUS_GENERIC = "GENERIC";
    private static final String TRIGGER_SOURCE_AUTO_SCORE_QUEUE = "AUTO_SCORE_QUEUE";
    private static final String TRIGGER_SOURCE_MANUAL_REPROCESS = "MANUAL_REPROCESS";
    private static final String RESEARCH_MODE_ROUTINE_REALITY = "ROUTINE_REALITY_RESEARCH";

    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
    private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository;
    private final OprmNicheRoutineCardRepository routineCardRepository;
    private final RoutineResearchNicheNameNormalizer nicheNameNormalizer = new RoutineResearchNicheNameNormalizer();

    /** Inicializa o serviço com os repositórios canônicos usados pela etapa zero do pipeline. */
    public BackendRoutineResearchOrchestratorService(
            OprmNicheCandidateRepository nicheCandidateRepository,
            OprmRoutineResearchCycleRepository routineResearchCycleRepository,
            OprmMeiAudienceProfileRepository meiAudienceProfileRepository,
            OprmNicheRoutineCardRepository routineCardRepository) {
        this.nicheCandidateRepository = nicheCandidateRepository;
        this.routineResearchCycleRepository = routineResearchCycleRepository;
        this.meiAudienceProfileRepository = meiAudienceProfileRepository;
        this.routineCardRepository = routineCardRepository;
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

    /** Cria imediatamente um novo ciclo para tirar o usuário de ciclos falhos, fracos ou genéricos. */
    @Transactional
    public RecordRoutineResearchOrchestratorReprocessResult reprocessCycle(Long researchCycleId) {
        LOGGER.info(
                "Criando novo ciclo para nova pesquisa pela etapa zero OPRM nichocnae (researchCycleId={})",
                researchCycleId);
        OprmRoutineResearchCycle cycle = routineResearchCycleRepository
                .findById(researchCycleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Routine research cycle not found: " + researchCycleId));
        if (!isReprocessableStatus(cycle.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only failed, weak MEI audience, outdated, corporate, contaminated, generic or materialization-failed routine research cycles can be reprocessed: "
                            + researchCycleId);
        }
        OprmNicheCandidate candidate = nicheCandidateRepository
                .findById(cycle.getSourceNicheId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Niche candidate not found: " + cycle.getSourceNicheId()));
        String previousRoutineResearchStatus = candidate.getRoutineResearchStatus();
        Instant now = Instant.now();
        OprmRoutineResearchCycle newCycle = createCycle(candidate, now);
        newCycle.setTriggerSource(TRIGGER_SOURCE_MANUAL_REPROCESS);
        OprmRoutineResearchCycle savedCycle = routineResearchCycleRepository.save(newCycle);
        candidate.setRoutineResearchStatus(ROUTINE_STATUS_RUNNING);
        candidate.setLastRoutineResearchCycleId(savedCycle.getId());
        candidate.setUpdatedAt(now);
        OprmNicheCandidate savedCandidate = nicheCandidateRepository.save(candidate);
        LOGGER.info(
                "Novo ciclo criado imediatamente para reprocessamento OPRM nichocnae (previousResearchCycleId={}, newResearchCycleId={}, sourceNicheId={}, cnaeCode={}, previousCycleStatus={}, previousRoutineResearchStatus={}, routineResearchStatus={}, lastRoutineResearchCycleId={}, triggerSource={})",
                cycle.getId(),
                savedCycle.getId(),
                savedCandidate.getId(),
                savedCandidate.getCnaeCode(),
                cycle.getStatus(),
                previousRoutineResearchStatus,
                savedCandidate.getRoutineResearchStatus(),
                savedCandidate.getLastRoutineResearchCycleId(),
                savedCycle.getTriggerSource());
        return new RecordRoutineResearchOrchestratorReprocessResult(
                savedCycle.getId(),
                savedCandidate.getId(),
                savedCandidate.getCnaeCode(),
                savedCandidate.getCnaeDescription(),
                cycle.getStatus(),
                previousRoutineResearchStatus,
                savedCandidate.getRoutineResearchStatus(),
                savedCandidate.getLastRoutineResearchCycleId(),
                buildReprocessMessage(cycle.getStatus()));
    }

    /** Informa se o ciclo terminal permite nova pesquisa manual para sair de falha, material fraco ou falha de materialização. */
    private boolean isReprocessableStatus(String status) {
        return CYCLE_STATUS_FAILED.equals(status)
                || CYCLE_STATUS_NEEDS_MORE_RESEARCH.equals(status)
                || CYCLE_STATUS_NEEDS_MORE_MEI_RESEARCH.equals(status)
                || CYCLE_STATUS_OUTDATED_SOURCES.equals(status)
                || CYCLE_STATUS_TOO_CORPORATE.equals(status)
                || CYCLE_STATUS_SOLUTION_CONTAMINATED.equals(status)
                || CYCLE_STATUS_GENERIC.equals(status)
                || "ENRICHED_NICHE_FAILED".equals(status);
    }

    /** Monta mensagem operacional para explicar ao usuário por que o novo ciclo foi criado. */
    private String buildReprocessMessage(String previousStatus) {
        if (CYCLE_STATUS_FAILED.equals(previousStatus)) {
            return "Novo ciclo de pesquisa de rotina criado imediatamente para reprocessar o CNAE com falha.";
        }
        if (CYCLE_STATUS_NEEDS_MORE_RESEARCH.equals(previousStatus) || CYCLE_STATUS_NEEDS_MORE_MEI_RESEARCH.equals(previousStatus)) {
            return "Novo ciclo de pesquisa de rotina criado imediatamente para aprofundar um público MEI/autônomo que precisava de mais pesquisa.";
        }
        if (CYCLE_STATUS_OUTDATED_SOURCES.equals(previousStatus)) {
            return "Novo ciclo de pesquisa de rotina criado imediatamente para buscar fontes brasileiras mais recentes.";
        }
        if (CYCLE_STATUS_TOO_CORPORATE.equals(previousStatus)) {
            return "Novo ciclo de pesquisa de rotina criado imediatamente para focar no dono-operador MEI/autônomo, não em empresa estruturada.";
        }
        if (CYCLE_STATUS_SOLUTION_CONTAMINATED.equals(previousStatus)) {
            return "Novo ciclo de pesquisa de rotina criado imediatamente para remover contaminação por produto, oferta ou solução.";
        }
        if ("ENRICHED_NICHE_FAILED".equals(previousStatus)) {
            return "Novo ciclo de pesquisa de rotina criado imediatamente para refazer pelo front-end um CNAE aprovado cuja materialização falhou.";
        }
        return "Novo ciclo de pesquisa de rotina criado imediatamente para refazer um CNAE genérico ou sem material suficiente.";
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
        OprmMeiAudienceProfile audienceProfile = meiAudienceProfileRepository
                .findFirstByResearchCycleIdOrderByIdDesc(cycle.getId())
                .orElse(null);
        OprmNicheRoutineCard routineCard = routineCardRepository
                .findFirstByResearchCycleIdOrderByIdDesc(cycle.getId())
                .orElse(null);
        Long existingMarketNicheId = resolveExistingMarketNicheId(cycle);
        return new RecordRoutineResearchOrchestratorRecent(
                cycle.getId(),
                cycle.getSourceNicheId(),
                existingMarketNicheId,
                existingMarketNicheId != null,
                cycle.getCnaeCode(),
                cycle.getCnaeDescription(),
                cycle.getNicheName(),
                cycle.getOriginalNicheName(),
                cycle.getNeutralNicheName(),
                cycle.getResearchMode(),
                cycle.getSolutionLanguageRiskScore(),
                cycle.getSourceScore(),
                audienceProfile == null ? null : audienceProfile.getAudienceName(),
                audienceProfile == null ? null : audienceProfile.getAutonomousProfessionalFitScore(),
                audienceProfile == null ? null : audienceProfile.getSourceFreshnessScore(),
                audienceProfile == null ? null : audienceProfile.getOutdatedSourceRiskScore(),
                audienceProfile == null ? null : audienceProfile.getStructuredBusinessDriftRiskScore(),
                routineCard == null ? null : routineCard.getQualityStatus(),
                cycle.getTriggerSource(),
                cycle.getStatus(),
                cycle.getStartedAt(),
                cycle.getFinishedAt(),
                cycle.getErrorMessage());
    }

    /** Identifica o nicho de mercado já associado ao ciclo para evitar linguagem de criação duplicada na UI. */
    private Long resolveExistingMarketNicheId(OprmRoutineResearchCycle cycle) {
        Long candidateMarketNicheId = nicheCandidateRepository
                .findById(cycle.getSourceNicheId())
                .map(OprmNicheCandidate::getMarketNicheId)
                .orElse(null);
        if (candidateMarketNicheId != null) {
            return candidateMarketNicheId;
        }
        return routineResearchCycleRepository
                .findLatestMaterializedMarketNicheIdByResearchCycleId(cycle.getId(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
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
