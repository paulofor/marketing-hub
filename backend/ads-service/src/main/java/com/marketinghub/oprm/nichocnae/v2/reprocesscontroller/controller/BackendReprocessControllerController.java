package com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.controller;

import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.BackendReprocessControllerService;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.completeStageExecution.ReprocessControllerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.completeStageExecution.ReprocessControllerCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.createStageExecution.ReprocessControllerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.createStageExecution.ReprocessControllerCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.failStageExecution.ReprocessControllerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.failStageExecution.ReprocessControllerFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.reprocesscontroller.service.pending.ReprocessControllerPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa reprocess-controller do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/reprocess-controller/stage-executions")
public class BackendReprocessControllerController {
    private final BackendReprocessControllerService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendReprocessControllerController(BackendReprocessControllerService service) { this.service = service; }

    /** Entrega execuções pendentes da etapa reprocess-controller ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<ReprocessControllerPendingResponse> pending() { return service.pending(); }

    /** Grava uma pendência da etapa reprocess-controller solicitada pelo módulo executor OPRM. */
    @PostMapping
    public ReprocessControllerCreateResponse create(@RequestBody ReprocessControllerCreateRequest request) { return service.create(request); }

    /** Registra a conclusão do controlador de reprocessamento informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public ReprocessControllerCompletionResponse complete(@PathVariable Long stageExecutionId, @RequestBody ReprocessControllerCompletionRequest request) { return service.complete(stageExecutionId, request); }

    /** Registra a falha do controlador de reprocessamento informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public ReprocessControllerFailureResponse fail(@PathVariable Long stageExecutionId, @RequestBody ReprocessControllerFailureRequest request) { return service.fail(stageExecutionId, request); }
}
