package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.controller;

import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.BackendCommercialEvidenceGateService;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.completeStageExecution.CommercialEvidenceGateCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.completeStageExecution.CommercialEvidenceGateCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.createStageExecution.CommercialEvidenceGateCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.createStageExecution.CommercialEvidenceGateCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.failStageExecution.CommercialEvidenceGateFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.failStageExecution.CommercialEvidenceGateFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.pending.CommercialEvidenceGatePendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa commercial-evidence-gate do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/commercial-evidence-gate/stage-executions")
public class BackendCommercialEvidenceGateController {
    private final BackendCommercialEvidenceGateService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendCommercialEvidenceGateController(BackendCommercialEvidenceGateService service) {
        this.service = service;
    }

    /** Entrega execuções pendentes da etapa commercial-evidence-gate ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<CommercialEvidenceGatePendingResponse> pending() {
        return service.pending();
    }

    /** Grava uma pendência da etapa commercial-evidence-gate solicitada pelo módulo executor OPRM. */
    @PostMapping
    public CommercialEvidenceGateCreateResponse create(@RequestBody CommercialEvidenceGateCreateRequest request) {
        return service.create(request);
    }

    /** Registra a conclusão do gate comercial informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public CommercialEvidenceGateCompletionResponse complete(
            @PathVariable Long stageExecutionId, @RequestBody CommercialEvidenceGateCompletionRequest request) {
        return service.complete(stageExecutionId, request);
    }

    /** Registra a falha do gate comercial informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public CommercialEvidenceGateFailureResponse fail(
            @PathVariable Long stageExecutionId, @RequestBody CommercialEvidenceGateFailureRequest request) {
        return service.fail(stageExecutionId, request);
    }
}
