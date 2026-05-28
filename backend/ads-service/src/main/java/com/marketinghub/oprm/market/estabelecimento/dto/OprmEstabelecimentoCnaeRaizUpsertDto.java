package com.marketinghub.oprm.market.estabelecimento.dto;

/**
 * Representa uma linha normalizada de vínculo entre CNPJ raiz, CNAE principal e email de estabelecimento.
 */
public record OprmEstabelecimentoCnaeRaizUpsertDto(
        String cnpjRaiz,
        String cnaeCode,
        String email
) {}
