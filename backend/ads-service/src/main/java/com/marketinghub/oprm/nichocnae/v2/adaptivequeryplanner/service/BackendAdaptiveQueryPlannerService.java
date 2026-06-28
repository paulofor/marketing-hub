package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteraction;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.completeStageExecution.AdaptiveQueryPlannerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.completeStageExecution.AdaptiveQueryPlannerCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.createStageExecution.AdaptiveQueryPlannerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.createStageExecution.AdaptiveQueryPlannerCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.failStageExecution.AdaptiveQueryPlannerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.failStageExecution.AdaptiveQueryPlannerFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.pending.AdaptiveQueryPlannerPendingResponse;
import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionCostCalculator;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteractionRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Expõe contratos de leitura/escrita do backend para a etapa adaptive-query-planner do pipeline NichoCNAE v2. */
@Service
public class BackendAdaptiveQueryPlannerService {
    public static final String STAGE_CODE = "adaptive-query-planner";
    private final OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;
    private final OprmNichoCnaeV2OpenAiInteractionRepository openAiInteractionRepository;
    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final boolean v2Enabled;

    /** Inicializa o service com repositório canônico e feature flag de calibração da v2. */
    @Autowired
    public BackendAdaptiveQueryPlannerService(
            OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository,
            OprmNichoCnaeV2OpenAiInteractionRepository openAiInteractionRepository,
            OprmNicheCandidateRepository nicheCandidateRepository,
            @Value("${oprm.nichocnae.v2.enabled:false}") boolean v2Enabled) {
        this.stageExecutionRepository = stageExecutionRepository;
        this.openAiInteractionRepository = openAiInteractionRepository;
        this.nicheCandidateRepository = nicheCandidateRepository;
        this.v2Enabled = v2Enabled;
    }


    /** Mantém compatibilidade com testes unitários que não exercem auditoria OpenAI. */
    public BackendAdaptiveQueryPlannerService(
            OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository,
            OprmNicheCandidateRepository nicheCandidateRepository,
            boolean v2Enabled) {
        this(stageExecutionRepository, null, nicheCandidateRepository, v2Enabled);
    }
    /** Lista pendências disponíveis para consumo canônico pelo executor OPRM NichoCNAE v2. */
    @Transactional(readOnly = true)
    public List<AdaptiveQueryPlannerPendingResponse> pending() {
        if (!v2Enabled) {
            return List.of();
        }
        return stageExecutionRepository
                .findByStageCodeAndStatusOrderByCreatedAtAsc(
                        STAGE_CODE, OprmNichoCnaeV2StageExecutionStatus.PENDING, PageRequest.of(0, 5))
                .stream()
                .map(this::toPendingResponse)
                .toList();
    }

    /** Registra conclusão do planejamento usando a próxima etapa informada pelo executor externo. */
    @Transactional
    public AdaptiveQueryPlannerCompletionResponse complete(
            Long stageExecutionId, AdaptiveQueryPlannerCompletionRequest request) {
        OprmNichoCnaeV2StageExecution execution = findExecution(stageExecutionId);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        execution.setOutputPayload(request == null ? null : request.outputPayload());
        execution.setNextStageCode(request == null ? null : request.nextStageCode());
        execution.setUpdatedAt(Instant.now());
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(execution);
        recordOpenAiInteractions(saved, request == null ? null : request.openAiInteractions());
        return new AdaptiveQueryPlannerCompletionResponse(
                String.valueOf(saved.getId()),
                saved.getStatus().name(),
                saved.getNextStageCode(),
                request == null ? null : request.plannedQueryCount(),
                request == null ? null : request.reusedQueryCount(),
                request == null ? null : request.skippedQueryCount());
    }

    /** Registra falha e cria nova execução somente quando a falha for retry técnico de infraestrutura. */
    @Transactional
    public AdaptiveQueryPlannerFailureResponse fail(Long stageExecutionId, AdaptiveQueryPlannerFailureRequest request) {
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
            return new AdaptiveQueryPlannerFailureResponse(
                    String.valueOf(execution.getId()),
                    execution.getStatus().name(),
                    String.valueOf(savedRetry.getId()),
                    savedRetry.getAttemptNumber(),
                    savedRetry.getTechnicalRetryNumber());
        }
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.FAILED);
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(execution);
        return new AdaptiveQueryPlannerFailureResponse(
                String.valueOf(saved.getId()),
                saved.getStatus().name(),
                null,
                saved.getAttemptNumber(),
                saved.getTechnicalRetryNumber());
    }

    /** Grava uma pendência da etapa de planejamento solicitada pelo executor externo. */
    @Transactional
    public AdaptiveQueryPlannerCreateResponse create(AdaptiveQueryPlannerCreateRequest request) {
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(toPendingExecution(request));
        return new AdaptiveQueryPlannerCreateResponse(
                String.valueOf(saved.getId()), saved.getStatus().name(), saved.getStageCode());
    }

    /** Converte o contrato recebido do executor em entidade persistível, sem decidir regra operacional. */
    private OprmNichoCnaeV2StageExecution toPendingExecution(AdaptiveQueryPlannerCreateRequest request) {
        Instant now = Instant.now();
        OprmNichoCnaeV2StageExecution execution = new OprmNichoCnaeV2StageExecution();
        execution.setJobId(request.jobId());
        execution.setResearchCycleId(request.researchCycleId());
        execution.setSourceNicheId(request.sourceNicheId());
        execution.setCnaeCode(request.cnaeCode());
        execution.setStageCode(STAGE_CODE);
        execution.setAttemptNumber(request.attemptNumber());
        execution.setTechnicalRetryNumber(0);
        execution.setKnowledgeVersion(request.knowledgeVersion());
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.PENDING);
        execution.setInputPayload(request.inputPayload());
        execution.setMaterializationEnabled(Boolean.TRUE.equals(request.materializationEnabled()));
        execution.setCreatedAt(now);
        execution.setUpdatedAt(now);
        return execution;
    }

    /** Cria uma nova linha para retry técnico preservando a tentativa cognitiva e a versão de conhecimento. */
    private OprmNichoCnaeV2StageExecution createTechnicalRetry(OprmNichoCnaeV2StageExecution previousExecution) {
        OprmNichoCnaeV2StageExecution retry = toPendingExecution(new AdaptiveQueryPlannerCreateRequest(
                previousExecution.getJobId(),
                previousExecution.getResearchCycleId(),
                previousExecution.getSourceNicheId(),
                previousExecution.getCnaeCode(),
                previousExecution.getAttemptNumber(),
                previousExecution.getKnowledgeVersion(),
                previousExecution.getMaterializationEnabled(),
                previousExecution.getInputPayload()));
        retry.setTechnicalRetryNumber(previousExecution.getTechnicalRetryNumber() + 1);
        return retry;
    }

    /** Carrega a execução da etapa atual ou devolve erro HTTP claro ao executor. */
    private OprmNichoCnaeV2StageExecution findExecution(Long stageExecutionId) {
        return stageExecutionRepository
                .findByIdAndStageCode(stageExecutionId, STAGE_CODE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "NichoCNAE v2 adaptive-query-planner stage execution not found: " + stageExecutionId));
    }

    /** Busca a descrição canônica do CNAE pelo candidato de origem para compor o envelope do pending. */
    private String cnaeDescription(OprmNichoCnaeV2StageExecution execution) {
        Long sourceNicheId = execution.getSourceNicheId();
        if (sourceNicheId == null) {
            return null;
        }
        return nicheCandidateRepository
                .findById(sourceNicheId)
                .map(candidate -> candidate.getCnaeDescription())
                .orElse(null);
    }

    /** Converte a entidade persistida no contrato canônico de pending do executor. */
    private AdaptiveQueryPlannerPendingResponse toPendingResponse(OprmNichoCnaeV2StageExecution execution) {
        return new AdaptiveQueryPlannerPendingResponse(
                String.valueOf(execution.getId()),
                execution.getJobId(),
                execution.getCnaeCode(),
                cnaeDescription(execution),
                execution.getResearchCycleId(),
                execution.getSourceNicheId(),
                execution.getAttemptNumber(),
                execution.getTechnicalRetryNumber(),
                execution.getKnowledgeVersion(),
                execution.getMaterializationEnabled(),
                execution.getInputPayload());
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
        entity.setCostUsd(OpenAiInteractionCostCalculator.resolveCostUsd(request));
        entity.setOpenAiResponseId(request.openAiResponseId());
        entity.setRawRequest(request.rawRequest());
        entity.setRawResponse(request.rawResponse());
        entity.setStatus(request.status());
        entity.setErrorMessage(request.errorMessage());
        entity.setCreatedAt(now);
        return entity;
    }
}
