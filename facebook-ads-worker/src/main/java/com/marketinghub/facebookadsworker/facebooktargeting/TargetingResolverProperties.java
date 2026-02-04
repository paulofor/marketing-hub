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
    /** Controle de variações por seed. */
    private int maxSeedVariants = 6;
    /** Tempo máximo para manter resultados em cache dentro do FacebookAdsService. */
    private Duration cacheTtl = Duration.ofMinutes(30);
    /** Conta de anúncio padrão usada quando o payload não fornece uma. */
    private String defaultAdAccountId;
    /** Ativa a chamada de targetingsuggestions após encontrar ao menos uma opção via search. */
    private boolean suggestionsEnabled = true;
    /** Limite de sugestões retornadas pela API. */
    private int suggestionLimit = 10;
    /** Quantidade de seeds (IDs) usados como base das sugestões. */
    private int suggestionSeedLimit = 3;

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

    public int getMaxSeedVariants() {
        return maxSeedVariants;
    }

    public void setMaxSeedVariants(int maxSeedVariants) {
        this.maxSeedVariants = maxSeedVariants;
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

    public boolean isSuggestionsEnabled() {
        return suggestionsEnabled;
    }

    public void setSuggestionsEnabled(boolean suggestionsEnabled) {
        this.suggestionsEnabled = suggestionsEnabled;
    }

    public int getSuggestionLimit() {
        return suggestionLimit;
    }

    public void setSuggestionLimit(int suggestionLimit) {
        this.suggestionLimit = suggestionLimit;
    }

    public int getSuggestionSeedLimit() {
        return suggestionSeedLimit;
    }

    public void setSuggestionSeedLimit(int suggestionSeedLimit) {
        this.suggestionSeedLimit = suggestionSeedLimit;
    }
}
