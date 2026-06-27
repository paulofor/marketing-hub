package com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.BackendRoutineSignalExtractorService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.completeStageExecution.RoutineSignalExtractorCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.createStageExecution.RoutineSignalExtractorCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.failStageExecution.RoutineSignalExtractorFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.pending.RoutineSignalExtractorPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeRequestRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa routine-signal-extractor do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprmcoletormei/nichocnae/v3/routine-signal-extractor/stage-executions")
public class BackendRoutineSignalExtractorController {
    private static final Logger log = LoggerFactory.getLogger(BackendRoutineSignalExtractorController.class);
    private final BackendRoutineSignalExtractorService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendRoutineSignalExtractorController(BackendRoutineSignalExtractorService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/{idExterno}/start")
    public RoutineSignalExtractorCreateResponse start(@PathVariable("idExterno") String cnaeCode) {
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

    /** Recebe o request bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public RoutineSignalExtractorCreateResponse recebeRequest(@PathVariable("idExterno") String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeRequestRequest request) {
        return service.recebeRequest(cnaeCode, jobId, request);
    }

    /** Recebe o response bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public OprmNichoCnaeV3RecebeResponseResponse recebeResponse(@PathVariable("idExterno") String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeResponseRequest request) {
        log.info("Recebendo response NichoCNAE v3. etapa={}, cnaeCode={}, jobId={}, payload={}", service.stageCode(), cnaeCode, jobId, request);
        return service.recebeResponse(cnaeCode, jobId, request);
    }

    /** Entrega pendências da etapa routine-signal-extractor ao executor OPRM. */
    @PostMapping("/pending")
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
