package com.marketinghub.pipelines.oprm.nichocnae.v3.sourcesearcher.service;

import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.pipelines.oprm.nichocnae.v3.sourcesearcher.service.createStageExecution.SourceSearcherCreateResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.sourcesearcher.service.pending.SourceSearcherPendingResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa source-searcher do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendSourceSearcherV3Service extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "source-searcher";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendSourceSearcherV3Service(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa source-searcher. */
    public SourceSearcherCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa source-searcher para o executor OPRM. */
    public List<SourceSearcherPendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa source-searcher. */
    public SourceSearcherCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa source-searcher. */
    public SourceSearcherCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private SourceSearcherCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new SourceSearcherCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private SourceSearcherPendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new SourceSearcherPendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
