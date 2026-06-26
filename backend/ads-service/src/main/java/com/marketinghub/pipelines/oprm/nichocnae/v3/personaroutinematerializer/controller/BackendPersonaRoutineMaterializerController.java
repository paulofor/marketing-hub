package com.marketinghub.pipelines.oprm.nichocnae.v3.personaroutinematerializer.controller;

import com.marketinghub.pipelines.oprm.nichocnae.v3.personaroutinematerializer.service.BackendPersonaRoutineMaterializerService;
import com.marketinghub.pipelines.oprm.nichocnae.v3.personaroutinematerializer.service.completeStageExecution.PersonaRoutineMaterializerCompletionRequest;
import com.marketinghub.pipelines.oprm.nichocnae.v3.personaroutinematerializer.service.createStageExecution.PersonaRoutineMaterializerCreateResponse;
import com.marketinghub.pipelines.oprm.nichocnae.v3.personaroutinematerializer.service.failStageExecution.PersonaRoutineMaterializerFailureRequest;
import com.marketinghub.pipelines.oprm.nichocnae.v3.personaroutinematerializer.service.pending.PersonaRoutineMaterializerPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa persona-routine-materializer do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/persona-routine-materializer/stage-executions")
public class BackendPersonaRoutineMaterializerController {
    private final BackendPersonaRoutineMaterializerService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendPersonaRoutineMaterializerController(BackendPersonaRoutineMaterializerService service) {
        this.service = service;
    }

    /** Cria uma execução pendente da etapa persona-routine-materializer. */
    @PostMapping
    public PersonaRoutineMaterializerCreateResponse create(@RequestBody PersonaRoutineMaterializerPendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public PersonaRoutineMaterializerCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Entrega pendências da etapa persona-routine-materializer ao executor OPRM. */
    @GetMapping("/pending")
    public List<PersonaRoutineMaterializerPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa persona-routine-materializer enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public PersonaRoutineMaterializerCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody PersonaRoutineMaterializerCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa persona-routine-materializer enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public PersonaRoutineMaterializerCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody PersonaRoutineMaterializerFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
