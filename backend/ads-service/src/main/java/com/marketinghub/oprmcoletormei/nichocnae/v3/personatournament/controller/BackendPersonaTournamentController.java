package com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.BackendPersonaTournamentService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.completeStageExecution.PersonaTournamentCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.createStageExecution.PersonaTournamentCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.failStageExecution.PersonaTournamentFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.pending.PersonaTournamentPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa persona-tournament do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/persona-tournament/stage-executions")
public class BackendPersonaTournamentController {
    private final BackendPersonaTournamentService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendPersonaTournamentController(BackendPersonaTournamentService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/start")
    public PersonaTournamentCreateResponse start(@RequestParam String cnaeCode) {
        return service.start(cnaeCode);
    }

    /** Cria uma execução pendente da etapa persona-tournament. */
    @PostMapping
    public PersonaTournamentCreateResponse create(@RequestBody PersonaTournamentPendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public PersonaTournamentCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Entrega pendências da etapa persona-tournament ao executor OPRM. */
    @GetMapping("/pending")
    public List<PersonaTournamentPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa persona-tournament enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public PersonaTournamentCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody PersonaTournamentCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa persona-tournament enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public PersonaTournamentCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody PersonaTournamentFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
