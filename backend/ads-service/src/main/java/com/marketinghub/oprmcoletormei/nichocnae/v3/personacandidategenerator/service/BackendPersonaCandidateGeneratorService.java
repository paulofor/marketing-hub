package com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service;

import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service.createStageExecution.PersonaCandidateGeneratorCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service.pending.PersonaCandidateGeneratorPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeRequestRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseResponse;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.PipelineNichoCnaeRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service canônico da etapa persona-candidate-generator do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendPersonaCandidateGeneratorService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "persona-candidate-generator";
    private static final String NEXT_STAGE = "persona-tournament";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendPersonaCandidateGeneratorService(OprmNichoCnaeV3StageExecutionRepository repository, OprmCnpjCnaeDimRepository cnaeRepository, PipelineNichoCnaeRepository pipelineNichoCnaeRepository, OpenAiPricingService openAiPricingService) {
        super(repository, cnaeRepository, pipelineNichoCnaeRepository, openAiPricingService, STAGE_CODE);
    }

    /** Cria pendência inicial ou encadeada para a etapa persona-candidate-generator. */
    public PersonaCandidateGeneratorCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Inicia pendência da etapa para o CNAE informado pela tela administrativa. */
    public PersonaCandidateGeneratorCreateResponse start(String cnaeCode) {
        markCnaePipelineStarted(cnaeCode, STATUS_STARTED);
        return create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Recebe o request bruto da etapa e registra auditoria para o pipeline NichoCNAE v3. */
    public PersonaCandidateGeneratorCreateResponse recebeRequest(String cnaeCode, String jobId, OprmNichoCnaeV3RecebeRequestRequest request) {
        return new PersonaCandidateGeneratorCreateResponse(null, doRecebeRequest(cnaeCode, jobId, request).getJobId(), cnaeCode, STAGE_CODE, "AGUARDANDO_MODULO");
    }

    /** Recebe o response bruto da etapa e registra conclusão ou falha do pipeline NichoCNAE v3. */
    public OprmNichoCnaeV3RecebeResponseResponse recebeResponse(String cnaeCode, String jobId, OprmNichoCnaeV3RecebeResponseRequest request) {
        return doRecebeResponse(cnaeCode, jobId, request, NEXT_STAGE);
    }

    /** Lista pendências da etapa persona-candidate-generator para o executor OPRM. */
    public List<PersonaCandidateGeneratorPendingResponse> pending() {
        return pendingCnaes().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa persona-candidate-generator. */
    public PersonaCandidateGeneratorCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        return toCreateResponse(doComplete(stageExecutionId, outputPayload, nextStageCode));
    }

    /** Registra falha da etapa persona-candidate-generator. */
    public PersonaCandidateGeneratorCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private PersonaCandidateGeneratorCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new PersonaCandidateGeneratorCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private PersonaCandidateGeneratorPendingResponse toPendingResponse(OprmCnpjCnaeDim cnae) {
        OprmNichoCnaeV3StageExecution execution = pendingExecution(cnae).orElse(null);
        return new PersonaCandidateGeneratorPendingResponse(
                execution == null ? null : execution.getId(),
                execution == null ? pendingJobId(cnae) : execution.getJobId(),
                cnae.getCnaeCode(),
                execution == null ? cnaeInputPayload(cnae) : execution.getInputPayload(),
                execution == null ? 1 : execution.getAttemptNumber(),
                execution == null ? 1 : execution.getKnowledgeVersion());
    }
}
