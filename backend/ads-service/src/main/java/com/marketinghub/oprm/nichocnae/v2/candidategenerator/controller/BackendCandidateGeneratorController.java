package com.marketinghub.oprm.nichocnae.v2.candidategenerator.controller;

import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.BackendCandidateGeneratorService;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.completeStageExecution.CandidateGeneratorCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.createStageExecution.CandidateGeneratorCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution.CandidateGeneratorFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.pending.CandidateGeneratorPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa candidate-generator do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions")
public class BackendCandidateGeneratorController {
    private final BackendCandidateGeneratorService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendCandidateGeneratorController(BackendCandidateGeneratorService service) {
        this.service = service;
    }

    /** Grava um novo job da etapa candidate-generator para o CNAE selecionado na UI. */
    @PostMapping("/cnaes/{cnaeCode}")
    public CandidateGeneratorCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.createForCnae(cnaeCode);
    }

    /** Entrega execuções pendentes da etapa candidate-generator ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<CandidateGeneratorPendingResponse> pending() {
        return service.pending();
    }

    /** Registra a conclusão da execução de etapa informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public CandidateGeneratorCompletionResponse complete(
            @PathVariable Long stageExecutionId, @RequestBody CandidateGeneratorCompletionRequest request) {
        return service.complete(stageExecutionId, request);
    }

    /** Registra a falha da execução de etapa informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public CandidateGeneratorFailureResponse fail(
            @PathVariable Long stageExecutionId, @RequestBody CandidateGeneratorFailureRequest request) {
        return service.fail(stageExecutionId, request);
    }
}
