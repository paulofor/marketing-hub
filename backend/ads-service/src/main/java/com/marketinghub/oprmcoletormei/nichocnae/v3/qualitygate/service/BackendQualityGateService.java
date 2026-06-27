package com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service;

import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service.createStageExecution.QualityGateCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service.pending.QualityGatePendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeRequestRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseResponse;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.PipelineNichoCnaeRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa quality-gate do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendQualityGateService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "quality-gate";
    private static final String NEXT_STAGE = "persona-routine-materializer";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendQualityGateService(OprmNichoCnaeV3StageExecutionRepository repository, OprmCnpjCnaeDimRepository cnaeRepository, PipelineNichoCnaeRepository pipelineNichoCnaeRepository) {
        super(repository, cnaeRepository, pipelineNichoCnaeRepository, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa quality-gate. */
    public QualityGateCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Inicia pendência da etapa para o CNAE informado pela tela administrativa. */
    public QualityGateCreateResponse start(String cnaeCode) {
        markCnaePipelineStarted(cnaeCode, STATUS_STARTED);
        return create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Recebe o request bruto da etapa e registra auditoria para o pipeline NichoCNAE v3. */
    public QualityGateCreateResponse recebeRequest(String cnaeCode, String jobId, OprmNichoCnaeV3RecebeRequestRequest request) {
        return new QualityGateCreateResponse(null, doRecebeRequest(cnaeCode, jobId, request).getJobId(), cnaeCode, STAGE_CODE, "AGUARDANDO_MODULO");
    }

    /** Recebe o response bruto da etapa e registra conclusão ou falha do pipeline NichoCNAE v3. */
    public OprmNichoCnaeV3RecebeResponseResponse recebeResponse(String cnaeCode, String jobId, OprmNichoCnaeV3RecebeResponseRequest request) {
        return doRecebeResponse(cnaeCode, jobId, request, NEXT_STAGE);
    }

    /** Lista pendências da etapa quality-gate para o executor OPRM. */
    public List<QualityGatePendingResponse> pending() {
        return pendingCnaes().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa quality-gate. */
    public QualityGateCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa quality-gate. */
    public QualityGateCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private QualityGateCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new QualityGateCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private QualityGatePendingResponse toPendingResponse(OprmCnpjCnaeDim cnae) {
        OprmNichoCnaeV3StageExecution execution = pendingExecution(cnae).orElse(null);
        return new QualityGatePendingResponse(
                execution == null ? null : execution.getId(),
                execution == null ? pendingJobId(cnae) : execution.getJobId(),
                cnae.getCnaeCode(),
                execution == null ? cnaeInputPayload(cnae) : execution.getInputPayload(),
                execution == null ? 1 : execution.getAttemptNumber(),
                execution == null ? 1 : execution.getKnowledgeVersion());
    }
}
