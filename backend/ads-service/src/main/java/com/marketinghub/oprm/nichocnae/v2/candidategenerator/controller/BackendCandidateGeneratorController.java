package com.marketinghub.oprm.nichocnae.v2.candidategenerator.controller;

import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.BackendCandidateGeneratorService;
import com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.pending.CandidateGeneratorPendingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP interna da etapa candidate-generator do pipeline NichoCNAE versão 2. */
@RestController
@RequestMapping("/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions")
public class BackendCandidateGeneratorController {
    private final BackendCandidateGeneratorService service;

    /** Recebe o service canônico da etapa para delegar operações HTTP internas. */
    public BackendCandidateGeneratorController(BackendCandidateGeneratorService service) {
        this.service = service;
    }

    /** Entrega execuções pendentes da etapa candidate-generator ao módulo executor OPRM. */
    @GetMapping("/pending")
    public List<CandidateGeneratorPendingResponse> pending() {
        return service.pending();
    }
}
