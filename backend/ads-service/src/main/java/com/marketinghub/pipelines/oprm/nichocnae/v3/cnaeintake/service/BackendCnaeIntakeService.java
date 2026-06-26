package com.marketinghub.pipelines.oprm.nichocnae.v3.cnaeintake.service;

import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.pipelines.oprm.nichocnae.v3.cnaeintake.service.createStageExecution.CnaeIntakeCreateResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.cnaeintake.service.pending.CnaeIntakePendingResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa cnae-intake do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendCnaeIntakeService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "cnae-intake";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendCnaeIntakeService(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa cnae-intake. */
    public CnaeIntakeCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa cnae-intake para o executor OPRM. */
    public List<CnaeIntakePendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa cnae-intake. */
    public CnaeIntakeCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa cnae-intake. */
    public CnaeIntakeCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private CnaeIntakeCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new CnaeIntakeCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private CnaeIntakePendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new CnaeIntakePendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
