package com.marketinghub.oprm.market.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO responsável por expor o ranking operacional de CNAEs com volume de mercado e score OPRM.
 */
public record OprmTopCnaeMarketVolumeDto(
        LocalDate snapshotDate,
        String cnaeCode,
        String cnaeDescription,
        long totalEstabelecimentos,
        long totalEstabelecimentosAtivos,
        long totalEmpresas,
        long totalEmpresasMei,
        long totalEmpresasSimples,
        BigDecimal opportunityScore,
        String scoreStatus) {}
