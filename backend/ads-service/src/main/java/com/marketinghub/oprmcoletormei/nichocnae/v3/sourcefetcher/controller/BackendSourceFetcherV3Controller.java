package com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.BackendSourceFetcherV3Service;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.completeStageExecution.SourceFetcherCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.createStageExecution.SourceFetcherCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.failStageExecution.SourceFetcherFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcefetcher.service.pending.SourceFetcherPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa source-fetcher do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/source-fetcher/stage-executions")
public class BackendSourceFetcherV3Controller {
    private final BackendSourceFetcherV3Service service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendSourceFetcherV3Controller(BackendSourceFetcherV3Service service) {
        this.service = service;
    }

    /** Cria uma execução pendente da etapa source-fetcher. */
    @PostMapping
    public SourceFetcherCreateResponse create(@RequestBody SourceFetcherPendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public SourceFetcherCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Entrega pendências da etapa source-fetcher ao executor OPRM. */
    @GetMapping("/pending")
    public List<SourceFetcherPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa source-fetcher enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public SourceFetcherCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody SourceFetcherCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa source-fetcher enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public SourceFetcherCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody SourceFetcherFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
