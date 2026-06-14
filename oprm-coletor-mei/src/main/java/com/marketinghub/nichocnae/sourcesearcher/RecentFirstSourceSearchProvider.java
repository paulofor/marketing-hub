package com.marketinghub.nichocnae.sourcesearcher;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Escolhe Google recente quando configurado e usa DuckDuckGo HTML como fallback operacional. */
@Primary
@Component
public class RecentFirstSourceSearchProvider implements PublicSourceSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(RecentFirstSourceSearchProvider.class);
    private static final String COMPOSITE_PROVIDER_CODE = "RECENT_FIRST_SEARCH";

    private final GoogleCustomSearchSourceSearchProvider googleProvider;
    private final DuckDuckGoHtmlSourceSearchProvider duckDuckGoProvider;
    private final ThreadLocal<String> lastProviderCode = ThreadLocal.withInitial(() -> COMPOSITE_PROVIDER_CODE);

    /** Inicializa o provedor composto com Google recente e fallback DuckDuckGo. */
    public RecentFirstSourceSearchProvider(
            GoogleCustomSearchSourceSearchProvider googleProvider,
            DuckDuckGoHtmlSourceSearchProvider duckDuckGoProvider) {
        this.googleProvider = googleProvider;
        this.duckDuckGoProvider = duckDuckGoProvider;
    }

    /** Executa busca priorizando fontes recentes do Google e preservando fallback sem travar o pipeline. */
    @Override
    public List<SourceSearchResult> search(String queryText, int maxResults) {
        if (googleProvider.configured()) {
            try {
                List<SourceSearchResult> googleResults = googleProvider.search(queryText, maxResults);
                if (!googleResults.isEmpty()) {
                    lastProviderCode.set(googleProvider.providerCode());
                    return googleResults;
                }
                log.warn("Google Custom Search não retornou fontes recentes; usando fallback DuckDuckGo HTML (queryText={})", queryText);
            } catch (RuntimeException ex) {
                log.error("Falha no Google Custom Search; usando fallback DuckDuckGo HTML (queryText={})", queryText, ex);
            }
        }
        List<SourceSearchResult> fallbackResults = duckDuckGoProvider.search(queryText, maxResults);
        lastProviderCode.set(duckDuckGoProvider.providerCode());
        return fallbackResults;
    }

    /** Informa o provedor que gerou a última busca da thread atual para persistência auditável. */
    @Override
    public String providerCode() {
        return lastProviderCode.get();
    }
}
