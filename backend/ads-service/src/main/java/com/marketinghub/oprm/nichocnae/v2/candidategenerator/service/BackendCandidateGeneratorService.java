package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteraction;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.cancelJob.CandidateGeneratorCancelJobResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.createStageExecution.CandidateGeneratorCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.detailJob.CandidateGeneratorJobDetailResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.detailJob.CandidateGeneratorJobStageStep;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.listCnaeJobs.CandidateGeneratorCnaeJobSummary;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.listCnaeJobs.CandidateGeneratorCnaeJobsResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.pending.CandidateGeneratorPendingResponse;
import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteractionRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Expõe contratos de leitura/escrita do backend para a etapa candidate-generator do pipeline NichoCNAE v2. */
@Service
public class BackendCandidateGeneratorService {
    private static final String STAGE_CODE = "candidate-generator";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final List<String> DECISION_KEYS = List.of(
            "tournamentDecision",
            "gateDecision",
            "materializationDecision",
            "sourceFetchDecision",
            "planDecision",
            "safetyDecision",
            "qualityStatus",
            "decision");
    private static final List<OprmNichoCnaeV2StageExecutionStatus> OPEN_STATUSES = List.of(
            OprmNichoCnaeV2StageExecutionStatus.PENDING,
            OprmNichoCnaeV2StageExecutionStatus.RUNNING,
            OprmNichoCnaeV2StageExecutionStatus.TECHNICAL_RETRY_SCHEDULED);
    private static final List<String> COST_KEYS = List.of(
            "aiCostUsd",
            "totalAiCostUsd",
            "estimatedAiCostUsd",
            "costUsd",
            "estimatedCostUsd");
    private static final List<String> MARKET_NICHE_ID_KEYS = List.of(
            "marketNicheId",
            "market_niche_id",
            "existingMarketNicheId",
            "createdMarketNicheId");

    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;
    private final OprmNichoCnaeV2OpenAiInteractionRepository openAiInteractionRepository;
    private final boolean v2Enabled;
    private final boolean materializationEnabled;

    /** Inicializa o service com repositórios canônicos e feature flags de calibração da v2. */
    @Autowired
    public BackendCandidateGeneratorService(
            OprmNicheCandidateRepository nicheCandidateRepository,
            OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository,
            OprmNichoCnaeV2OpenAiInteractionRepository openAiInteractionRepository,
            @Value("${oprm.nichocnae.v2.enabled:false}") boolean v2Enabled,
            @Value("${oprm.nichocnae.v2.materialization-enabled:false}") boolean materializationEnabled) {
        this.nicheCandidateRepository = nicheCandidateRepository;
        this.stageExecutionRepository = stageExecutionRepository;
        this.openAiInteractionRepository = openAiInteractionRepository;
        this.v2Enabled = v2Enabled;
        this.materializationEnabled = materializationEnabled;
    }

    /** Mantém compatibilidade com testes unitários que não exercem auditoria OpenAI. */
    public BackendCandidateGeneratorService(
            OprmNicheCandidateRepository nicheCandidateRepository,
            OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository,
            boolean v2Enabled,
            boolean materializationEnabled) {
        this(nicheCandidateRepository, stageExecutionRepository, null, v2Enabled, materializationEnabled);
    }

    /** Lista pendências disponíveis para consumo canônico pelo executor OPRM NichoCNAE v2. */
    @Transactional
    public List<CandidateGeneratorPendingResponse> pending() {
        if (!v2Enabled) {
            return List.of();
        }
        ensureInitialPendingExecution();
        return stageExecutionRepository
                .findByStageCodeAndStatusOrderByCreatedAtAsc(
                        STAGE_CODE, OprmNichoCnaeV2StageExecutionStatus.PENDING, PageRequest.of(0, 1))
                .stream()
                .map(this::toPendingResponse)
                .toList();
    }

    /** Grava um novo job inicial da v2 para o CNAE escolhido, deixando a execução para o módulo externo. */
    @Transactional
    public CandidateGeneratorCreateResponse createForCnae(String cnaeCode) {
        if (!v2Enabled) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "NichoCNAE v2 is disabled");
        }
        if (stageExecutionRepository.countByCnaeCodeAndStatusIn(cnaeCode, OPEN_STATUSES) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe job NichoCNAE v2 aberto para este CNAE. Encerre ou aguarde o job atual antes de iniciar outro.");
        }
        OprmNicheCandidate candidate = nicheCandidateRepository
                .findManualRoutineResearchCandidateByCnaeCode(cnaeCode, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No scored NichoCNAE candidate found for CNAE: " + cnaeCode));
        long existingExecutions = stageExecutionRepository.countBySourceNicheIdAndStageCode(candidate.getId(), STAGE_CODE);
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(createInitialExecution(candidate, existingExecutions + 1));
        return new CandidateGeneratorCreateResponse(
                String.valueOf(saved.getId()),
                saved.getJobId(),
                saved.getCnaeCode(),
                saved.getSourceNicheId(),
                saved.getAttemptNumber(),
                saved.getTechnicalRetryNumber(),
                saved.getKnowledgeVersion(),
                saved.getMaterializationEnabled(),
                saved.getStatus().name());
    }

    /** Lista jobs do CNAE agrupados em abertos e encerrados para a tela administrativa. */
    @Transactional(readOnly = true)
    public CandidateGeneratorCnaeJobsResponse listJobsForCnae(String cnaeCode) {
        Map<String, List<OprmNichoCnaeV2StageExecution>> executionsByJob = new LinkedHashMap<>();
        stageExecutionRepository.findByCnaeCodeOrderByUpdatedAtDesc(cnaeCode).forEach(execution ->
                executionsByJob.computeIfAbsent(execution.getJobId(), ignored -> new ArrayList<>()).add(execution));
        List<CandidateGeneratorCnaeJobSummary> openJobs = new ArrayList<>();
        List<CandidateGeneratorCnaeJobSummary> completedJobs = new ArrayList<>();
        executionsByJob.values().forEach(executions -> {
            CandidateGeneratorCnaeJobSummary summary = toJobSummary(executions);
            if (hasOpenExecution(executions)) {
                openJobs.add(summary);
            } else {
                completedJobs.add(summary);
            }
        });
        Comparator<CandidateGeneratorCnaeJobSummary> newestFirst = Comparator
                .comparing(CandidateGeneratorCnaeJobSummary::updatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
        openJobs.sort(newestFirst);
        completedJobs.sort(newestFirst);
        BigDecimal cnaeAiCostUsd = executionsByJob.values().stream()
                .map(executions -> sumAiCostUsd(executions.getFirst().getJobId(), executions))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean cnaeUsedAi = cnaeAiCostUsd.compareTo(BigDecimal.ZERO) > 0
                || executionsByJob.keySet().stream().anyMatch(this::hasAuditedOpenAiInteraction)
                || executionsByJob.values().stream().flatMap(List::stream).anyMatch(this::hasAiUsageSignal);
        return new CandidateGeneratorCnaeJobsResponse(cnaeCode, cnaeAiCostUsd, cnaeUsedAi, openJobs, completedJobs);
    }

    /** Cancela manualmente execuções abertas de um job preso para liberar novo ciclo do CNAE. */
    @Transactional
    public CandidateGeneratorCancelJobResponse cancelJob(String jobId) {
        List<OprmNichoCnaeV2StageExecution> executions = stageExecutionRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        if (executions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "NichoCNAE v2 job not found: " + jobId);
        }
        List<OprmNichoCnaeV2StageExecution> openExecutions = stageExecutionRepository.findByJobIdAndStatusIn(jobId, OPEN_STATUSES);
        if (openExecutions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este job NichoCNAE v2 não está aberto e não precisa ser cancelado.");
        }
        Instant now = Instant.now();
        openExecutions.forEach(execution -> {
            execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.CANCELED);
            execution.setErrorMessage("Cancelado manualmente pelo usuário para liberar novo job do CNAE.");
            execution.setUpdatedAt(now);
        });
        stageExecutionRepository.saveAll(openExecutions);
        String cnaeCode = executions.getFirst().getCnaeCode();
        return new CandidateGeneratorCancelJobResponse(
                jobId,
                cnaeCode,
                openExecutions.size(),
                OprmNichoCnaeV2StageExecutionStatus.CANCELED.name(),
                "Job cancelado. O CNAE está liberado para iniciar uma nova execução v2.",
                now);
    }

    /** Detalha as etapas persistidas de um job para a tela explicar o caminho até o fracasso ou sucesso. */
    @Transactional(readOnly = true)
    public CandidateGeneratorJobDetailResponse detailJob(String jobId) {
        List<OprmNichoCnaeV2StageExecution> executions = stageExecutionRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        if (executions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "NichoCNAE v2 job not found: " + jobId);
        }
        CandidateGeneratorCnaeJobSummary summary = toJobSummary(executions);
        return new CandidateGeneratorJobDetailResponse(
                summary.jobId(),
                summary.cnaeCode(),
                summary.status(),
                summary.finalDecision(),
                summary.finalDecisionLabel(),
                summary.finalDecisionReason(),
                summary.outcomeStatus(),
                summary.outcomeMessage(),
                executions.stream().map(this::toJobStageStep).toList());
    }

    /** Registra conclusão da etapa persistindo a próxima etapa informada pelo executor externo. */
    @Transactional
    public CandidateGeneratorCompletionResponse complete(
            Long stageExecutionId, CandidateGeneratorCompletionRequest request) {
        OprmNichoCnaeV2StageExecution execution = findExecution(stageExecutionId);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        execution.setOutputPayload(request == null ? null : request.outputPayload());
        execution.setNextStageCode(request == null ? null : request.nextStageCode());
        execution.setUpdatedAt(Instant.now());
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(execution);
        recordOpenAiInteractions(saved, request == null ? null : request.openAiInteractions());
        return new CandidateGeneratorCompletionResponse(
                String.valueOf(saved.getId()),
                saved.getStatus().name(),
                saved.getNextStageCode(),
                saved.getMaterializationEnabled());
    }

    /** Registra falha e cria nova execução somente quando a falha for retry técnico de infraestrutura. */
    @Transactional
    public CandidateGeneratorFailureResponse fail(Long stageExecutionId, CandidateGeneratorFailureRequest request) {
        OprmNichoCnaeV2StageExecution execution = findExecution(stageExecutionId);
        OprmNichoCnaeV2FailureType failureType = request == null || request.failureType() == null
                ? OprmNichoCnaeV2FailureType.VALIDATION
                : request.failureType();
        execution.setFailureType(failureType);
        execution.setErrorMessage(request == null ? null : request.errorMessage());
        execution.setInputPayload(request == null ? execution.getInputPayload() : request.inputPayload());
        execution.setUpdatedAt(Instant.now());
        if (failureType == OprmNichoCnaeV2FailureType.INFRASTRUCTURE) {
            execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.TECHNICAL_RETRY_SCHEDULED);
            OprmNichoCnaeV2StageExecution retry = createTechnicalRetry(execution);
            stageExecutionRepository.save(execution);
            OprmNichoCnaeV2StageExecution savedRetry = stageExecutionRepository.save(retry);
            return new CandidateGeneratorFailureResponse(
                    String.valueOf(execution.getId()),
                    execution.getStatus().name(),
                    String.valueOf(savedRetry.getId()),
                    savedRetry.getAttemptNumber(),
                    savedRetry.getTechnicalRetryNumber());
        }
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.FAILED);
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(execution);
        return new CandidateGeneratorFailureResponse(
                String.valueOf(saved.getId()),
                saved.getStatus().name(),
                null,
                saved.getAttemptNumber(),
                saved.getTechnicalRetryNumber());
    }

    /** Converte uma execução persistida em item de linha cronológico do relatório de job. */
    private CandidateGeneratorJobStageStep toJobStageStep(OprmNichoCnaeV2StageExecution execution) {
        return new CandidateGeneratorJobStageStep(
                String.valueOf(execution.getId()),
                execution.getStageCode(),
                execution.getStatus().name(),
                execution.getFailureType() == null ? null : execution.getFailureType().name(),
                execution.getAttemptNumber(),
                execution.getTechnicalRetryNumber(),
                execution.getKnowledgeVersion(),
                execution.getMaterializationEnabled(),
                execution.getInputPayload(),
                execution.getOutputPayload(),
                execution.getErrorMessage(),
                execution.getNextStageCode(),
                execution.getCreatedAt(),
                execution.getUpdatedAt());
    }

    /** Monta resumo de job usando a etapa aberta mais recente ou, se não existir, a última etapa atualizada. */
    private CandidateGeneratorCnaeJobSummary toJobSummary(List<OprmNichoCnaeV2StageExecution> executions) {
        OprmNichoCnaeV2StageExecution lastExecution = executions.stream()
                .max(Comparator.comparing(OprmNichoCnaeV2StageExecution::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow();
        OprmNichoCnaeV2StageExecution currentExecution = executions.stream()
                .filter(this::isOpenExecution)
                .max(Comparator.comparing(OprmNichoCnaeV2StageExecution::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(lastExecution);
        String jobStatus = hasOpenExecution(executions) ? "OPEN" : "COMPLETED";
        Map<String, Object> lastOutput = parsePayload(lastExecution.getOutputPayload());
        String finalDecision = decisionFrom(lastOutput, lastExecution, executions);
        String finalDecisionReason = reasonForDecision(finalDecision, lastOutput, lastExecution, executions);
        String marketNicheId = marketNicheIdFrom(executions);
        BigDecimal aiCostUsd = sumAiCostUsd(lastExecution.getJobId(), executions);
        boolean usedAi = aiCostUsd.compareTo(BigDecimal.ZERO) > 0
                || hasAuditedOpenAiInteraction(lastExecution.getJobId())
                || executions.stream().anyMatch(this::hasAiUsageSignal);
        return new CandidateGeneratorCnaeJobSummary(
                lastExecution.getJobId(),
                lastExecution.getCnaeCode(),
                jobStatus,
                isOpenExecution(currentExecution) ? currentExecution.getStageCode() : null,
                lastExecution.getStageCode(),
                lastExecution.getStatus().name(),
                currentExecution.getAttemptNumber(),
                currentExecution.getTechnicalRetryNumber(),
                currentExecution.getKnowledgeVersion(),
                currentExecution.getMaterializationEnabled(),
                finalDecision,
                labelForDecision(finalDecision, lastExecution),
                finalDecisionReason,
                outcomeStatus(finalDecision, lastExecution),
                outcomeMessage(finalDecision, finalDecisionReason, marketNicheId, lastExecution),
                actionLabel(finalDecision, marketNicheId, lastExecution),
                actionUrl(finalDecision, lastExecution.getCnaeCode(), marketNicheId),
                usedAi,
                aiCostUsd,
                executions.stream().map(OprmNichoCnaeV2StageExecution::getCreatedAt).min(Comparator.naturalOrder()).orElse(null),
                lastExecution.getUpdatedAt());
    }

    /** Extrai a decisão funcional persistida no output da última etapa para evitar conclusão sem explicação ao usuário. */
    private String decisionFrom(
            Map<String, Object> output,
            OprmNichoCnaeV2StageExecution lastExecution,
            List<OprmNichoCnaeV2StageExecution> executions) {
        return DECISION_KEYS.stream()
                .map(output::get)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .findFirst()
                .or(() -> previousDecisionFrom(executions))
                .orElseGet(() -> lastExecution.getFailureType() == null
                        ? lastExecution.getStatus().name()
                        : lastExecution.getFailureType().name());
    }

    /** Reaproveita a última decisão funcional anterior quando a etapa final apenas encerra o reprocessamento. */
    private Optional<String> previousDecisionFrom(List<OprmNichoCnaeV2StageExecution> executions) {
        return executions.stream()
                .sorted(Comparator.comparing(OprmNichoCnaeV2StageExecution::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .map(execution -> parsePayload(execution.getOutputPayload()))
                .flatMap(output -> DECISION_KEYS.stream()
                        .map(output::get)
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .filter(value -> !value.isBlank()))
                .findFirst();
    }

    /** Traduz decisões funcionais críticas em rótulos claros de negócio para a tela administrativa. */
    private String labelForDecision(String decision, OprmNichoCnaeV2StageExecution lastExecution) {
        if ("NO_VIABLE_SUBNICHE".equals(decision)) {
            return "Encerrado sem subnicho viável";
        }
        if ("FINALISTS_SELECTED".equals(decision)) {
            return "Finalistas selecionados";
        }
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.CANCELED) {
            return "Cancelado pelo usuário";
        }
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.FAILED) {
            return "Falha encerrada";
        }
        return decision;
    }

    /** Monta uma explicação curta da decisão final usando contagens estruturadas persistidas pelo executor. */
    private String reasonForDecision(
            String decision,
            Map<String, Object> output,
            OprmNichoCnaeV2StageExecution lastExecution,
            List<OprmNichoCnaeV2StageExecution> executions) {
        if ("NO_VIABLE_SUBNICHE".equals(decision)) {
            Map<String, Object> decisionOutput = output.containsKey("candidateCount")
                    ? output
                    : latestOutputWithKey(executions, "candidateCount");
            return "O torneio terminou sem finalistas viáveis; candidatos="
                    + numberText(decisionOutput.get("candidateCount"))
                    + ", finalistas="
                    + numberText(decisionOutput.get("finalistCount"))
                    + ".";
        }
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.CANCELED) {
            return lastExecution.getErrorMessage();
        }
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.FAILED) {
            return lastExecution.getErrorMessage();
        }
        return null;
    }

    /** Localiza o output mais recente que contém a chave necessária para explicar a decisão ao usuário. */
    private Map<String, Object> latestOutputWithKey(List<OprmNichoCnaeV2StageExecution> executions, String key) {
        return executions.stream()
                .sorted(Comparator.comparing(OprmNichoCnaeV2StageExecution::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .map(execution -> parsePayload(execution.getOutputPayload()))
                .filter(output -> output.containsKey(key))
                .findFirst()
                .orElse(Map.of());
    }

    /** Classifica o resultado final em linguagem simples para a tela administrativa. */
    private String outcomeStatus(String decision, OprmNichoCnaeV2StageExecution lastExecution) {
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.FAILED
                || lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.CANCELED
                || "NO_VIABLE_SUBNICHE".equals(decision)) {
            return "FAILURE";
        }
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.COMPLETED) {
            return "SUCCESS";
        }
        return "IN_PROGRESS";
    }

    /** Monta a mensagem principal do card para deixar claro se houve sucesso, fracasso ou ação pendente. */
    private String outcomeMessage(
            String decision, String reason, String marketNicheId, OprmNichoCnaeV2StageExecution lastExecution) {
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.CANCELED) {
            return "Cancelado pelo usuário: o CNAE está liberado para uma nova tentativa.";
        }
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.FAILED) {
            return "Falha técnica: corrija o erro antes de iniciar outro job.";
        }
        if ("NO_VIABLE_SUBNICHE".equals(decision)) {
            return "Fracasso controlado: este job terminou sem subnicho viável. " + reason;
        }
        if (marketNicheId != null) {
            return "Sucesso: o nicho foi materializado e já pode ser visualizado.";
        }
        if (Boolean.TRUE.equals(lastExecution.getMaterializationEnabled())) {
            return "Sucesso parcial: o job encontrou avanço possível, mas o nicho ainda precisa ser materializado.";
        }
        return "Sucesso operacional: o job terminou, mas a materialização automática está desativada para calibração.";
    }

    /** Define o texto do comando principal associado ao resultado do job. */
    private String actionLabel(String decision, String marketNicheId, OprmNichoCnaeV2StageExecution lastExecution) {
        if (marketNicheId != null) {
            return "Visualizar novo nicho";
        }
        if ("NO_VIABLE_SUBNICHE".equals(decision)) {
            return "Pesquisar outro recorte";
        }
        if (lastExecution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.COMPLETED) {
            return "Abrir CNAE para materializar";
        }
        return null;
    }

    /** Define a URL interna do comando principal sem deixar a tela inferir regra de negócio. */
    private String actionUrl(String decision, String cnaeCode, String marketNicheId) {
        if (marketNicheId != null) {
            return "/niches/" + marketNicheId;
        }
        if ("NO_VIABLE_SUBNICHE".equals(decision)) {
            return "/oprm/cnaes/" + cnaeCode;
        }
        return "/oprm/cnaes/" + cnaeCode;
    }

    /** Extrai o ID de nicho materializado quando alguma etapa persistiu esse identificador no payload. */
    private String marketNicheIdFrom(List<OprmNichoCnaeV2StageExecution> executions) {
        return executions.stream()
                .sorted(Comparator.comparing(OprmNichoCnaeV2StageExecution::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .map(execution -> parsePayload(execution.getOutputPayload()))
                .flatMap(output -> MARKET_NICHE_ID_KEYS.stream().map(output::get))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    /** Soma custo de IA em dólares registrado na saída própria de cada etapa do job. */
    private BigDecimal sumAiCostUsd(String jobId, List<OprmNichoCnaeV2StageExecution> executions) {
        BigDecimal auditedCost = openAiInteractionRepository == null ? BigDecimal.ZERO : openAiInteractionRepository.sumCostUsdByJobId(jobId);
        if (auditedCost != null && auditedCost.compareTo(BigDecimal.ZERO) > 0) {
            return auditedCost;
        }
        return executions.stream()
                .map(execution -> costFromPayload(execution.getOutputPayload()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Verifica se há sinal de uso de IA mesmo quando o custo registrado for zero ou ausente. */
    private boolean hasAiUsageSignal(OprmNichoCnaeV2StageExecution execution) {
        return hasAiUsageSignal(parsePayload(execution.getOutputPayload()));
    }

    /** Verifica se o job possui interação OpenAI auditada em tabela própria. */
    private boolean hasAuditedOpenAiInteraction(String jobId) {
        return openAiInteractionRepository != null && openAiInteractionRepository.existsByJobId(jobId);
    }

    /** Detecta chaves explícitas de IA em payload estruturado para classificar o job corretamente. */
    private boolean hasAiUsageSignal(Map<String, Object> payload) {
        return Boolean.TRUE.equals(payload.get("usedAi"))
                || Boolean.TRUE.equals(payload.get("aiUsed"))
                || payload.containsKey("model")
                || payload.containsKey("openAiModel")
                || payload.containsKey("aiUsage")
                || payload.containsKey("tokenUsage");
    }

    /** Extrai custos conhecidos de um payload JSON sem falhar a tela quando o contrato antigo não tem custo. */
    private BigDecimal costFromPayload(String payload) {
        return costFromMap(parsePayload(payload));
    }

    /** Soma custos por chaves canônicas de custo de IA, incluindo objetos e listas aninhadas. */
    private BigDecimal costFromMap(Map<String, Object> payload) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();
            if (COST_KEYS.contains(entry.getKey())) {
                total = total.add(decimal(value));
            } else if (value instanceof Map<?, ?> nested) {
                total = total.add(costFromMap(toStringKeyMap(nested)));
            } else if (value instanceof List<?> items) {
                total = total.add(costFromList(items));
            }
        }
        return total;
    }

    /** Soma custos encontrados dentro de listas estruturadas do payload. */
    private BigDecimal costFromList(List<?> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object item : items) {
            if (item instanceof Map<?, ?> nested) {
                total = total.add(costFromMap(toStringKeyMap(nested)));
            } else if (item instanceof List<?> nestedItems) {
                total = total.add(costFromList(nestedItems));
            }
        }
        return total;
    }

    /** Converte mapas vindos do Jackson para chaves textuais sem acoplar o contrato a DTO específico. */
    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        Map<String, Object> converted = new LinkedHashMap<>();
        source.forEach((key, value) -> converted.put(String.valueOf(key), value));
        return converted;
    }

    /** Normaliza números de custo de IA vindos como número ou texto decimal. */
    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            try {
                return new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    /** Lê payload JSON estruturado sem impedir o acompanhamento do job quando o payload estiver ausente ou legado. */
    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(payload, MAP_TYPE);
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    /** Formata contagens funcionais ausentes como zero para manter a explicação objetiva ao usuário. */
    private String numberText(Object value) {
        if (value instanceof Number number) {
            return String.valueOf(number.intValue());
        }
        return value == null || String.valueOf(value).isBlank() ? "0" : String.valueOf(value);
    }

    /** Verifica se o job possui alguma etapa ainda operacionalmente aberta. */
    private boolean hasOpenExecution(List<OprmNichoCnaeV2StageExecution> executions) {
        return executions.stream().anyMatch(this::isOpenExecution);
    }

    /** Identifica estados que ainda exigem execução ou retry pelo módulo OPRM. */
    private boolean isOpenExecution(OprmNichoCnaeV2StageExecution execution) {
        return execution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.PENDING
                || execution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.RUNNING
                || execution.getStatus() == OprmNichoCnaeV2StageExecutionStatus.TECHNICAL_RETRY_SCHEDULED;
    }

    /** Garante uma pendência inicial real a partir da fila atual de candidatos, sem materialização automática. */
    private void ensureInitialPendingExecution() {
        if (!stageExecutionRepository
                .findByStageCodeAndStatusOrderByCreatedAtAsc(
                        STAGE_CODE, OprmNichoCnaeV2StageExecutionStatus.PENDING, PageRequest.of(0, 1))
                .isEmpty()) {
            return;
        }
        nicheCandidateRepository.findNextPendingRoutineResearchCandidatePreview(PageRequest.of(0, 1)).stream()
                .filter(candidate -> !stageExecutionRepository.existsBySourceNicheIdAndStageCode(
                        candidate.getId(), STAGE_CODE))
                .findFirst()
                .ifPresent(candidate -> stageExecutionRepository.save(createInitialExecution(candidate, 1)));
    }

    /** Cria a primeira execução imutável da etapa candidate-generator para o candidato priorizado. */
    private OprmNichoCnaeV2StageExecution createInitialExecution(OprmNicheCandidate candidate, long jobSequence) {
        Instant now = Instant.now();
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setJobId("nichocnae-v2-candidate-" + candidate.getId() + "-job-" + jobSequence);
        execution.setSourceNicheId(candidate.getId());
        execution.setCnaeCode(candidate.getCnaeCode());
        execution.setStageCode(STAGE_CODE);
        execution.setAttemptNumber(1);
        execution.setTechnicalRetryNumber(0);
        execution.setKnowledgeVersion(1);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        execution.setMaterializationEnabled(materializationEnabled);
        execution.setCreatedAt(now);
        execution.setUpdatedAt(now);
        return execution;
    }

    /** Cria uma nova linha para retry técnico preservando a tentativa cognitiva e a versão de conhecimento. */
    private OprmNichoCnaeV2StageExecution createTechnicalRetry(OprmNichoCnaeV2StageExecution previousExecution) {
        Instant now = Instant.now();
        OprmNichoCnaeV2StageExecution retry = new OprmNichoCnaeV2StageExecution();
        retry.setJobId(previousExecution.getJobId());
        retry.setResearchCycleId(previousExecution.getResearchCycleId());
        retry.setSourceNicheId(previousExecution.getSourceNicheId());
        retry.setCnaeCode(previousExecution.getCnaeCode());
        retry.setStageCode(previousExecution.getStageCode());
        retry.setAttemptNumber(previousExecution.getAttemptNumber());
        retry.setTechnicalRetryNumber(previousExecution.getTechnicalRetryNumber() + 1);
        retry.setKnowledgeVersion(previousExecution.getKnowledgeVersion());
        retry.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        retry.setInputPayload(previousExecution.getInputPayload());
        retry.setMaterializationEnabled(previousExecution.getMaterializationEnabled());
        retry.setCreatedAt(now);
        retry.setUpdatedAt(now);
        return retry;
    }

    /** Carrega a execução da etapa atual ou devolve erro HTTP claro ao executor. */
    private OprmNichoCnaeV2StageExecution findExecution(Long stageExecutionId) {
        return stageExecutionRepository
                .findByIdAndStageCode(stageExecutionId, STAGE_CODE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "NichoCNAE v2 candidate-generator stage execution not found: " + stageExecutionId));
    }

    /** Converte a entidade persistida no contrato canônico de pending do executor. */
    private CandidateGeneratorPendingResponse toPendingResponse(OprmNichoCnaeV2StageExecution execution) {
        String cnaeDescription = nicheCandidateRepository
                .findById(execution.getSourceNicheId())
                .map(OprmNicheCandidate::getCnaeDescription)
                .orElse(null);
        return new CandidateGeneratorPendingResponse(
                String.valueOf(execution.getId()),
                execution.getJobId(),
                execution.getCnaeCode(),
                cnaeDescription,
                execution.getResearchCycleId(),
                execution.getSourceNicheId(),
                execution.getAttemptNumber(),
                execution.getTechnicalRetryNumber(),
                execution.getKnowledgeVersion(),
                execution.getMaterializationEnabled());
    }

    /** Registra no banco as interações OpenAI informadas pelo executor externo. */
    private void recordOpenAiInteractions(
            OprmNichoCnaeV2StageExecution execution, List<OpenAiInteractionAuditRequest> requests) {
        if (openAiInteractionRepository == null || execution == null || requests == null || requests.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        openAiInteractionRepository.saveAll(requests.stream()
                .map(request -> toOpenAiInteraction(execution, request, now))
                .toList());
    }

    /** Converte o contrato de auditoria recebido do executor em entidade persistível do backend. */
    private OprmNichoCnaeV2OpenAiInteraction toOpenAiInteraction(
            OprmNichoCnaeV2StageExecution execution, OpenAiInteractionAuditRequest request, Instant now) {
        OprmNichoCnaeV2OpenAiInteraction entity = new OprmNichoCnaeV2OpenAiInteraction();
        entity.setStageExecutionId(execution.getId());
        entity.setJobId(execution.getJobId());
        entity.setStageCode(execution.getStageCode());
        entity.setAttemptNumber(execution.getAttemptNumber());
        entity.setTechnicalRetryNumber(execution.getTechnicalRetryNumber());
        entity.setModel(request.model());
        entity.setServiceTier(request.serviceTier());
        entity.setInputTokens(request.inputTokens());
        entity.setOutputTokens(request.outputTokens());
        entity.setTotalTokens(request.totalTokens());
        entity.setCostUsd(request.costUsd());
        entity.setOpenAiResponseId(request.openAiResponseId());
        entity.setRawRequest(request.rawRequest());
        entity.setRawResponse(request.rawResponse());
        entity.setStatus(request.status());
        entity.setErrorMessage(request.errorMessage());
        entity.setCreatedAt(now);
        return entity;
    }
}
