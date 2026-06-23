package com.marketinghub.opsmonitor.service.listAvailabilityHistory;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ponto diário do histórico de disponibilidade de um módulo. */
public record ModuleAvailabilityHistoryResponse(LocalDate date, Integer totalChecks, Integer successfulChecks,
        Integer failedChecks, BigDecimal availabilityPercentage, Long offlineSeconds, Long degradedSeconds) {}
