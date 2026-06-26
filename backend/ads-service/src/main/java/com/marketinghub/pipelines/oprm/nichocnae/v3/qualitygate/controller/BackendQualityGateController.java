package com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.controller;

import com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.BackendQualityGateService;
import com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.completeStageExecution.QualityGateCompletionRequest;
import com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.createStageExecution.QualityGateCreateResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.failStageExecution.QualityGateFailureRequest;
import com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.pending.QualityGatePendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa quality-gate do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/quality-gate/stage-executions")
public class BackendQualityGateController {
    private final BackendQualityGateService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendQualityGateController(BackendQualityGateService service) {
        this.service = service;
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

    /** Entrega pendências da etapa quality-gate ao executor OPRM. */
    @GetMapping("/pending")
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
