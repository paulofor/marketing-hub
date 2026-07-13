package com.marketinghub.experiment.salespagetype.service.updateselection;

import java.util.List;

/** Recebe a selecao completa de tipos de pagina de venda para substituir a configuracao atual. */
public record UpdateExperimentSalesPageTypeSelectionRequest(
        List<UpdateExperimentSalesPageTypeSelectionItem> selections) {
}
