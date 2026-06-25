package com.marketinghub.oprm.nichocnae.v3.sourcefetcher.service;

import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.nichocnae.v3.sourcefetcher.service.createStageExecution.SourceFetcherCreateResponse;
import com.marketinghub.oprm.nichocnae.v3.sourcefetcher.service.pending.SourceFetcherPendingResponse;
import com.marketinghub.oprm.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa source-fetcher do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendSourceFetcherV3Service extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "source-fetcher";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendSourceFetcherV3Service(OprmNichoCnaeV3StageExecutionRepository repository) {
        super(repository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa source-fetcher. */
    public SourceFetcherCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Lista pendências da etapa source-fetcher para o executor OPRM. */
    public List<SourceFetcherPendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa source-fetcher. */
    public SourceFetcherCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa source-fetcher. */
    public SourceFetcherCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private SourceFetcherCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new SourceFetcherCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private SourceFetcherPendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new SourceFetcherPendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }
}
