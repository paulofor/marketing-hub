package com.marketinghub.oprm.nichocnae.v3.progress.controller;

import com.marketinghub.oprm.nichocnae.v3.progress.service.BackendNichoCnaeV3ProgressService;
import com.marketinghub.oprm.nichocnae.v3.progress.service.NichoCnaeV3JobProgressResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller de leitura do progresso do pipeline NichoCNAE v3 para a tela administrativa. */
@RestController
@RequestMapping("/api/oprm/nichocnae/v3/cnaes/{cnaeCode}/progress")
public class BackendNichoCnaeV3ProgressController {
    private final BackendNichoCnaeV3ProgressService service;

    /** Inicializa o controller com service de leitura do progresso v3. */
    public BackendNichoCnaeV3ProgressController(BackendNichoCnaeV3ProgressService service) {
        this.service = service;
    }

    /** Expõe o progresso do job mais recente do CNAE informado. */
    @GetMapping
    public NichoCnaeV3JobProgressResponse latestByCnae(@PathVariable String cnaeCode) {
        return service.latestByCnae(cnaeCode);
    }

    /** Confirma a revisão exibida na tela e libera a etapa final do pipeline. */
    @PostMapping("/confirm-finalization")
    public void confirmFinalization(@PathVariable String cnaeCode) {
        service.confirmFinalization(cnaeCode);
    }
}
