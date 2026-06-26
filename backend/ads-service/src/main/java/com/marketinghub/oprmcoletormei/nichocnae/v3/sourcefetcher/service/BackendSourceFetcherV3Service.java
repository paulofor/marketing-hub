package com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service;

import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.createStageExecution.SourceFetcherCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.pending.SourceFetcherPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeRequestRequest;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.PipelineNichoCnaeRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa source-fetcher do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendSourceFetcherV3Service extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "source-fetcher";
    private static final String NEXT_STAGE = "routine-signal-extractor";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendSourceFetcherV3Service(OprmNichoCnaeV3StageExecutionRepository repository, OprmCnpjCnaeDimRepository cnaeRepository, PipelineNichoCnaeRepository pipelineNichoCnaeRepository) {
        super(repository, cnaeRepository, pipelineNichoCnaeRepository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa source-fetcher. */
    public SourceFetcherCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Inicia pendência da etapa para o CNAE informado pela tela administrativa. */
    public SourceFetcherCreateResponse start(String cnaeCode) {
        markCnaePipelineStarted(cnaeCode, STATUS_STARTED);
        return create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Recebe o request bruto da etapa e registra auditoria para o pipeline NichoCNAE v3. */
    public SourceFetcherCreateResponse recebeRequest(String cnaeCode, OprmNichoCnaeV3RecebeRequestRequest request) {
        return new SourceFetcherCreateResponse(null, doRecebeRequest(cnaeCode, request).getJobId(), cnaeCode, STAGE_CODE, "AGUARDANDO_MODULO");
    }

    /** Lista pendências da etapa source-fetcher para o executor OPRM. */
    public List<SourceFetcherPendingResponse> pending() {
        return pendingCnaes().stream().map(this::toPendingResponse).toList();
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
    private SourceFetcherPendingResponse toPendingResponse(OprmCnpjCnaeDim cnae) {
        OprmNichoCnaeV3StageExecution execution = pendingExecution(cnae).orElse(null);
        return new SourceFetcherPendingResponse(
                execution == null ? null : execution.getId(),
                execution == null ? pendingJobId(cnae) : execution.getJobId(),
                cnae.getCnaeCode(),
                execution == null ? cnaeInputPayload(cnae) : execution.getInputPayload(),
                execution == null ? 1 : execution.getAttemptNumber(),
                execution == null ? 1 : execution.getKnowledgeVersion());
    }
}
