package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.controller;

import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.BackendAdaptiveQueryPlannerService;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.completeStageExecution.AdaptiveQueryPlannerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.completeStageExecution.AdaptiveQueryPlannerCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.createStageExecution.AdaptiveQueryPlannerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.createStageExecution.AdaptiveQueryPlannerCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.failStageExecution.AdaptiveQueryPlannerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.failStageExecution.AdaptiveQueryPlannerFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.pending.AdaptiveQueryPlannerPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa adaptive-query-planner do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/adaptive-query-planner/stage-executions")
public class BackendAdaptiveQueryPlannerController {
    private final BackendAdaptiveQueryPlannerService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendAdaptiveQueryPlannerController(BackendAdaptiveQueryPlannerService service) {
        this.service = service;
    }

    /** Entrega execuções pendentes da etapa adaptive-query-planner ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<AdaptiveQueryPlannerPendingResponse> pending() {
        return service.pending();
    }

    /** Grava uma pendência da etapa adaptive-query-planner solicitada pelo módulo executor OPRM. */
    @PostMapping
    public AdaptiveQueryPlannerCreateResponse create(@RequestBody AdaptiveQueryPlannerCreateRequest request) {
        return service.create(request);
    }

    /** Registra a conclusão da execução de planejamento informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public AdaptiveQueryPlannerCompletionResponse complete(
            @PathVariable Long stageExecutionId, @RequestBody AdaptiveQueryPlannerCompletionRequest request) {
        return service.complete(stageExecutionId, request);
    }

    /** Registra a falha da execução de planejamento informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public AdaptiveQueryPlannerFailureResponse fail(
            @PathVariable Long stageExecutionId, @RequestBody AdaptiveQueryPlannerFailureRequest request) {
        return service.fail(stageExecutionId, request);
    }
}
