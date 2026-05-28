package com.marketinghub.oprm.market.estabelecimento.dto;

/**
 * Informa a quantidade de vínculos CNPJ raiz/CNAE recebidos e gravados em lote.
 */
public record OprmEstabelecimentoCnaeRaizBatchResponseDto(
        int received,
        int persisted
) {}
