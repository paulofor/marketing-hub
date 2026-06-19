package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.controller;

import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.BackendEnrichedNicheMaterializerService;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.completeStageExecution.EnrichedNicheMaterializerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.completeStageExecution.EnrichedNicheMaterializerCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.createStageExecution.EnrichedNicheMaterializerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.createStageExecution.EnrichedNicheMaterializerCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.failStageExecution.EnrichedNicheMaterializerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.failStageExecution.EnrichedNicheMaterializerFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.pending.EnrichedNicheMaterializerPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa enriched-niche-materializer do pipeline NichoCNAE versão 2. */
@RestController("oprmNichocnaeV2BackendEnrichedNicheMaterializerController")
@RequestMapping("/api/internal/oprm/nichocnae/v2/enriched-niche-materializer/stage-executions")
public class BackendEnrichedNicheMaterializerController {
    private final BackendEnrichedNicheMaterializerService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendEnrichedNicheMaterializerController(BackendEnrichedNicheMaterializerService service) {
        this.service = service;
    }

    /** Entrega execuções pendentes da etapa enriched-niche-materializer ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<EnrichedNicheMaterializerPendingResponse> pending() {
        return service.pending();
    }

    /** Grava uma pendência da etapa enriched-niche-materializer solicitada pelo módulo executor OPRM. */
    @PostMapping
    public EnrichedNicheMaterializerCreateResponse create(@RequestBody EnrichedNicheMaterializerCreateRequest request) {
        return service.create(request);
    }

    /** Registra a conclusão do materializador de nicho enriquecido informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public EnrichedNicheMaterializerCompletionResponse complete(
            @PathVariable Long stageExecutionId, @RequestBody EnrichedNicheMaterializerCompletionRequest request) {
        return service.complete(stageExecutionId, request);
    }

    /** Registra a falha do materializador de nicho enriquecido informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public EnrichedNicheMaterializerFailureResponse fail(
            @PathVariable Long stageExecutionId, @RequestBody EnrichedNicheMaterializerFailureRequest request) {
        return service.fail(stageExecutionId, request);
    }
}
