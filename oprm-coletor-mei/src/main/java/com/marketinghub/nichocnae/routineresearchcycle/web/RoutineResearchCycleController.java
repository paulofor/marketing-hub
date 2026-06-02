package com.marketinghub.nichocnae.routineresearchcycle.web;

import com.marketinghub.nichocnae.routineresearchcycle.RoutineResearchCycleDetail;
import com.marketinghub.nichocnae.routineresearchcycle.RoutineResearchCyclePending;
import com.marketinghub.nichocnae.routineresearchcycle.RoutineResearchCycleService;
import com.marketinghub.nichocnae.routineresearchcycle.RoutineResearchCycleSummary;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe acompanhamento e acionamento manual da etapa um nichocnae no coletor OPRM. */
@RestController
@RequestMapping("/api/oprm-mei/nichocnae/routine-research-cycle")
public class RoutineResearchCycleController {
    private final RoutineResearchCycleService routineResearchCycleService;

    /** Inicializa o controller com o serviço de execução sob demanda da etapa um. */
    public RoutineResearchCycleController(RoutineResearchCycleService routineResearchCycleService) {
        this.routineResearchCycleService = routineResearchCycleService;
    }

    /** Lista ciclos em execução que a etapa um pode controlar para continuidade do pipeline. */
    @GetMapping("/pending")
    public ResponseEntity<List<RoutineResearchCyclePending>> pending() {
        return ResponseEntity.ok(routineResearchCycleService.listPendingCycles());
    }

    /** Lista ciclos de pesquisa de rotina vinculados ao nicho CNAE de origem informado. */
    @GetMapping("/source-niches/{sourceNicheId}/stage-executions")
    public ResponseEntity<List<RoutineResearchCycleSummary>> listBySourceNicheId(@PathVariable Long sourceNicheId) {
        return ResponseEntity.ok(routineResearchCycleService.listBySourceNicheId(sourceNicheId));
    }

    /** Retorna detalhes de uma execução específica do ciclo de pesquisa de rotina. */
    @GetMapping("/stage-executions/{researchCycleId}")
    public ResponseEntity<RoutineResearchCycleDetail> detailStageExecution(@PathVariable Long researchCycleId) {
        return ResponseEntity.ok(routineResearchCycleService.detailStageExecution(researchCycleId));
    }

    /** Executa manualmente a etapa um para todos os ciclos pendentes retornados pelo backend. */
    @PostMapping("/process-pending")
    public ResponseEntity<List<RoutineResearchCycleDetail>> processPending(
            @RequestHeader(value = "X-Requested-By", required = false, defaultValue = "MANUAL_API") String requestedBy) {
        return ResponseEntity.accepted().body(routineResearchCycleService.processPending(requestedBy));
    }
}
