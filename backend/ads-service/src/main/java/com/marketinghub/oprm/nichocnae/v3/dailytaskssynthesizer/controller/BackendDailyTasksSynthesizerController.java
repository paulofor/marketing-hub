package com.marketinghub.oprm.nichocnae.v3.dailytaskssynthesizer.controller;

import com.marketinghub.oprm.nichocnae.v3.dailytaskssynthesizer.service.BackendDailyTasksSynthesizerService;
import com.marketinghub.oprm.nichocnae.v3.dailytaskssynthesizer.service.completeStageExecution.DailyTasksSynthesizerCompletionRequest;
import com.marketinghub.oprm.nichocnae.v3.dailytaskssynthesizer.service.createStageExecution.DailyTasksSynthesizerCreateResponse;
import com.marketinghub.oprm.nichocnae.v3.dailytaskssynthesizer.service.failStageExecution.DailyTasksSynthesizerFailureRequest;
import com.marketinghub.oprm.nichocnae.v3.dailytaskssynthesizer.service.pending.DailyTasksSynthesizerPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa daily-tasks-synthesizer do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/daily-tasks-synthesizer/stage-executions")
public class BackendDailyTasksSynthesizerController {
    private final BackendDailyTasksSynthesizerService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendDailyTasksSynthesizerController(BackendDailyTasksSynthesizerService service) {
        this.service = service;
    }

    /** Cria uma execução pendente da etapa daily-tasks-synthesizer. */
    @PostMapping
    public DailyTasksSynthesizerCreateResponse create(@RequestBody DailyTasksSynthesizerPendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public DailyTasksSynthesizerCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Entrega pendências da etapa daily-tasks-synthesizer ao executor OPRM. */
    @GetMapping("/pending")
    public List<DailyTasksSynthesizerPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa daily-tasks-synthesizer enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public DailyTasksSynthesizerCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody DailyTasksSynthesizerCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa daily-tasks-synthesizer enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public DailyTasksSynthesizerCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody DailyTasksSynthesizerFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
