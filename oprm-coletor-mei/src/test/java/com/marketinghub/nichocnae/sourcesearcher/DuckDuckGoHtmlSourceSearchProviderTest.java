package com.marketinghub.nichocnae.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a localização Brasil-first aplicada às buscas públicas da etapa três. */
class DuckDuckGoHtmlSourceSearchProviderTest {
    private final DuckDuckGoHtmlSourceSearchProvider provider = new DuckDuckGoHtmlSourceSearchProvider();

    /** Deve adicionar marcador Brasil quando a query ainda não explicita o mercado brasileiro. */
    @Test
    void shouldAppendBrazilWhenQueryHasNoBrazilianMarketMarker() {
        assertThat(provider.buildBrazilianMarketQuery("rotina manicure agenda clientes"))
                .isEqualTo("rotina manicure agenda clientes Brasil");
    }

    /** Deve preservar queries que já trazem marcador brasileiro para evitar duplicidade artificial. */
    @Test
    void shouldKeepQueryWhenBrazilianMarketMarkerAlreadyExists() {
        assertThat(provider.buildBrazilianMarketQuery("rotina manicure Brasil"))
                .isEqualTo("rotina manicure Brasil");
        assertThat(provider.buildBrazilianMarketQuery("rotina manicure site:.br"))
                .isEqualTo("rotina manicure site:.br");
    }
}
