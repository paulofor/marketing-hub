package com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.service.BackendCnaeIntakeService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.service.completeStageExecution.CnaeIntakeCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.service.createStageExecution.CnaeIntakeCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.service.failStageExecution.CnaeIntakeFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.cnaeintake.service.pending.CnaeIntakePendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa cnae-intake do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/cnae-intake/stage-executions")
public class BackendCnaeIntakeController {
    private final BackendCnaeIntakeService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendCnaeIntakeController(BackendCnaeIntakeService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/start")
    public CnaeIntakeCreateResponse start(@RequestParam String cnaeCode) {
        return service.start(cnaeCode);
    }

    /** Cria uma execução pendente da etapa cnae-intake. */
    @PostMapping
    public CnaeIntakeCreateResponse create(@RequestBody CnaeIntakePendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public CnaeIntakeCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Entrega pendências da etapa cnae-intake ao executor OPRM. */
    @GetMapping("/pending")
    public List<CnaeIntakePendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa cnae-intake enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public CnaeIntakeCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody CnaeIntakeCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa cnae-intake enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public CnaeIntakeCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody CnaeIntakeFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
