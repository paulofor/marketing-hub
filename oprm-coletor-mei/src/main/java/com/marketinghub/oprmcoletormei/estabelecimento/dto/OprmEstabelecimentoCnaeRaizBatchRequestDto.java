package com.marketinghub.oprmcoletormei.estabelecimento.dto;

import java.util.List;

/**
 * Transporta para o backend um lote de vínculos extraídos dos arquivos ESTABELECIMENTOS.
 */
public record OprmEstabelecimentoCnaeRaizBatchRequestDto(
        List<OprmEstabelecimentoCnaeRaizUpsertDto> estabelecimentos
) {}
