package com.marketinghub.experiment.salespagetype.controller;

import com.marketinghub.experiment.salespagetype.service.SalesPageTypeService;
import com.marketinghub.experiment.salespagetype.service.listtypes.ExperimentSalesPageTypeSelectionResponse;
import com.marketinghub.experiment.salespagetype.service.listtypes.SalesPageTypeResponse;
import com.marketinghub.experiment.salespagetype.service.updateselection.UpdateExperimentSalesPageTypeSelectionRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor catalogo e selecao de tipos de pagina de venda para campanhas. */
@RestController
@RequestMapping("/api")
public class SalesPageTypeController {
    private final SalesPageTypeService service;

    /** Inicializa o controller com o servico de tipos de pagina de venda. */
    public SalesPageTypeController(SalesPageTypeService service) {
        this.service = service;
    }

    /** Lista os tipos de pagina de venda ativos. */
    @GetMapping("/sales-page-types")
    public List<SalesPageTypeResponse> listTypes() {
        return service.listActiveTypes();
    }

    /** Lista os tipos de pagina de venda selecionados para o experimento. */
    @GetMapping("/experiments/{experimentId}/sales-page-types")
    public List<ExperimentSalesPageTypeSelectionResponse> listExperimentSelections(@PathVariable Long experimentId) {
        return service.listExperimentSelections(experimentId);
    }

    /** Substitui os tipos de pagina de venda selecionados para o experimento. */
    @PutMapping("/experiments/{experimentId}/sales-page-types")
    public List<ExperimentSalesPageTypeSelectionResponse> replaceExperimentSelections(
            @PathVariable Long experimentId,
            @RequestBody UpdateExperimentSalesPageTypeSelectionRequest request) {
        return service.replaceExperimentSelections(experimentId, request);
    }
}
