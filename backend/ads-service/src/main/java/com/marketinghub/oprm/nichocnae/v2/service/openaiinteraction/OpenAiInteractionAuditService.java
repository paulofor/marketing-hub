package com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteraction;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.repository.jpa.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteractionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service responsável por persistir auditoria financeira e operacional de chamadas OpenAI do NichoCNAE v2. */
@Service
public class OpenAiInteractionAuditService {
    private final OprmNichoCnaeV2OpenAiInteractionRepository repository;

    /** Inicializa o service com o repositório canônico de interações OpenAI do pipeline v2. */
    public OpenAiInteractionAuditService(OprmNichoCnaeV2OpenAiInteractionRepository repository) {
        this.repository = repository;
    }

    /** Registra as interações OpenAI informadas pelo executor para a execução de etapa concluída. */
    public void record(OprmNichoCnaeV2StageExecution execution, List<OpenAiInteractionAuditRequest> requests) {
        if (execution == null || requests == null || requests.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        repository.saveAll(requests.stream().map(request -> toEntity(execution, request, now)).toList());
    }

    /** Converte o contrato de auditoria em entidade persistível ligada ao job e à etapa. */
    private OprmNichoCnaeV2OpenAiInteraction toEntity(
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
