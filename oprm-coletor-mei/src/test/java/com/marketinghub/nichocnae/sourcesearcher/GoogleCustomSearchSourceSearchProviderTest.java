package com.marketinghub.nichocnae.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Responsabilidade: validar a configuração e a query recente Brasil-first do provedor Google. */
class GoogleCustomSearchSourceSearchProviderTest {

    /** Deve considerar Google configurado somente quando habilitado com chave e motor de busca. */
    @Test
    void shouldRequireEnabledApiKeyAndSearchEngineId() {
        GoogleCustomSearchProperties disabled = new GoogleCustomSearchProperties(false, "key", "cx", null, null, null, null);
        GoogleCustomSearchProperties missingKey = new GoogleCustomSearchProperties(true, "", "cx", null, null, null, null);
        GoogleCustomSearchProperties configured = new GoogleCustomSearchProperties(true, "key", "cx", null, null, null, null);

        assertThat(disabled.configured()).isFalse();
        assertThat(missingKey.configured()).isFalse();
        assertThat(configured.configured()).isTrue();
    }

    /** Deve reforçar Brasil e anos recentes na query enviada ao Google. */
    @Test
    void shouldBuildRecentBrazilianQuery() {
        GoogleCustomSearchSourceSearchProvider provider = new GoogleCustomSearchSourceSearchProvider(
                new GoogleCustomSearchProperties(true, "key", "cx", null, null, null, null),
                RestClient.builder().build());

        assertThat(provider.buildRecentBrazilianQuery("manicure faltas clientes"))
                .isEqualTo("manicure faltas clientes Brasil 2025 OR 2026");
        assertThat(provider.buildRecentBrazilianQuery("manicure faltas clientes Brasil"))
                .isEqualTo("manicure faltas clientes Brasil 2025 OR 2026");
    }
}
