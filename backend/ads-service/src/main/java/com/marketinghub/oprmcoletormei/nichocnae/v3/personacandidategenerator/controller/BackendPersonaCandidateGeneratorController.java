package com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.controller;

import com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service.BackendPersonaCandidateGeneratorService;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service.completeStageExecution.PersonaCandidateGeneratorCompletionRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service.createStageExecution.PersonaCandidateGeneratorCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service.failStageExecution.PersonaCandidateGeneratorFailureRequest;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personacandidategenerator.service.pending.PersonaCandidateGeneratorPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3RecebeRequestRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller canônico da etapa persona-candidate-generator do pipeline NichoCNAE v3. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v3/persona-candidate-generator/stage-executions")
public class BackendPersonaCandidateGeneratorController {
    private final BackendPersonaCandidateGeneratorService service;

    /** Inicializa o controller com service canônico da etapa. */
    public BackendPersonaCandidateGeneratorController(BackendPersonaCandidateGeneratorService service) {
        this.service = service;
    }

    /** Inicia uma execução pendente da etapa a partir do CNAE informado pela tela. */
    @PostMapping("/start")
    public PersonaCandidateGeneratorCreateResponse start(@RequestParam String cnaeCode) {
        return service.start(cnaeCode);
    }

    /** Cria uma execução pendente da etapa persona-candidate-generator. */
    @PostMapping
    public PersonaCandidateGeneratorCreateResponse create(@RequestBody PersonaCandidateGeneratorPendingResponse request) {
        return service.create(request.jobId(), request.cnaeCode(), request.inputPayload(), request.attemptNumber(), request.knowledgeVersion());
    }

    /** Cria a primeira execução v3 diretamente a partir de um CNAE. */
    @PostMapping("/cnaes/{cnaeCode}")
    public PersonaCandidateGeneratorCreateResponse createForCnae(@PathVariable String cnaeCode) {
        return service.create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Recebe o request bruto da etapa identificado pelo CNAE. */
    @PostMapping("/cnaes/{cnaeCode}/recebeRequest")
    public PersonaCandidateGeneratorCreateResponse recebeRequest(@PathVariable String cnaeCode, @RequestBody OprmNichoCnaeV3RecebeRequestRequest request) {
        return service.recebeRequest(cnaeCode, request);
    }

    /** Entrega pendências da etapa persona-candidate-generator ao executor OPRM. */
    @GetMapping("/pending")
    public List<PersonaCandidateGeneratorPendingResponse> pending() {
        return service.pending();
    }

    /** Recebe conclusão da etapa persona-candidate-generator enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public PersonaCandidateGeneratorCreateResponse complete(@PathVariable Long stageExecutionId, @RequestBody PersonaCandidateGeneratorCompletionRequest request) {
        return service.complete(stageExecutionId, request.outputPayload(), request.nextStageCode());
    }

    /** Recebe falha da etapa persona-candidate-generator enviada pelo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public PersonaCandidateGeneratorCreateResponse fail(@PathVariable Long stageExecutionId, @RequestBody PersonaCandidateGeneratorFailureRequest request) {
        return service.fail(stageExecutionId, request.errorMessage());
    }
}
