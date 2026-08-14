package com.marketinghub.moishotmart.web;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
import com.marketinghub.moishotmart.service.HotmartCollectorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mois-hotmart")
/** Expõe health e comandos explícitos de coleta autenticada da Hotmart. */
public class HotmartCollectorController {

    private final HotmartCollectorService collectorService;

    /** Inicializa o controller com o serviço canônico do coletor. */
    public HotmartCollectorController(HotmartCollectorService collectorService) {
        this.collectorService = collectorService;
    }

    /** Confirma que o processo HTTP do coletor está disponível. */
    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    /** Executa uma coleta opcionalmente dirigida por consulta e categoria. */
    @PostMapping("/collections")
    public HotmartCollectionResponse collect(@Valid @RequestBody HotmartCollectionRequest request) {
        return collectorService.collect(request);
    }
}
