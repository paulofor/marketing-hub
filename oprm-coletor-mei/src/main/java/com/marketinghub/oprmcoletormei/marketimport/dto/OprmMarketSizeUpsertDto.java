package com.marketinghub.oprmcoletormei.marketimport.dto;

public record OprmMarketSizeUpsertDto(
        String cnaeCode,
        Long totalEstabelecimentos,
        Long totalEstabelecimentosAtivos,
        Long totalEmpresas,
        Long totalEmpresasMei,
        Long totalEmpresasSimples,
        Double avgSociosPorEmpresa
) {}
