package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.web;

import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.BackendRoutineResearchOrchestratorService;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.pending.RecordRoutineResearchOrchestratorPending;
import com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.runNext.RecordRoutineResearchOrchestratorResult;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por expor os endpoints backend da etapa zero do pipeline OPRM nichocnae. */
@RestController
@RequestMapping("/api")
public class BackendRoutineResearchOrchestratorController {
    private final BackendRoutineResearchOrchestratorService executionService;

    /** Inicializa o controller com o serviço backend da etapa zero de pesquisa de rotina. */
    public BackendRoutineResearchOrchestratorController(BackendRoutineResearchOrchestratorService executionService) {
        this.executionService = executionService;
    }

    /** Lista o próximo nicho CNAE pendente que será usado pelo orquestrador automático. */
    @GetMapping("/internal/oprm/nichocnae/routine-research-orchestrator/stage-executions/pending")
    public List<RecordRoutineResearchOrchestratorPending> pending() {
        return executionService.listPending();
    }

    /** Executa a etapa zero para iniciar o próximo ciclo de pesquisa de rotina por score. */
    @PostMapping("/internal/oprm/nichocnae/routine-research-orchestrator/run-next")
    public ResponseEntity<RecordRoutineResearchOrchestratorResult> runNext() {
        RecordRoutineResearchOrchestratorResult response = executionService.runNext();
        return response.started() ? ResponseEntity.accepted().body(response) : ResponseEntity.ok(response);
    }
}
