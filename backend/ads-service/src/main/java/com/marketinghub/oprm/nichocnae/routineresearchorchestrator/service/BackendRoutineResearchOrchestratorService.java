package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
import com.marketinghub.oprm.nichocnae.RoutineResearchNicheNameNormalizer;
import com.marketinghub.oprm.nichocnae.RoutineResearchNicheNameNormalizer.NormalizedNicheName;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.pending.RecordRoutineResearchOrchestratorPending;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.recent.RecordRoutineResearchOrchestratorRecent;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.reprocess.RecordRoutineResearchOrchestratorReprocessRequest;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.reprocess.RecordRoutineResearchOrchestratorReprocessResult;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.runNext.RecordRoutineResearchOrchestratorResult;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheResearchSeedRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmResearchQueryRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
    private static final String CYCLE_STATUS_NEEDS_EXECUTOR_ROUTINE_EVIDENCE = "NEEDS_EXECUTOR_ROUTINE_EVIDENCE";
    private static final String CYCLE_STATUS_CANCELLED_BY_MANUAL_RESTART = "CANCELLED_BY_MANUAL_RESTART";
    private static final String CYCLE_STATUS_STALLED = "STALLED";
    private static final String TRIGGER_SOURCE_AUTO_SCORE_QUEUE = "AUTO_SCORE_QUEUE";
    private static final String TRIGGER_SOURCE_MANUAL_REPROCESS = "MANUAL_REPROCESS";
    private static final String TRIGGER_SOURCE_MANUAL_CNAE_DETAIL = "MANUAL_CNAE_DETAIL";
    private static final String TRIGGER_SOURCE_AUTO_QUALITY_REPROCESS = "AUTO_QUALITY_REPROCESS";
    private static final String RESEARCH_MODE_ROUTINE_REALITY = "ROUTINE_REALITY_RESEARCH";

    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
    private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository;
    private final OprmNicheRoutineCardRepository routineCardRepository;
    private final OprmNicheResearchSeedRepository seedRepository;
    private final OprmResearchQueryRepository researchQueryRepository;
    private final OprmSourceCandidateRepository sourceCandidateRepository;
    private final OprmSourceSnapshotRepository sourceSnapshotRepository;
    private final OprmExtractedSignalRepository extractedSignalRepository;
    private final RoutineResearchNicheNameNormalizer nicheNameNormalizer = new RoutineResearchNicheNameNormalizer();

    /** Inicializa o serviço com os repositórios canônicos usados pela etapa zero do pipeline. */
    public BackendRoutineResearchOrchestratorService(
            OprmNicheCandidateRepository nicheCandidateRepository,
            OprmRoutineResearchCycleRepository routineResearchCycleRepository,
            OprmMeiAudienceProfileRepository meiAudienceProfileRepository,
            OprmNicheRoutineCardRepository routineCardRepository,
            OprmNicheResearchSeedRepository seedRepository,
            OprmResearchQueryRepository researchQueryRepository,
            OprmSourceCandidateRepository sourceCandidateRepository,
            OprmSourceSnapshotRepository sourceSnapshotRepository,
            OprmExtractedSignalRepository extractedSignalRepository) {
        this.nicheCandidateRepository = nicheCandidateRepository;
        this.routineResearchCycleRepository = routineResearchCycleRepository;
        this.meiAudienceProfileRepository = meiAudienceProfileRepository;
        this.routineCardRepository = routineCardRepository;
        this.seedRepository = seedRepository;
        this.researchQueryRepository = researchQueryRepository;
        this.sourceCandidateRepository = sourceCandidateRepository;
        this.sourceSnapshotRepository = sourceSnapshotRepository;
        this.extractedSignalRepository = extractedSignalRepository;
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

    /** Lista jobs por status para o executor decidir reprocessamentos com aprendizado da tentativa anterior. */
    @Transactional(readOnly = true)
    public List<RecordRoutineResearchOrchestratorRecent> listJobsByStatus(List<String> statuses, int limit) {
        List<String> normalizedStatuses = normalizeStatusFilter(statuses);
        int safeLimit = Math.max(1, Math.min(limit, 50));
        LOGGER.info(
                "Listando jobs OPRM nichocnae por status para executor (statuses={}, requestedLimit={}, safeLimit={})",
                normalizedStatuses,
                limit,
                safeLimit);
        List<RecordRoutineResearchOrchestratorRecent> jobs = routineResearchCycleRepository
                .findByStatusInOrderByStartedAtAsc(normalizedStatuses, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toRecentProcessed)
                .toList();
        LOGGER.info("Jobs OPRM nichocnae por status retornados para executor (count={})", jobs.size());
        return jobs;
    }

    /** Reabre o mesmo job e limpa os artefatos das etapas que serão executadas novamente com novo input. */
    @Transactional
    public RecordRoutineResearchOrchestratorReprocessResult reprocessCycle(
            Long researchCycleId, RecordRoutineResearchOrchestratorReprocessRequest request) {
        LOGGER.info(
                "Reabrindo o mesmo job para reexecução de etapas OPRM nichocnae (researchCycleId={})",
                researchCycleId);
        OprmRoutineResearchCycle cycle = routineResearchCycleRepository
                .findById(researchCycleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Routine research cycle not found: " + researchCycleId));
        if (!isReprocessableStatus(cycle.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only failed, stalled, weak MEI audience, outdated, corporate, contaminated, generic or materialization-failed routine research cycles can be reprocessed: "
                            + researchCycleId);
        }
        OprmNicheCandidate candidate = nicheCandidateRepository
                .findById(cycle.getSourceNicheId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Niche candidate not found: " + cycle.getSourceNicheId()));
        String previousCycleStatus = cycle.getStatus();
        String previousRoutineResearchStatus = candidate.getRoutineResearchStatus();
        OprmNicheRoutineCard previousQualityCard = routineCardRepository
                .findFirstByResearchCycleIdOrderByIdDesc(cycle.getId())
                .orElse(null);
        String triggerSource = resolveReprocessTriggerSource(request);
        Instant now = Instant.now();
        clearArtifactsForStageRerun(cycle.getId());
        reopenCycleForStageRerun(cycle, previousCycleStatus, previousQualityCard, triggerSource, now);
        OprmRoutineResearchCycle savedCycle = routineResearchCycleRepository.save(cycle);
        candidate.setRoutineResearchStatus(ROUTINE_STATUS_RUNNING);
        candidate.setLastRoutineResearchCycleId(savedCycle.getId());
        candidate.setUpdatedAt(now);
        OprmNicheCandidate savedCandidate = nicheCandidateRepository.save(candidate);
        LOGGER.info(
                "Mesmo job reaberto para reexecução OPRM nichocnae (researchCycleId={}, sourceNicheId={}, cnaeCode={}, previousCycleStatus={}, previousRoutineResearchStatus={}, routineResearchStatus={}, lastRoutineResearchCycleId={}, triggerSource={})",
                savedCycle.getId(),
                savedCandidate.getId(),
                savedCandidate.getCnaeCode(),
                previousCycleStatus,
                previousRoutineResearchStatus,
                savedCandidate.getRoutineResearchStatus(),
                savedCandidate.getLastRoutineResearchCycleId(),
                savedCycle.getTriggerSource());
        return new RecordRoutineResearchOrchestratorReprocessResult(
                savedCycle.getId(),
                savedCandidate.getId(),
                savedCandidate.getCnaeCode(),
                savedCandidate.getCnaeDescription(),
                previousCycleStatus,
                previousRoutineResearchStatus,
                savedCandidate.getRoutineResearchStatus(),
                savedCandidate.getLastRoutineResearchCycleId(),
                buildReprocessMessage(previousCycleStatus));
    }

    /** Remove artefatos derivados para que as etapas do próprio job sejam executadas novamente com novo input. */
    private void clearArtifactsForStageRerun(Long researchCycleId) {
        meiAudienceProfileRepository.deleteByResearchCycleId(researchCycleId);
        routineCardRepository.deleteByResearchCycleId(researchCycleId);
        extractedSignalRepository.deleteByResearchCycleId(researchCycleId);
        sourceSnapshotRepository.deleteByResearchCycleId(researchCycleId);
        sourceCandidateRepository.deleteByResearchCycleId(researchCycleId);
        researchQueryRepository.deleteByResearchCycleId(researchCycleId);
        seedRepository.deleteByResearchCycleId(researchCycleId);
    }

    /** Atualiza o ciclo existente para que a fila volte à primeira etapa reexecutável sem criar outro job. */
    private void reopenCycleForStageRerun(
            OprmRoutineResearchCycle cycle,
            String previousCycleStatus,
            OprmNicheRoutineCard previousQualityCard,
            String triggerSource,
            Instant now) {
        cycle.setTriggerSource(triggerSource);
        cycle.setStatus(CYCLE_STATUS_RUNNING);
        cycle.setTotalQueries(0);
        cycle.setTotalSourceCandidates(0);
        cycle.setTotalSourceSnapshots(0);
        cycle.setTotalExtractedSignals(0);
        cycle.setFinishedAt(null);
        cycle.setErrorMessage(buildStageRerunLearningNote(previousCycleStatus, previousQualityCard, triggerSource));
        cycle.setUpdatedAt(now);
    }

    /** Monta a nota auditável que mantém o aprendizado do gate mesmo após limpar artefatos reexecutáveis. */
    private String buildStageRerunLearningNote(
            String previousCycleStatus, OprmNicheRoutineCard previousQualityCard, String triggerSource) {
        StringBuilder note = new StringBuilder()
                .append("Reexecução de etapas do mesmo job solicitada por ")
                .append(triggerSource)
                .append(" após status ")
                .append(previousCycleStatus)
                .append(". O próximo seed deve usar o aprendizado do gate anterior.");
        if (previousQualityCard != null) {
            note.append(" previousQualityStatus=")
                    .append(previousQualityCard.getQualityStatus())
                    .append("; previousQualityNotes=")
                    .append(previousQualityCard.getQualityNotes());
        }
        return note.toString();
    }

    /** Normaliza a origem solicitada pelo executor externo sem permitir valores arbitrários no histórico. */
    private String resolveReprocessTriggerSource(RecordRoutineResearchOrchestratorReprocessRequest request) {
        if (request != null && TRIGGER_SOURCE_AUTO_QUALITY_REPROCESS.equals(request.triggerSource())) {
            return TRIGGER_SOURCE_AUTO_QUALITY_REPROCESS;
        }
        return TRIGGER_SOURCE_MANUAL_REPROCESS;
    }

    /** Informa se o ciclo terminal permite nova pesquisa manual para sair de falha, material fraco ou falha de materialização. */
    private boolean isReprocessableStatus(String status) {
        return CYCLE_STATUS_FAILED.equals(status)
                || CYCLE_STATUS_STALLED.equals(status)
                || CYCLE_STATUS_NEEDS_MORE_RESEARCH.equals(status)
                || CYCLE_STATUS_NEEDS_MORE_MEI_RESEARCH.equals(status)
                || CYCLE_STATUS_OUTDATED_SOURCES.equals(status)
                || CYCLE_STATUS_TOO_CORPORATE.equals(status)
                || CYCLE_STATUS_SOLUTION_CONTAMINATED.equals(status)
                || CYCLE_STATUS_GENERIC.equals(status)
                || CYCLE_STATUS_NEEDS_EXECUTOR_ROUTINE_EVIDENCE.equals(status)
                || "ENRICHED_NICHE_FAILED".equals(status);
    }

    /** Normaliza o filtro de status e bloqueia consultas amplas sem status explícito. */
    private List<String> normalizeStatusFilter(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one status is required");
        }
        List<String> normalizedStatuses = statuses.stream()
                .filter(status -> status != null && !status.isBlank())
                .map(status -> status.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalizedStatuses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one status is required");
        }
        return normalizedStatuses;
    }

    /** Monta mensagem operacional para explicar ao usuário por que o mesmo job foi reaberto. */
    private String buildReprocessMessage(String previousStatus) {
        if (CYCLE_STATUS_FAILED.equals(previousStatus)) {
            return "Mesmo job reaberto para reexecutar as etapas de pesquisa de rotina após falha.";
        }
        if (CYCLE_STATUS_STALLED.equals(previousStatus)) {
            return "Mesmo job reaberto para recuperar etapas paradas sem progresso.";
        }
        if (CYCLE_STATUS_NEEDS_MORE_RESEARCH.equals(previousStatus) || CYCLE_STATUS_NEEDS_MORE_MEI_RESEARCH.equals(previousStatus)) {
            return "Mesmo job reaberto para reexecutar etapas com mais foco no público MEI/autônomo.";
        }
        if (CYCLE_STATUS_OUTDATED_SOURCES.equals(previousStatus)) {
            return "Mesmo job reaberto para reexecutar etapas buscando fontes brasileiras mais recentes.";
        }
        if (CYCLE_STATUS_TOO_CORPORATE.equals(previousStatus)) {
            return "Mesmo job reaberto para reexecutar etapas com foco em dono-operador MEI/autônomo, não empresa estruturada.";
        }
        if (CYCLE_STATUS_SOLUTION_CONTAMINATED.equals(previousStatus)) {
            return "Mesmo job reaberto para reexecutar etapas removendo contaminação por produto, oferta ou solução.";
        }
        if ("ENRICHED_NICHE_FAILED".equals(previousStatus)) {
            return "Mesmo job reaberto para reexecutar etapas após falha de materialização.";
        }
        return "Mesmo job reaberto para reexecutar etapas após resultado genérico ou sem material suficiente.";
    }

    /** Executa a etapa zero para o CNAE escolhido, encerrando ciclos abertos antes de criar uma execução nova. */
    @Transactional
    public RecordRoutineResearchOrchestratorResult runForCnae(String cnaeCode) {
        LOGGER.info("Iniciando etapa zero OPRM nichocnae por CNAE manual com reinício forçado (cnaeCode={})", cnaeCode);
        List<OprmNicheCandidate> candidates = nicheCandidateRepository.findManualRoutineResearchCandidateByCnaeCode(
                cnaeCode, PageRequest.of(0, 1));
        if (candidates.isEmpty()) {
            LOGGER.info("Nenhum candidato com score encontrado para acionamento manual do CNAE (cnaeCode={})", cnaeCode);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Não existe candidato de nicho com score para iniciar um novo pipeline deste CNAE: " + cnaeCode);
        }
        finishOpenCyclesForManualRestart(cnaeCode);
        return startCycleForCandidate(candidates.getFirst(), TRIGGER_SOURCE_MANUAL_CNAE_DETAIL);
    }

    /** Encerra ciclos ainda abertos do CNAE para impedir que execuções antigas continuem concorrendo com o novo ciclo manual. */
    private void finishOpenCyclesForManualRestart(String cnaeCode) {
        List<OprmRoutineResearchCycle> openCycles =
                routineResearchCycleRepository.findOpenCyclesByCnaeCodeForUpdate(cnaeCode);
        if (openCycles == null || openCycles.isEmpty()) {
            LOGGER.info(
                    "Nenhum ciclo aberto encontrado para encerramento antes do reinício manual do CNAE (cnaeCode={})",
                    cnaeCode);
            return;
        }
        Instant now = Instant.now();
        openCycles.forEach(cycle -> {
            cycle.setStatus(CYCLE_STATUS_CANCELLED_BY_MANUAL_RESTART);
            cycle.setFinishedAt(now);
            cycle.setUpdatedAt(now);
            cycle.setErrorMessage("Ciclo encerrado automaticamente para reinício manual completo do CNAE " + cnaeCode + ".");
        });
        routineResearchCycleRepository.saveAll(openCycles);
        LOGGER.info(
                "Ciclos abertos encerrados antes do reinício manual OPRM nichocnae (cnaeCode={}, count={}, status={})",
                cnaeCode,
                openCycles.size(),
                CYCLE_STATUS_CANCELLED_BY_MANUAL_RESTART);
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

        return startCycleForCandidate(candidates.getFirst(), TRIGGER_SOURCE_AUTO_SCORE_QUEUE);
    }

    /** Cria ciclo de pesquisa para um candidato já selecionado e atualiza o status operacional do nicho. */
    private RecordRoutineResearchOrchestratorResult startCycleForCandidate(OprmNicheCandidate candidate, String triggerSource) {
        LOGGER.info(
                "Candidato selecionado para etapa zero OPRM nichocnae (sourceNicheId={}, cnaeCode={}, nicheName={}, score={}, routineStatus={}, lastRoutineResearchCycleId={}, triggerSource={})",
                candidate.getId(),
                candidate.getCnaeCode(),
                candidate.getCandidateNicheName(),
                candidate.getOpportunityScore(),
                candidate.getRoutineResearchStatus(),
                candidate.getLastRoutineResearchCycleId(),
                triggerSource);
        Instant now = Instant.now();
        try {
            OprmRoutineResearchCycle cycle = createCycle(candidate, now);
            cycle.setTriggerSource(triggerSource);
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
                    "Erro ao executar etapa zero do OPRM nichocnae (sourceNicheId={}, cnaeCode={}, nicheName={}, score={}, triggerSource={})",
                    candidate.getId(),
                    candidate.getCnaeCode(),
                    candidate.getCandidateNicheName(),
                    candidate.getOpportunityScore(),
                    triggerSource,
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
