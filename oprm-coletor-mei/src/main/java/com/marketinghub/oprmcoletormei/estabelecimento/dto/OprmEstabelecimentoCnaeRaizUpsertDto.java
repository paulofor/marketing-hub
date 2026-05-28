package com.marketinghub.oprmcoletormei.estabelecimento.dto;

/**
 * Representa o vínculo mínimo de CNPJ raiz, CNAE principal e email extraído de ESTABELECIMENTOS.
 */
public record OprmEstabelecimentoCnaeRaizUpsertDto(
        String cnpjRaiz,
        String cnaeCode,
        String email
) {}
