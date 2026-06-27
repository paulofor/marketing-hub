package com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.BackendPersonaTournamentService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.completeStageExecution.PersonaTournamentCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.createStageExecution.PersonaTournamentCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.failStageExecution.PersonaTournamentFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personatournament.service.pending.PersonaTournamentPendingResponse;
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

/** Controller canônico da etapa persona-tournament do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprmcoletormei/nichocnae/v3/persona-tournament/stage-executions")
public class BackendPersonaTournamentController {
    private static final Logger log = LoggerFactory.getLogger(BackendPersonaTournamentController.class);
    private final BackendPersonaTournamentService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendPersonaTournamentController(BackendPersonaTournamentService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/{idExterno}/start")
    public PersonaTournamentCreateResponse start(@PathVariable("idExterno") String cnaeCode) {
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

    /** Recebe o request bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public PersonaTournamentCreateResponse recebeRequest(@PathVariable("idExterno") String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeRequestRequest request) {
        return service.recebeRequest(cnaeCode, jobId, request);
    }

    /** Recebe o response bruto da etapa identificado pelo CNAE e jobId. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public OprmNichoCnaeV3RecebeResponseResponse recebeResponse(@PathVariable("idExterno") String cnaeCode, @PathVariable String jobId, @RequestBody OprmNichoCnaeV3RecebeResponseRequest request) {
        log.info("Recebendo response NichoCNAE v3. etapa={}, cnaeCode={}, jobId={}, payload={}", service.stageCode(), cnaeCode, jobId, request);
        return service.recebeResponse(cnaeCode, jobId, request);
    }

    /** Entrega pendências da etapa persona-tournament ao executor OPRM. */
    @PostMapping("/pending")
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
