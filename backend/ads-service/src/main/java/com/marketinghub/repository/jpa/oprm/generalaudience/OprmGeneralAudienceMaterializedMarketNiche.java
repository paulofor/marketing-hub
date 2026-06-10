package com.marketinghub.repository.jpa.oprm.generalaudience;

/** Referência do MarketNiche materializado para manter o OPRM desacoplado do domínio de nicho. */
public record OprmGeneralAudienceMaterializedMarketNiche(
        Long id,
        String name,
        boolean reusedExisting
) {
}
