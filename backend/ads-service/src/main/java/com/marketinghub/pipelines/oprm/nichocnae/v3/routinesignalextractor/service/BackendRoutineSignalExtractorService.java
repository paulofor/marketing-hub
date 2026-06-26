package com.marketinghub.pipelines.oprm.nichocnae.v3.routinesignalextractor.service;

import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.pipelines.oprm.nichocnae.v3.routinesignalextractor.service.createStageExecution.RoutineSignalExtractorCreateResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.routinesignalextractor.service.pending.RoutineSignalExtractorPendingResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa routine-signal-extractor do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendRoutineSignalExtractorService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "routine-signal-extractor";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendRoutineSignalExtractorService(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa routine-signal-extractor. */
    public RoutineSignalExtractorCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa routine-signal-extractor para o executor OPRM. */
    public List<RoutineSignalExtractorPendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa routine-signal-extractor. */
    public RoutineSignalExtractorCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa routine-signal-extractor. */
    public RoutineSignalExtractorCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private RoutineSignalExtractorCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new RoutineSignalExtractorCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private RoutineSignalExtractorPendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new RoutineSignalExtractorPendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
