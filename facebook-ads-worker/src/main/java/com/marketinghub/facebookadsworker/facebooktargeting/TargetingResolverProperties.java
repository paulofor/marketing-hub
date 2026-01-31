package com.marketinghub.facebookadsworker.facebooktargeting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configurações para o resolver de candidatos de targeting.
 */
@Component
@ConfigurationProperties(prefix = "targeting.resolver")
public class TargetingResolverProperties {
    /** Locale padrão utilizado quando o request não informa. */
    private String defaultLocale = "pt_BR";
    /** País padrão (ISO alpha-2) utilizado como fallback. */
    private String defaultCountry = "BR";
    /** Limite máximo de itens solicitados ao targetingsearch. */
    private int searchLimit = 25;
    /** Quantidade máxima de opções retornadas por candidato. */
    private int resultLimit = 5;
    /** Tempo máximo para manter resultados em cache dentro do FacebookAdsService. */
    private Duration cacheTtl = Duration.ofMinutes(30);
    /** Conta de anúncio padrão usada quando o payload não fornece uma. */
    private String defaultAdAccountId;

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getDefaultCountry() {
        return defaultCountry;
    }

    public void setDefaultCountry(String defaultCountry) {
        this.defaultCountry = defaultCountry;
    }

    public int getSearchLimit() {
        return searchLimit;
    }

    public void setSearchLimit(int searchLimit) {
        this.searchLimit = searchLimit;
    }

    public int getResultLimit() {
        return resultLimit;
    }

    public void setResultLimit(int resultLimit) {
        this.resultLimit = resultLimit;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public String getDefaultAdAccountId() {
        return defaultAdAccountId;
    }

    public void setDefaultAdAccountId(String defaultAdAccountId) {
        this.defaultAdAccountId = defaultAdAccountId;
    }
}
