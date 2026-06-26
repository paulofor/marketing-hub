package com.marketinghub.oprmcoletormei.nichocnae.v3.sourcesearcher.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcesearcher.service.BackendSourceSearcherV3Service;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcesearcher.service.completeStageExecution.SourceSearcherCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcesearcher.service.createStageExecution.SourceSearcherCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcesearcher.service.failStageExecution.SourceSearcherFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.sourcesearcher.service.pending.SourceSearcherPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeRequestRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa source-searcher do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/source-searcher/stage-executions")
public class BackendSourceSearcherV3Controller {
    private final BackendSourceSearcherV3Service service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendSourceSearcherV3Controller(BackendSourceSearcherV3Service service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/start")
    public SourceSearcherCreateResponse start(@RequestParam String cnaeCode) {
        return service.start(cnaeCode);
    }

    /** Cria uma execução pendente da etapa source-searcher. */
    @PostMapping
    public SourceSearcherCreateResponse create(@RequestBody SourceSearcherPendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public SourceSearcherCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Recebe o request bruto da etapa identificado pelo CNAE. */
    @PostMapping("/cnaes/{cnaeCode}/recebeRequest")
    public SourceSearcherCreateResponse recebeRequest(@PathVariable String cnaeCode, @RequestBody OprmNichoCnaeV3RecebeRequestRequest request) {
        return service.recebeRequest(cnaeCode, request);
    }

    /** Entrega pendências da etapa source-searcher ao executor OPRM. */
    @GetMapping("/pending")
    public List<SourceSearcherPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa source-searcher enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public SourceSearcherCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody SourceSearcherCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa source-searcher enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public SourceSearcherCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody SourceSearcherFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
