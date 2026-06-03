package com.marketinghub.nichocnae.nicheresearchseedbuilder.web;

import com.marketinghub.nichocnae.nicheresearchseedbuilder.NicheResearchSeedBuilderOutput;
import com.marketinghub.nichocnae.nicheresearchseedbuilder.NicheResearchSeedBuilderPending;
import com.marketinghub.nichocnae.nicheresearchseedbuilder.NicheResearchSeedBuilderService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe acionamento manual complementar da etapa dois nichocnae no coletor OPRM. */
@RestController
@RequestMapping("/api/oprm-mei/nichocnae/niche-research-seed-builder")
public class NicheResearchSeedBuilderController {
    private final NicheResearchSeedBuilderService seedBuilderService;

    /** Inicializa o controller com o serviço de execução sob demanda da etapa dois. */
    public NicheResearchSeedBuilderController(NicheResearchSeedBuilderService seedBuilderService) {
        this.seedBuilderService = seedBuilderService;
    }

    /** Lista ciclos pendentes de geração de seed e queries da etapa dois. */
    @GetMapping("/pending")
    public ResponseEntity<List<NicheResearchSeedBuilderPending>> pending() {
        return ResponseEntity.ok(seedBuilderService.listPendingSeeds());
    }

    /** Detalha o seed e as queries gerados para o ciclo informado. */
    @GetMapping("/{researchCycleId}")
    public ResponseEntity<NicheResearchSeedBuilderOutput> detail(@PathVariable Long researchCycleId) {
        return ResponseEntity.ok(seedBuilderService.detailStageExecution(researchCycleId));
    }

    /** Executa manualmente a etapa dois para todos os ciclos pendentes retornados pelo backend. */
    @PostMapping("/process-pending")
    public ResponseEntity<List<NicheResearchSeedBuilderOutput>> processPending(
            @RequestHeader(value = "X-Requested-By", required = false, defaultValue = "MANUAL_API") String requestedBy) {
        return ResponseEntity.accepted().body(seedBuilderService.processPending(requestedBy));
    }
}
