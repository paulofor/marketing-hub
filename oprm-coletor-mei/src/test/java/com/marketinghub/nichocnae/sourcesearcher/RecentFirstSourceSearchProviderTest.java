package com.marketinghub.nichocnae.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a escolha recent-first entre Google configurado e fallback DuckDuckGo. */
class RecentFirstSourceSearchProviderTest {

    /** Deve usar Google quando configurado e houver fontes recentes retornadas. */
    @Test
    void shouldUseGoogleWhenConfiguredAndResultsExist() {
        GoogleCustomSearchSourceSearchProvider google = mock(GoogleCustomSearchSourceSearchProvider.class);
        DuckDuckGoHtmlSourceSearchProvider duckDuckGo = mock(DuckDuckGoHtmlSourceSearchProvider.class);
        SourceSearchResult result = result("https://exemplo.com.br/recente");
        when(google.configured()).thenReturn(true);
        when(google.search("manicure agenda", 20)).thenReturn(List.of(result));
        when(google.providerCode()).thenReturn("GOOGLE_CUSTOM_SEARCH_RECENT");
        RecentFirstSourceSearchProvider provider = new RecentFirstSourceSearchProvider(google, duckDuckGo);

        List<SourceSearchResult> results = provider.search("manicure agenda", 20);

        assertThat(results).containsExactly(result);
        assertThat(provider.providerCode()).isEqualTo("GOOGLE_CUSTOM_SEARCH_RECENT");
        verifyNoInteractions(duckDuckGo);
    }

    /** Deve usar DuckDuckGo quando Google não estiver configurado para não interromper o pipeline. */
    @Test
    void shouldUseDuckDuckGoWhenGoogleIsNotConfigured() {
        GoogleCustomSearchSourceSearchProvider google = mock(GoogleCustomSearchSourceSearchProvider.class);
        DuckDuckGoHtmlSourceSearchProvider duckDuckGo = mock(DuckDuckGoHtmlSourceSearchProvider.class);
        SourceSearchResult result = result("https://fallback.com.br/fonte");
        when(google.configured()).thenReturn(false);
        when(duckDuckGo.search("manicure agenda", 20)).thenReturn(List.of(result));
        when(duckDuckGo.providerCode()).thenReturn("DUCKDUCKGO_HTML");
        RecentFirstSourceSearchProvider provider = new RecentFirstSourceSearchProvider(google, duckDuckGo);

        List<SourceSearchResult> results = provider.search("manicure agenda", 20);

        assertThat(results).containsExactly(result);
        assertThat(provider.providerCode()).isEqualTo("DUCKDUCKGO_HTML");
        verify(duckDuckGo).search("manicure agenda", 20);
    }

    /** Cria resultado mínimo para validar roteamento entre provedores. */
    private SourceSearchResult result(String url) {
        return new SourceSearchResult(url, "Fonte recente", "Resumo", "exemplo.com.br", 1, null, null, false, false);
    }
}
