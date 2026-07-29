package com.marketinghub.pde.infrastructure.service.listVps;

import java.math.BigDecimal;
import java.util.List;

/** Responsabilidade: expor a visão consolidada de VPS e custo fixo mensal dos PDEs. */
public record PdeVpsSummaryResponse(
    BigDecimal totalMonthlyCostBrl,
    int totalServers,
    int activeServers,
    List<PdeVpsServerResponse> servers) {}
