package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.createStageExecution.CandidateGeneratorCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.pending.CandidateGeneratorPendingResponse;
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

/** Expõe contratos de leitura/escrita do backend para a etapa candidate-generator do pipeline NichoCNAE v2. */
@Service
public class BackendCandidateGeneratorService {
    private static final String STAGE_CODE = "candidate-generator";

    private final OprmNicheCandidateRepository nicheCandidateRepository;
    private final OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository;
    private final boolean v2Enabled;
    private final boolean materializationEnabled;

    /** Inicializa o service com repositórios canônicos e feature flags de calibração da v2. */
    public BackendCandidateGeneratorService(
            OprmNicheCandidateRepository nicheCandidateRepository,
            OprmNichoCnaeV2StageExecutionRepository stageExecutionRepository,
            @Value("${oprm.nichocnae.v2.enabled:false}") boolean v2Enabled,
            @Value("${oprm.nichocnae.v2.materialization-enabled:false}") boolean materializationEnabled) {
        this.nicheCandidateRepository = nicheCandidateRepository;
        this.stageExecutionRepository = stageExecutionRepository;
        this.v2Enabled = v2Enabled;
        this.materializationEnabled = materializationEnabled;
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
        return new CandidateGeneratorPendingResponse(
                String.valueOf(execution.getId()),
                execution.getJobId(),
                execution.getCnaeCode(),
                execution.getSourceNicheId(),
                execution.getAttemptNumber(),
                execution.getTechnicalRetryNumber(),
                execution.getKnowledgeVersion(),
                execution.getMaterializationEnabled());
    }
}
