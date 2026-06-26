package com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service;

import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.createStageExecution.PersonaTournamentCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.pending.PersonaTournamentPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa persona-tournament do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendPersonaTournamentService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "persona-tournament";
    private static final String NEXT_STAGE = "routine-query-planner";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendPersonaTournamentService(OprmNichoCnaeV3StageExecutionRepository repository, OprmCnpjCnaeDimRepository cnaeRepository) {
        super(repository, cnaeRepository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa persona-tournament. */
    public PersonaTournamentCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Inicia pendência da etapa para o CNAE informado pela tela administrativa. */
    public PersonaTournamentCreateResponse start(String cnaeCode) {
        markCnaePipelineStarted(cnaeCode, STATUS_STARTED);
        return create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
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
