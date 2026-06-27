package com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service.BackendQualityGateService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service.completeStageExecution.QualityGateCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service.createStageExecution.QualityGateCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service.failStageExecution.QualityGateFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.qualitygate.service.pending.QualityGatePendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeRequestRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa quality-gate do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprmcoletormei/nichocnae/v3/quality-gate/stage-executions")
public class BackendQualityGateController {
    private static final Logger log = LoggerFactory.getLogger(BackendQualityGateController.class);
    private final BackendQualityGateService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendQualityGateController(BackendQualityGateService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/{idExterno}/start")
    public QualityGateCreateResponse start(@PathVariable("idExterno") String cnaeCode) {
        return service.start(cnaeCode);
    }

    /** Cria uma execução pendente da etapa quality-gate. */
    @PostMapping
    public QualityGateCreateResponse create(@RequestBody QualityGatePendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public QualityGateCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Recebe o request bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public QualityGateCreateResponse recebeRequest(@PathVariable("idExterno") String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeRequestRequest request) {
        return service.recebeRequest(cnaeCode, jobId, request);
    }

    /** Recebe o response bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public OprmNichoCnaeV3RecebeResponseResponse recebeResponse(@PathVariable("idExterno") String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeResponseRequest request) {
        log.info("Recebendo response NichoCNAE v3. etapa={}, cnaeCode={}, jobId={}, payload={}", service.stageCode(), cnaeCode, jobId, request);
        return service.recebeResponse(cnaeCode, jobId, request);
    }

    /** Entrega pendências da etapa quality-gate ao executor OPRM. */
    @PostMapping("/pending")
    public List<QualityGatePendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa quality-gate enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public QualityGateCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody QualityGateCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa quality-gate enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public QualityGateCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody QualityGateFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
