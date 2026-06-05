package com.marketinghub.nichocnae.routinequalitygate.web;

import com.marketinghub.nichocnae.routinequalitygate.RoutineQualityGateOutput;
import com.marketinghub.nichocnae.routinequalitygate.RoutineQualityGatePending;
import com.marketinghub.nichocnae.routinequalitygate.RoutineQualityGateService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe acionamento manual complementar da etapa sete NichoCNAE no coletor OPRM. */
@RestController
@RequestMapping("/api/oprm-mei/nichocnae/routine-quality-gate")
public class RoutineQualityGateController {
    private final RoutineQualityGateService qualityGateService;

    /** Inicializa o controller com o serviço de execução sob demanda da etapa sete. */
    public RoutineQualityGateController(RoutineQualityGateService qualityGateService) {
        this.qualityGateService = qualityGateService;
    }

    /** Lista cartões pendentes de avaliação pela etapa sete. */
    @GetMapping("/pending")
    public ResponseEntity<List<RoutineQualityGatePending>> pending() {
        return ResponseEntity.ok(qualityGateService.listPendingCards());
    }

    /** Executa manualmente a etapa sete para todos os cartões pendentes retornados pelo backend. */
    @PostMapping("/process-pending")
    public ResponseEntity<List<RoutineQualityGateOutput>> processPending(
            @RequestHeader(value = "X-Requested-By", required = false, defaultValue = "MANUAL_API") String requestedBy) {
        return ResponseEntity.accepted().body(qualityGateService.processPending(requestedBy));
    }
}
