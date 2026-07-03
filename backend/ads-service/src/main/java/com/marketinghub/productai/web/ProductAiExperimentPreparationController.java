package com.marketinghub.productai.web;

import com.marketinghub.productai.dto.ProductAiExperimentPreparationDto;
import com.marketinghub.productai.service.ProductAiExperimentPreparationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o preparo sistêmico de hipóteses Produto IA antes da criação de experimento. */
@RestController
@RequestMapping("/api/product-ai/experiment-preparations")
public class ProductAiExperimentPreparationController {
    private final ProductAiExperimentPreparationService service;

    /** Inicializa o controller com o serviço de preparo de Produto IA. */
    public ProductAiExperimentPreparationController(ProductAiExperimentPreparationService service) {
        this.service = service;
    }

    /** Retorna bloqueios e rascunho canônico para a hipótese informada. */
    @GetMapping("/{hypothesisId}")
    public ProductAiExperimentPreparationDto get(@PathVariable UUID hypothesisId) {
        return service.prepare(hypothesisId);
    }
}
