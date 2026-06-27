package com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.service.BackendDailyTasksSynthesizerService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.service.completeStageExecution.DailyTasksSynthesizerCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.service.createStageExecution.DailyTasksSynthesizerCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.service.failStageExecution.DailyTasksSynthesizerFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.dailytaskssynthesizer.service.pending.DailyTasksSynthesizerPendingResponse;
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

/** Controller canônico da etapa daily-tasks-synthesizer do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprmcoletormei/nichocnae/v3/daily-tasks-synthesizer/stage-executions")
public class BackendDailyTasksSynthesizerController {
    private static final Logger log = LoggerFactory.getLogger(BackendDailyTasksSynthesizerController.class);
    private final BackendDailyTasksSynthesizerService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendDailyTasksSynthesizerController(BackendDailyTasksSynthesizerService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/{idExterno}/start")
    public DailyTasksSynthesizerCreateResponse start(@PathVariable("idExterno") String cnaeCode) {
        return service.start(cnaeCode);
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

    /** Recebe o request bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public DailyTasksSynthesizerCreateResponse recebeRequest(@PathVariable("idExterno") String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeRequestRequest request) {
        return service.recebeRequest(cnaeCode, jobId, request);
    }

    /** Recebe o response bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public OprmNichoCnaeV3RecebeResponseResponse recebeResponse(@PathVariable("idExterno") String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeResponseRequest request) {
        log.info("Recebendo response NichoCNAE v3. etapa={}, cnaeCode={}, jobId={}, payload={}", service.stageCode(), cnaeCode, jobId, request);
        return service.recebeResponse(cnaeCode, jobId, request);
    }

    /** Entrega pendências da etapa daily-tasks-synthesizer ao executor OPRM. */
    @PostMapping("/pending")
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
