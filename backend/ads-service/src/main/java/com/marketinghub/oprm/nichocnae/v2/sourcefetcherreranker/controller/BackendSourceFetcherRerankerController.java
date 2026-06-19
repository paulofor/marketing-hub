package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.controller;

import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.BackendSourceFetcherRerankerService;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.completeStageExecution.SourceFetcherRerankerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.completeStageExecution.SourceFetcherRerankerCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.createStageExecution.SourceFetcherRerankerCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.createStageExecution.SourceFetcherRerankerCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.failStageExecution.SourceFetcherRerankerFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.failStageExecution.SourceFetcherRerankerFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.pending.SourceFetcherRerankerPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa source-fetcher-reranker do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/source-fetcher-reranker/stage-executions")
public class BackendSourceFetcherRerankerController {
    private final BackendSourceFetcherRerankerService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendSourceFetcherRerankerController(BackendSourceFetcherRerankerService service) {
        this.service = service;
    }

    /** Entrega execuções pendentes da etapa source-fetcher-reranker ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<SourceFetcherRerankerPendingResponse> pending() {
        return service.pending();
    }

    /** Grava uma pendência da etapa source-fetcher-reranker solicitada pelo módulo executor OPRM. */
    @PostMapping
    public SourceFetcherRerankerCreateResponse create(@RequestBody SourceFetcherRerankerCreateRequest request) {
        return service.create(request);
    }

    /** Registra a conclusão da execução de coleta e reranking informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public SourceFetcherRerankerCompletionResponse complete(
            @PathVariable Long stageExecutionId, @RequestBody SourceFetcherRerankerCompletionRequest request) {
        return service.complete(stageExecutionId, request);
    }

    /** Registra a falha da execução de coleta e reranking informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public SourceFetcherRerankerFailureResponse fail(
            @PathVariable Long stageExecutionId, @RequestBody SourceFetcherRerankerFailureRequest request) {
        return service.fail(stageExecutionId, request);
    }
}
