package com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.service;

import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.service.createStageExecution.DailyTasksSynthesizerCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.service.pending.DailyTasksSynthesizerPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa daily-tasks-synthesizer do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendDailyTasksSynthesizerService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "daily-tasks-synthesizer";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendDailyTasksSynthesizerService(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa daily-tasks-synthesizer. */
    public DailyTasksSynthesizerCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa daily-tasks-synthesizer para o executor OPRM. */
    public List<DailyTasksSynthesizerPendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa daily-tasks-synthesizer. */
    public DailyTasksSynthesizerCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa daily-tasks-synthesizer. */
    public DailyTasksSynthesizerCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private DailyTasksSynthesizerCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new DailyTasksSynthesizerCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private DailyTasksSynthesizerPendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new DailyTasksSynthesizerPendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
