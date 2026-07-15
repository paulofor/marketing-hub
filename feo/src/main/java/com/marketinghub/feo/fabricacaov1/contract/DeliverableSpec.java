package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Define um entregavel planejado com papel comercial, formato e criterios de qualidade.
 */
public record DeliverableSpec(
        String code,
        String title,
        String componentType,
        String format,
        String role,
        String consumptionOrder,
        List<String> qualityCriteria,
        List<String> sections) {
}
