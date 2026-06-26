package com.marketinghub.oprmcoletormei.nichocnae.v3.shared;

import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Base compartilhada para services canônicos de etapas NichoCNAE v3 sem assumir execução operacional. */
public abstract class OprmNichoCnaeV3StageServiceSupport {
    protected static final String STATUS_STARTED = "INICIADO";
    private static final int PENDING_LIMIT = 10;
    private static final Map<String, Integer> STAGE_ORDER = Map.ofEntries(
            Map.entry("cnae-intake", 1),
            Map.entry("persona-candidate-generator", 2),
            Map.entry("persona-tournament", 3),
            Map.entry("routine-query-planner", 4),
            Map.entry("source-searcher", 5),
            Map.entry("source-fetcher", 6),
            Map.entry("routine-signal-extractor", 7),
            Map.entry("daily-tasks-synthesizer", 8),
            Map.entry("quality-gate", 9),
            Map.entry("persona-routine-materializer", 10));
    private final OprmNichoCnaeV3StageExecutionRepository repository;
    private final OprmCnpjCnaeDimRepository cnaeRepository;
    private final String stageCode;

    /** Inicializa o suporte com repository canônico e código da etapa. */
    protected OprmNichoCnaeV3StageServiceSupport(
            OprmNichoCnaeV3StageExecutionRepository repository,
            OprmCnpjCnaeDimRepository cnaeRepository,
            String stageCode) {
        this.repository = repository;
        this.cnaeRepository = cnaeRepository;
        this.stageCode = stageCode;
    }

    /** Marca no cadastro de CNAE que o pipeline NichoCNAE v3 foi iniciado na etapa atual. */
    protected void markCnaePipelineStarted(String cnaeCode, String statusStarted) {
        OprmCnpjCnaeDim cnae = cnaeRepository.findById(cnaeCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CNAE não encontrado."));
        Instant now = Instant.now();
        cnae.setNichocnaePipelineStatus(statusStarted);
        cnae.setNichocnaeCurrentStageCode(stageCode);
        cnae.setNichocnaePipelineUpdatedAt(now);
        cnae.setUpdatedAt(now);
        cnaeRepository.save(cnae);
    }

    /** Cria uma execução pendente para a etapa canônica. */
    protected OprmNichoCnaeV3StageExecution doCreate(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        OprmNichoCnaeV3StageExecution execution = new OprmNichoCnaeV3StageExecution();
        execution.setJobId(jobId == null || jobId.isBlank() ? defaultJobId(cnaeCode) : jobId);
        execution.setCnaeCode(cnaeCode);
        execution.setStageCode(stageCode);
        execution.setInputPayload(inputPayload);
        execution.setAttemptNumber(attemptNumber == null ? 1 : attemptNumber);
        execution.setKnowledgeVersion(knowledgeVersion == null ? 1 : knowledgeVersion);
        return repository.save(execution);
    }

    /** Lista CNAEs iniciados na etapa corrente para o executor externo consumir pelo endpoint pending. */
    protected List<OprmCnpjCnaeDim> pendingCnaes() {
        return cnaeRepository.findByNichocnaeCurrentStageCodeAndNichocnaePipelineStatusOrderByNichocnaePipelineUpdatedAtAsc(
                stageCode, STATUS_STARTED, PageRequest.of(0, PENDING_LIMIT));
    }

    /** Recupera a execução mais recente do CNAE na etapa corrente para preservar o contrato de callback. */
    protected Optional<OprmNichoCnaeV3StageExecution> pendingExecution(OprmCnpjCnaeDim cnae) {
        return repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc(cnae.getCnaeCode(), stageCode);
    }

    /** Monta payload mínimo de entrada para um CNAE pendente quando não houver execução persistida. */
    protected String cnaeInputPayload(OprmCnpjCnaeDim cnae) {
        return "{\"cnaeCode\":\"" + cnae.getCnaeCode() + "\"}";
    }

    /** Monta jobId estável para uma pendência publicada diretamente pelo cadastro de CNAE. */
    protected String pendingJobId(OprmCnpjCnaeDim cnae) {
        return "nichocnae-v3-" + cnae.getCnaeCode();
    }

    /** Registra conclusão reportada pelo executor externo. */
    protected OprmNichoCnaeV3StageExecution doComplete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        OprmNichoCnaeV3StageExecution execution = find(stageExecutionId);
        execution.setStatus(OprmNichoCnaeV3StageExecutionStatus.COMPLETED);
        execution.setOutputPayload(outputPayload);
        execution.setNextStageCode(nextStageCode);
        execution.setUpdatedAt(Instant.now());
        OprmNichoCnaeV3StageExecution saved = repository.save(execution);
        createNextStageWhenAllowed(saved, outputPayload, nextStageCode);
        return saved;
    }

    /** Registra falha reportada pelo executor externo. */
    protected OprmNichoCnaeV3StageExecution doFail(Long stageExecutionId, String errorMessage) {
        OprmNichoCnaeV3StageExecution execution = find(stageExecutionId);
        execution.setStatus(OprmNichoCnaeV3StageExecutionStatus.FAILED);
        execution.setErrorMessage(errorMessage);
        execution.setUpdatedAt(Instant.now());
        return repository.save(execution);
    }

    /** Cria a próxima pendência quando o backend reconhecer avanço sequencial válido. */
    private void createNextStageWhenAllowed(OprmNichoCnaeV3StageExecution current, String outputPayload, String nextStageCode) {
        String normalizedNextStage = nextStageCode == null ? "" : nextStageCode.trim();
        if (requiresUserConfirmation(current, normalizedNextStage)
                || !isAllowedNextStage(normalizedNextStage)
                || repository.existsByJobIdAndStageCode(current.getJobId(), normalizedNextStage)) {
            return;
        }
        OprmNichoCnaeV3StageExecution next = new OprmNichoCnaeV3StageExecution();
        next.setJobId(current.getJobId());
        next.setCnaeCode(current.getCnaeCode());
        next.setStageCode(normalizedNextStage);
        next.setInputPayload(outputPayload);
        next.setAttemptNumber(current.getAttemptNumber());
        next.setKnowledgeVersion(current.getKnowledgeVersion());
        repository.save(next);
    }

    /** Bloqueia a etapa final até a confirmação explícita do usuário na tela. */
    private boolean requiresUserConfirmation(OprmNichoCnaeV3StageExecution current, String nextStageCode) {
        return "quality-gate".equals(current.getStageCode()) && "persona-routine-materializer".equals(nextStageCode);
    }

    /** Valida se a próxima etapa é conhecida e vem imediatamente depois da etapa atual. */
    private boolean isAllowedNextStage(String nextStageCode) {
        Integer currentOrder = STAGE_ORDER.get(stageCode);
        Integer nextOrder = STAGE_ORDER.get(nextStageCode);
        return currentOrder != null && nextOrder != null && nextOrder == currentOrder + 1;
    }

    /** Busca a execução garantindo que pertence à etapa atual. */
    private OprmNichoCnaeV3StageExecution find(Long stageExecutionId) {
        OprmNichoCnaeV3StageExecution execution = repository.findById(stageExecutionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execução v3 não encontrada."));
        if (!stageCode.equals(execution.getStageCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Execução v3 pertence a outra etapa.");
        }
        return execution;
    }

    /** Monta jobId simples quando a UI iniciar o fluxo por CNAE sem identificador prévio. */
    private String defaultJobId(String cnaeCode) {
        return "nichocnae-v3-" + cnaeCode + "-" + Instant.now().toEpochMilli();
    }
}
