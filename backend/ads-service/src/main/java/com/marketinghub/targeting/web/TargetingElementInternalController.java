package com.marketinghub.targeting.web;

import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationFailureRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationPendingDto;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationResultRequest;
import com.marketinghub.targeting.service.TargetingElementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints internos para o AI Worker consumir e reportar geração de públicos sem acessar o banco.
 */
@RestController
@RequestMapping("/api/internal/targeting-elements/generation")
public class TargetingElementInternalController {
    private final TargetingElementService service;

    /** Inicializa o controller interno com o serviço canônico de elementos de targeting. */
    public TargetingElementInternalController(TargetingElementService service) {
        this.service = service;
    }

    /** Lista pendências de geração de públicos para consumo exclusivo do AI Worker. */
    @GetMapping("/pending")
    public List<TargetingElementGenerationPendingDto> listPending(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return service.listPendingGeneration(limit);
    }

    /** Recebe os públicos gerados pelo AI Worker e materializa os elementos no backend. */
    @PostMapping("/{nicheId}/{type}/results")
    public void saveResults(@PathVariable Long nicheId,
                            @PathVariable TargetingElementType type,
                            @RequestBody TargetingElementGenerationResultRequest request) {
        service.saveGeneratedElements(nicheId, type, request);
    }

    /** Registra falha reportada pelo AI Worker e libera a pendência para evitar loop operacional. */
    @PostMapping("/{nicheId}/{type}/failure")
    public void markFailure(@PathVariable Long nicheId,
                            @PathVariable TargetingElementType type,
                            @RequestBody TargetingElementGenerationFailureRequest request) {
        service.markGenerationFailure(nicheId, type, request);
    }
}
