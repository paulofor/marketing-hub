package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.controller;

import com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.BackendKnowledgeAccumulatorService;
import com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.completeStageExecution.KnowledgeAccumulatorCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.completeStageExecution.KnowledgeAccumulatorCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.createStageExecution.KnowledgeAccumulatorCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.createStageExecution.KnowledgeAccumulatorCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.failStageExecution.KnowledgeAccumulatorFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.failStageExecution.KnowledgeAccumulatorFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.pending.KnowledgeAccumulatorPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa knowledge-accumulator do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/knowledge-accumulator/stage-executions")
public class BackendKnowledgeAccumulatorController {
    private final BackendKnowledgeAccumulatorService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendKnowledgeAccumulatorController(BackendKnowledgeAccumulatorService service) {
        this.service = service;
    }

    /** Entrega execuções pendentes da etapa knowledge-accumulator ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<KnowledgeAccumulatorPendingResponse> pending() {
        return service.pending();
    }

    /** Grava uma pendência da etapa knowledge-accumulator solicitada pelo módulo executor OPRM. */
    @PostMapping
    public KnowledgeAccumulatorCreateResponse create(@RequestBody KnowledgeAccumulatorCreateRequest request) {
        return service.create(request);
    }

    /** Registra a conclusão da execução de acumulação de conhecimento informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public KnowledgeAccumulatorCompletionResponse complete(
            @PathVariable Long stageExecutionId, @RequestBody KnowledgeAccumulatorCompletionRequest request) {
        return service.complete(stageExecutionId, request);
    }

    /** Registra a falha da execução de acumulação de conhecimento informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public KnowledgeAccumulatorFailureResponse fail(
            @PathVariable Long stageExecutionId, @RequestBody KnowledgeAccumulatorFailureRequest request) {
        return service.fail(stageExecutionId, request);
    }
}
