package com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service;

import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.createStageExecution.QualityGateCreateResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.pending.QualityGatePendingResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa quality-gate do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendQualityGateService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "quality-gate";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendQualityGateService(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa quality-gate. */
    public QualityGateCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa quality-gate para o executor OPRM. */
    public List<QualityGatePendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa quality-gate. */
    public QualityGateCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa quality-gate. */
    public QualityGateCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private QualityGateCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new QualityGateCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private QualityGatePendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new QualityGatePendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
