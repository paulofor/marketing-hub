package com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.service;

import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.service.createStageExecution.PersonaRoutineMaterializerCreateResponse;
import com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.service.pending.PersonaRoutineMaterializerPendingResponse;
import com.marketinghub.oprm.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa persona-routine-materializer do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendPersonaRoutineMaterializerService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "persona-routine-materializer";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendPersonaRoutineMaterializerService(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa persona-routine-materializer. */
    public PersonaRoutineMaterializerCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa persona-routine-materializer para o executor OPRM. */
    public List<PersonaRoutineMaterializerPendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa persona-routine-materializer. */
    public PersonaRoutineMaterializerCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa persona-routine-materializer. */
    public PersonaRoutineMaterializerCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private PersonaRoutineMaterializerCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new PersonaRoutineMaterializerCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private PersonaRoutineMaterializerPendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new PersonaRoutineMaterializerPendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
