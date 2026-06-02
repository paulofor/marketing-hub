package com.marketinghub.nichocnae.routineresearchorchestrator.web;

import com.marketinghub.nichocnae.routineresearchorchestrator.RoutineResearchOrchestratorOutput;
import com.marketinghub.nichocnae.routineresearchorchestrator.RoutineResearchOrchestratorPending;
import com.marketinghub.nichocnae.routineresearchorchestrator.RoutineResearchOrchestratorService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe acionamento manual da etapa zero nichocnae no coletor OPRM, sem agendamento automático. */
@RestController
@RequestMapping("/api/oprm-mei/nichocnae/routine-research-orchestrator")
public class RoutineResearchOrchestratorController {
    private final RoutineResearchOrchestratorService orchestratorService;

    /** Inicializa o controller com o serviço de execução sob demanda da etapa zero. */
    public RoutineResearchOrchestratorController(RoutineResearchOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    /** Lista o próximo nicho pendente que o backend selecionaria para iniciar pesquisa de rotina. */
    @GetMapping("/pending")
    public ResponseEntity<List<RoutineResearchOrchestratorPending>> pending() {
        return ResponseEntity.ok(orchestratorService.listPendingCandidates());
    }

    /** Executa manualmente a etapa zero e devolve o ciclo criado ou a mensagem de fila vazia. */
    @PostMapping("/run-next")
    public ResponseEntity<RoutineResearchOrchestratorOutput> runNext(
            @RequestHeader(value = "X-Requested-By", required = false, defaultValue = "MANUAL_API") String requestedBy) {
        RoutineResearchOrchestratorOutput response = orchestratorService.runNext(requestedBy);
        return response.started() ? ResponseEntity.accepted().body(response) : ResponseEntity.ok(response);
    }
}
