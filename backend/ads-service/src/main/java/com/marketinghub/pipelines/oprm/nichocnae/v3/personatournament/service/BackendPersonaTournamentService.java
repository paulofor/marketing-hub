package com.marketinghub.pipelines.oprm.nichocnae.v3.personatournament.service;

import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.pipelines.oprm.nichocnae.v3.personatournament.service.createStageExecution.PersonaTournamentCreateResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.personatournament.service.pending.PersonaTournamentPendingResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa persona-tournament do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendPersonaTournamentService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "persona-tournament";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendPersonaTournamentService(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa persona-tournament. */
    public PersonaTournamentCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa persona-tournament para o executor OPRM. */
    public List<PersonaTournamentPendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa persona-tournament. */
    public PersonaTournamentCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa persona-tournament. */
    public PersonaTournamentCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private PersonaTournamentCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new PersonaTournamentCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private PersonaTournamentPendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new PersonaTournamentPendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
