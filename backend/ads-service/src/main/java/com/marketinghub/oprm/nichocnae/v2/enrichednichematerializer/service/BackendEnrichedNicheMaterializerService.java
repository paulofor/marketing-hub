package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.completeStageExecution.EnrichedNicheMaterializerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.completeStageExecution.EnrichedNicheMaterializerCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.createStageExecution.EnrichedNicheMaterializerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.createStageExecution.EnrichedNicheMaterializerCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.failStageExecution.EnrichedNicheMaterializerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.failStageExecution.EnrichedNicheMaterializerFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.pending.EnrichedNicheMaterializerPendingResponse;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Expõe contratos de leitura/escrita do backend para a etapa enriched-niche-materializer do pipeline NichoCNAE v2. */
@Service("oprmNichocnaeV2BackendEnrichedNicheMaterializerService")
public class BackendEnrichedNicheMaterializerService {
    public static final String STAGE_CODE = "enriched-niche-materializer";
    private final OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;
    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final boolean v2Enabled;

    /** Inicializa o service com repositório canônico e feature flag de calibração da v2. */
    public BackendEnrichedNicheMaterializerService(
            OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository,
            OprmNicheCandidateRepository nicheCandidateRepository,
            @Value("${oprm.nichocnae.v2.enabled:false}") boolean v2Enabled) {
        this.stageExecutionRepository = stageExecutionRepository;
        this.nicheCandidateRepository = nicheCandidateRepository;
        this.v2Enabled = v2Enabled;
    }

    /** Lista pendências disponíveis para consumo canônico pelo executor OPRM NichoCNAE v2. */
    @Transactional(readOnly = true)
    public List<EnrichedNicheMaterializerPendingResponse> pending() {
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

    /** Registra conclusão do materializador de nicho enriquecido usando a decisão informada pelo executor externo. */
    @Transactional
    public EnrichedNicheMaterializerCompletionResponse complete(
            Long stageExecutionId, EnrichedNicheMaterializerCompletionRequest request) {
        OprmNichoCnaeV2StageExecution execution = findExecution(stageExecutionId);
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.COMPLETED);
        execution.setOutputPayload(request == null ? null : request.outputPayload());
        execution.setNextStageCode(request == null ? null : request.nextStageCode());
        execution.setUpdatedAt(Instant.now());
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(execution);
        return new EnrichedNicheMaterializerCompletionResponse(
                String.valueOf(saved.getId()),
                saved.getStatus().name(),
                saved.getNextStageCode(),
                request == null ? null : request.materializationDecision(),
                request == null ? null : request.validationLevel(),
                request == null ? null : request.confidence(),
                request == null ? null : request.materializedNicheId());
    }

    /** Registra falha e cria nova execução somente quando a falha for retry técnico de infraestrutura. */
    @Transactional
    public EnrichedNicheMaterializerFailureResponse fail(Long stageExecutionId, EnrichedNicheMaterializerFailureRequest request) {
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
            return new EnrichedNicheMaterializerFailureResponse(
                    String.valueOf(execution.getId()), execution.getStatus().name(), String.valueOf(savedRetry.getId()),
                    savedRetry.getAttemptNumber(), savedRetry.getTechnicalRetryNumber());
        }
        execution.setStatus(OprmNichoCnaeV2StageExecutionStatus.FAILED);
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(execution);
        return new EnrichedNicheMaterializerFailureResponse(
                String.valueOf(saved.getId()), saved.getStatus().name(), null, saved.getAttemptNumber(), saved.getTechnicalRetryNumber());
    }

    /** Grava uma pendência da etapa de materializador de nicho enriquecido solicitada pelo executor externo. */
    @Transactional
    public EnrichedNicheMaterializerCreateResponse create(EnrichedNicheMaterializerCreateRequest request) {
        OprmNichoCnaeV2StageExecution saved = stageExecutionRepository.save(toPendingExecution(request));
        return new EnrichedNicheMaterializerCreateResponse(String.valueOf(saved.getId()), saved.getStatus().name(), saved.getStageCode());
    }

    /** Converte o contrato recebido do executor em entidade persistível, sem decidir regra operacional. */
    private OprmNichoCnaeV2StageExecution toPendingExecution(EnrichedNicheMaterializerCreateRequest request) {
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
        OprmNichoCnaeV2StageExecution retry = toPendingExecution(new EnrichedNicheMaterializerCreateRequest(
                previousExecution.getJobId(), previousExecution.getResearchCycleId(), previousExecution.getSourceNicheId(),
                previousExecution.getCnaeCode(), previousExecution.getAttemptNumber(), previousExecution.getKnowledgeVersion(),
                previousExecution.getMaterializationEnabled(), previousExecution.getInputPayload()));
        retry.setTechnicalRetryNumber(previousExecution.getTechnicalRetryNumber() + 1);
        return retry;
    }

    /** Carrega a execução da etapa atual ou devolve erro HTTP claro ao executor. */
    private OprmNichoCnaeV2StageExecution findExecution(Long stageExecutionId) {
        return stageExecutionRepository.findByIdAndStageCode(stageExecutionId, STAGE_CODE).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "NichoCNAE v2 enriched-niche-materializer stage execution not found: " + stageExecutionId));
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
    private EnrichedNicheMaterializerPendingResponse toPendingResponse(OprmNichoCnaeV2StageExecution execution) {
        return new EnrichedNicheMaterializerPendingResponse(
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
}
