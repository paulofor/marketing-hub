package com.marketinghub.oprm.nichocnae.v3.routinequeryplanner.service;

import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.nichocnae.v3.routinequeryplanner.service.createStageExecution.RoutineQueryPlannerCreateResponse;
import com.marketinghub.oprm.nichocnae.v3.routinequeryplanner.service.pending.RoutineQueryPlannerPendingResponse;
import com.marketinghub.oprm.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa routine-query-planner do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendRoutineQueryPlannerService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "routine-query-planner";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendRoutineQueryPlannerService(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa routine-query-planner. */
    public RoutineQueryPlannerCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa routine-query-planner para o executor OPRM. */
    public List<RoutineQueryPlannerPendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa routine-query-planner. */
    public RoutineQueryPlannerCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa routine-query-planner. */
    public RoutineQueryPlannerCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private RoutineQueryPlannerCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new RoutineQueryPlannerCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private RoutineQueryPlannerPendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new RoutineQueryPlannerPendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
