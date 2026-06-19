package com.marketinghub.oprm.nichocnae.v2.candidatetournament.controller;

import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.BackendCandidateTournamentService;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.completeStageExecution.CandidateTournamentCompletionRequest;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.completeStageExecution.CandidateTournamentCompletionResponse;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.createStageExecution.CandidateTournamentCreateRequest;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.createStageExecution.CandidateTournamentCreateResponse;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.failStageExecution.CandidateTournamentFailureRequest;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.failStageExecution.CandidateTournamentFailureResponse;
import com.marketinghub.oprm.nichocnae.v2.candidatetournament.service.pending.CandidateTournamentPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa candidate-tournament do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/candidate-tournament/stage-executions")
public class BackendCandidateTournamentController {
    private final BackendCandidateTournamentService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendCandidateTournamentController(BackendCandidateTournamentService service) {
        this.service = service;
    }

    /** Entrega execuções pendentes da etapa candidate-tournament ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<CandidateTournamentPendingResponse> pending() {
        return service.pending();
    }

    /** Grava uma pendência da etapa candidate-tournament solicitada pelo módulo executor OPRM. */
    @PostMapping
    public CandidateTournamentCreateResponse create(@RequestBody CandidateTournamentCreateRequest request) {
        return service.create(request);
    }

    /** Registra a conclusão da execução de torneio informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/complete")
    public CandidateTournamentCompletionResponse complete(
            @PathVariable Long stageExecutionId, @RequestBody CandidateTournamentCompletionRequest request) {
        return service.complete(stageExecutionId, request);
    }

    /** Registra a falha da execução de torneio informada pelo módulo executor OPRM. */
    @PostMapping("/{stageExecutionId}/fail")
    public CandidateTournamentFailureResponse fail(
            @PathVariable Long stageExecutionId, @RequestBody CandidateTournamentFailureRequest request) {
        return service.fail(stageExecutionId, request);
    }
}
