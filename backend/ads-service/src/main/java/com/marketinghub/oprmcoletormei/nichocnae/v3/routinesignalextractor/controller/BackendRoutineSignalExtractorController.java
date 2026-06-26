package com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.BackendRoutineSignalExtractorService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.completeStageExecution.RoutineSignalExtractorCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.createStageExecution.RoutineSignalExtractorCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.failStageExecution.RoutineSignalExtractorFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.pending.RoutineSignalExtractorPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa routine-signal-extractor do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/routine-signal-extractor/stage-executions")
public class BackendRoutineSignalExtractorController {
    private final BackendRoutineSignalExtractorService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendRoutineSignalExtractorController(BackendRoutineSignalExtractorService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/start")
    public RoutineSignalExtractorCreateResponse start(@RequestParam String cnaeCode) {
        return service.start(cnaeCode);
    }

    /** Cria uma execução pendente da etapa routine-signal-extractor. */
    @PostMapping
    public RoutineSignalExtractorCreateResponse create(@RequestBody RoutineSignalExtractorPendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public RoutineSignalExtractorCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Entrega pendências da etapa routine-signal-extractor ao executor OPRM. */
    @GetMapping("/pending")
    public List<RoutineSignalExtractorPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa routine-signal-extractor enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public RoutineSignalExtractorCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody RoutineSignalExtractorCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa routine-signal-extractor enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public RoutineSignalExtractorCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody RoutineSignalExtractorFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
