package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Descreve o plano estruturado de entregaveis antes da montagem final.
 */
public record DeliverablePlan(String requestId, String packageTitle, List<DeliverableSpec> deliverables) {
}
