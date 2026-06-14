package com.marketinghub.nichocnae.sourcesearcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Configura o provedor Google Custom Search para reforçar buscas recentes do NichoCNAE. */
@ConfigurationProperties(prefix = "oprm.nichocnae.source-searcher.google")
public record GoogleCustomSearchProperties(
        boolean enabled,
        String apiKey,
        String searchEngineId,
        String baseUrl,
        String dateRestrict,
        String countryRestrict,
        String languageRestrict) {
    /** Indica se o provedor Google tem credenciais e pode ser usado como fonte primária. */
    public boolean configured() {
        return enabled && StringUtils.hasText(apiKey) && StringUtils.hasText(searchEngineId);
    }

    /** Retorna a URL base efetiva da API de busca customizada do Google. */
    public String effectiveBaseUrl() {
        return StringUtils.hasText(baseUrl) ? baseUrl : "https://www.googleapis.com/customsearch/v1";
    }

    /** Retorna o recorte temporal efetivo para privilegiar fontes recentes. */
    public String effectiveDateRestrict() {
        return StringUtils.hasText(dateRestrict) ? dateRestrict : "m24";
    }

    /** Retorna o filtro de país efetivo para preservar o foco Brasil-first. */
    public String effectiveCountryRestrict() {
        return StringUtils.hasText(countryRestrict) ? countryRestrict : "countryBR";
    }

    /** Retorna o filtro de idioma efetivo para preservar português do Brasil. */
    public String effectiveLanguageRestrict() {
        return StringUtils.hasText(languageRestrict) ? languageRestrict : "lang_pt";
    }
}
