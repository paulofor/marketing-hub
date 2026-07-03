package com.marketinghub.productai.web;

import com.marketinghub.productai.dto.PersonalizedSamplePreparationDto;
import com.marketinghub.productai.dto.PersonalizedSampleFunnelDto;
import com.marketinghub.productai.dto.ProductAiExperimentPreparationDto;
import com.marketinghub.productai.service.ProductAiExperimentPreparationService;
import com.marketinghub.productai.service.ProductAiPersonalizedSampleFunnelService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o preparo sistêmico de hipóteses Produto IA antes da criação de experimento. */
@RestController
@RequestMapping("/api/product-ai")
public class ProductAiExperimentPreparationController {
    private final ProductAiExperimentPreparationService service;
    private final ProductAiPersonalizedSampleFunnelService funnelService;

    /** Inicializa o controller com o serviço de preparo de Produto IA. */
    public ProductAiExperimentPreparationController(
            ProductAiExperimentPreparationService service,
            ProductAiPersonalizedSampleFunnelService funnelService) {
        this.service = service;
        this.funnelService = funnelService;
    }

    /** Retorna bloqueios e rascunho canônico para a hipótese informada. */
    @GetMapping("/experiment-preparations/{hypothesisId}")
    public ProductAiExperimentPreparationDto get(@PathVariable UUID hypothesisId) {
        return service.prepare(hypothesisId);
    }

    /** Completa a hipótese existente para o MVP de amostra personalizada antes da criação do experimento. */
    @PostMapping("/hypotheses/{hypothesisId}/personalized-sample-preparation")
    public PersonalizedSamplePreparationDto preparePersonalizedSample(@PathVariable UUID hypothesisId) {
        return service.preparePersonalizedSampleHypothesis(hypothesisId);
    }

    /** Cria ou reaproveita o funil canônico que coleta dados do lead para a amostra personalizada. */
    @PostMapping("/experiments/{experimentId}/personalized-sample-funnel")
    public PersonalizedSampleFunnelDto createPersonalizedSampleFunnel(@PathVariable Long experimentId) {
        return funnelService.createOrUpdate(experimentId);
    }
}
