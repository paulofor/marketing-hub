package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteraction;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.completeStageExecution.SourceFetcherRerankerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.completeStageExecution.SourceFetcherRerankerCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.createStageExecution.SourceFetcherRerankerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.createStageExecution.SourceFetcherRerankerCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.failStageExecution.SourceFetcherRerankerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.failStageExecution.SourceFetcherRerankerFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.pending.SourceFetcherRerankerPendingResponse;
import com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction.OpenAiInteractionAuditRequest;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteractionRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Expõe contratos de leitura/escrita do backend para a etapa source-fetcher-reranker do pipeline NichoCNAE v2. */
@Service
public class BackendSourceFetcherRerankerService {
    public static final String STAGE_CODE = "source-fetcher-reranker";
    private final OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;
    private final OprmNichoCnaeV2OpenAiInteractionRepository openAiInteractionRepository;
    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final boolean v2Enabled;

    /** Inicializa o service com repositório canônico e feature flag de calibração da v2. */
    public BackendSourceFetcherRerankerService(
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
    public BackendSourceFetcherRerankerService(
            OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository,
            OprmNicheCandidateRepository nicheCandidateRepository,
            boolean v2Enabled) {
        this(stageExecutionRepository, null, nicheCandidateRepository, v2Enabled);
    }
    /** Lista pendências disponíveis para consumo canônico pelo executor OPRM NichoCNAE v2. */
    @Transactional(readOnly = true)
    public List<SourceFetcherRerankerPendingResponse> pending() {
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

    /** Registra conclusão da coleta e do reranking usando a próxima etapa informada pelo executor externo. */
    @Transactional
    public SourceFetcherRerankerCompletionResponse complete(
            Long stageExecutionId, SourceFetcherRerankerCompletionRequest request) {
        OprmNichoCnaeV2StageExecution execution = findExecution(stageExecutionId);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        execution.setOutputPayload(request == null ? null : request.outputPayload());
        execution.setNextStageCode(request == null ? null : request.nextStageCode());
        execution.setUpdatedAt(Instant.now());
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(execution);
        recordOpenAiInteractions(saved, request == null ? null : request.openAiInteractions());
        return new SourceFetcherRerankerCompletionResponse(
                String.valueOf(saved.getId()),
                saved.getStatus().name(),
                saved.getNextStageCode(),
                request == null ? null : request.sourceFetchDecision(),
                request == null ? null : request.fetchedSnapshotCount(),
                request == null ? null : request.selectedSourceCount(),
                request == null ? null : request.rejectedSourceCount());
    }

    /** Registra falha e cria nova execução somente quando a falha for retry técnico de infraestrutura. */
    @Transactional
    public SourceFetcherRerankerFailureResponse fail(Long stageExecutionId, SourceFetcherRerankerFailureRequest request) {
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
            return new SourceFetcherRerankerFailureResponse(
                    String.valueOf(execution.getId()),
                    execution.getStatus().name(),
                    String.valueOf(savedRetry.getId()),
                    savedRetry.getAttemptNumber(),
                    savedRetry.getTechnicalRetryNumber());
        }
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.FAILED);
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(execution);
        return new SourceFetcherRerankerFailureResponse(
                String.valueOf(saved.getId()),
                saved.getStatus().name(),
                null,
                saved.getAttemptNumber(),
                saved.getTechnicalRetryNumber());
    }

    /** Grava uma pendência da etapa de coleta e reranking solicitada pelo executor externo. */
    @Transactional
    public SourceFetcherRerankerCreateResponse create(SourceFetcherRerankerCreateRequest request) {
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(toPendingExecution(request));
        return new SourceFetcherRerankerCreateResponse(
                String.valueOf(saved.getId()), saved.getStatus().name(), saved.getStageCode());
    }

    /** Converte o contrato recebido do executor em entidade persistível, sem decidir regra operacional. */
    private OprmNichoCnaeV2StageExecution toPendingExecution(SourceFetcherRerankerCreateRequest request) {
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
        OprmNichoCnaeV2StageExecution retry = toPendingExecution(new SourceFetcherRerankerCreateRequest(
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
                        "NichoCNAE v2 source-fetcher-reranker stage execution not found: " + stageExecutionId));
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
    private SourceFetcherRerankerPendingResponse toPendingResponse(OprmNichoCnaeV2StageExecution execution) {
        return new SourceFetcherRerankerPendingResponse(
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
