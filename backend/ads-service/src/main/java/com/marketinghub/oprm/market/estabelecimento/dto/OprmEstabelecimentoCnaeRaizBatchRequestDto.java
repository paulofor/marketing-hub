package com.marketinghub.oprm.market.estabelecimento.dto;

import java.util.List;

/**
 * Transporta um lote de estabelecimentos extraídos dos arquivos da Receita para persistência no backend.
 */
public record OprmEstabelecimentoCnaeRaizBatchRequestDto(
        List<OprmEstabelecimentoCnaeRaizUpsertDto> estabelecimentos
) {}
