package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.controller;

import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.BackendSourceSafetyFilterService;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.completeStageExecution.SourceSafetyFilterCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.completeStageExecution.SourceSafetyFilterCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.createStageExecution.SourceSafetyFilterCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.createStageExecution.SourceSafetyFilterCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.failStageExecution.SourceSafetyFilterFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.failStageExecution.SourceSafetyFilterFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.pending.SourceSafetyFilterPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa source-safety-filter do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/source-safety-filter/stage-executions")
public class BackendSourceSafetyFilterController {
    private final BackendSourceSafetyFilterService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendSourceSafetyFilterController(BackendSourceSafetyFilterService service) {
        this.service = service;
    }

    /** Entrega execuções pendentes da etapa source-safety-filter ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<SourceSafetyFilterPendingResponse> pending() {
        return service.pending();
    }

    /** Grava uma pendência da etapa source-safety-filter solicitada pelo módulo executor OPRM. */
    @PostMapping
    public SourceSafetyFilterCreateResponse create(@RequestBody SourceSafetyFilterCreateRequest request) {
        return service.create(request);
    }

    /** Registra a conclusão da execução de segurança informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public SourceSafetyFilterCompletionResponse complete(
            @PathVariable Long stageExecutionId, @RequestBody SourceSafetyFilterCompletionRequest request) {
        return service.complete(stageExecutionId, request);
    }

    /** Registra a falha da execução de segurança informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public SourceSafetyFilterFailureResponse fail(
            @PathVariable Long stageExecutionId, @RequestBody SourceSafetyFilterFailureRequest request) {
        return service.fail(stageExecutionId, request);
    }
}
