package com.marketinghub.oprm.market.dto;

import java.time.LocalDate;

public record OprmTopCnaeMarketVolumeDto(
        LocalDate snapshotDate,
        String cnaeCode,
        String cnaeDescription,
        long totalEstabelecimentos,
        long totalEstabelecimentosAtivos,
        long totalEmpresas,
        long totalEmpresasMei,
        long totalEmpresasSimples) {}
