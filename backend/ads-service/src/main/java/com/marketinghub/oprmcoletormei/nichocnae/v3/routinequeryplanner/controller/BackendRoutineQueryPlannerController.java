package com.marketinghub.oprmcoletormei.nichocnae.v3.routinequeryplanner.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.routinequeryplanner.service.BackendRoutineQueryPlannerService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinequeryplanner.service.completeStageExecution.RoutineQueryPlannerCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinequeryplanner.service.createStageExecution.RoutineQueryPlannerCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinequeryplanner.service.failStageExecution.RoutineQueryPlannerFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.routinequeryplanner.service.pending.RoutineQueryPlannerPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeRequestRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeResponseResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa routine-query-planner do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/routine-query-planner/stage-executions")
public class BackendRoutineQueryPlannerController {
    private static final Logger log = LoggerFactory.getLogger(BackendRoutineQueryPlannerController.class);
    private final BackendRoutineQueryPlannerService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendRoutineQueryPlannerController(BackendRoutineQueryPlannerService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/start")
    public RoutineQueryPlannerCreateResponse start(@RequestParam String cnaeCode) {
        return service.start(cnaeCode);
    }

    /** Cria uma execução pendente da etapa routine-query-planner. */
    @PostMapping
    public RoutineQueryPlannerCreateResponse create(@RequestBody RoutineQueryPlannerPendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public RoutineQueryPlannerCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Recebe o request bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/cnaes/{cnaeCode}/jobs/{jobId}/recebeRequest")
    public RoutineQueryPlannerCreateResponse recebeRequest(@PathVariable String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeRequestRequest request) {
        return service.recebeRequest(cnaeCode, jobId, request);
    }

    /** Recebe o response bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/cnaes/{cnaeCode}/jobs/{jobId}/recebeResponse")
    public OprmNichoCnaeV3RecebeResponseResponse recebeResponse(@PathVariable String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeResponseRequest request) {
        log.info("Recebendo response NichoCNAE v3. etapa={}, cnaeCode={}, jobId={}, payload={}", service.stageCode(), cnaeCode, jobId, request);
        return service.recebeResponse(cnaeCode, jobId, request);
    }

    /** Entrega pendências da etapa routine-query-planner ao executor OPRM. */
    @GetMapping("/pending")
    public List<RoutineQueryPlannerPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa routine-query-planner enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public RoutineQueryPlannerCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody RoutineQueryPlannerCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa routine-query-planner enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public RoutineQueryPlannerCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody RoutineQueryPlannerFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
