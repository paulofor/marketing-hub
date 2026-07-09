package com.marketinghub.experiment.salespageab.web;

import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbTestDto;
import com.marketinghub.experiment.salespageab.dto.UpdateExperimentSalesPageAbVariantRequest;
import com.marketinghub.experiment.salespageab.service.ExperimentSalesPageAbTestService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor operacoes de teste A/B de pagina de venda por experimento. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/sales-page-ab-tests")
public class ExperimentSalesPageAbTestController {
    private final ExperimentSalesPageAbTestService service;

    /** Inicializa o controller com o servico de testes A/B de pagina de venda. */
    public ExperimentSalesPageAbTestController(ExperimentSalesPageAbTestService service) {
        this.service = service;
    }

    /** Lista os testes A/B cadastrados no experimento. */
    @GetMapping
    public List<ExperimentSalesPageAbTestDto> list(@PathVariable Long experimentId) {
        return service.list(experimentId);
    }

    /** Cria o teste recomendado para Meta: tradicional contra video humano. */
    @PostMapping("/meta-video-vs-traditional")
    public ExperimentSalesPageAbTestDto createMetaVideoVsTraditional(@PathVariable Long experimentId) {
        return service.createMetaVideoVsTraditional(experimentId);
    }

    /** Atualiza os dados operacionais de uma variante do teste A/B. */
    @PatchMapping("/variants/{variantId}")
    public ExperimentSalesPageAbTestDto updateVariant(@PathVariable Long experimentId,
                                                      @PathVariable Long variantId,
                                                      @RequestBody UpdateExperimentSalesPageAbVariantRequest request) {
        return service.updateVariant(experimentId, variantId, request);
    }
}
