package com.marketinghub.moishotmart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import org.junit.jupiter.api.Test;

/**
 * Valida os estados operacionais do coletor Hotmart em cenários de token ausente ou expirado.
 */
class HotmartCollectorServiceTest {

    /**
     * Garante que a coleta é ignorada quando não há token configurado no backend.
     */
    @Test
    void shouldSkipWhenSessionAndCredentialsAreMissing() {
        HotmartCollectorService service = new HotmartCollectorService(
                true,
                "",
                "https://app.hotmart.com/market/search",
                "",
                "",
                "",
                "",
                "",
                false,
                "http://localhost:8000",
                "hotmart_access_token_jwt",
                "workspace-001",
                "marketing-digital",
                "ofertas-hotmart"
        );

        var response = service.collect(new HotmartCollectionRequest("hotmart-market", 10));

        assertEquals("COLLECTION_SKIPPED", response.status());
    }

    /**
     * Garante que a resposta 401 com JWT expirado gera mensagem acionável para atualização do token.
     */
    @Test
    void shouldClassifyExpiredJwtAsTokenUpdateRequired() {
        String body = "{\"error\":\"invalid_token\",\"error_description\":\"Expired JWT\"}";

        assertTrue(HotmartCollectorService.isHotmartTokenUpdateRequired(401, body));
        assertEquals(
                "Token JWT da Hotmart expirado ou inválido. Atualize o token na tela Hotmart para liberar o próximo ciclo de coleta.",
                HotmartCollectorService.buildHotmartApiFailureMessage(401, body)
        );
    }

    /**
     * Garante que o ciclo 1 esteja configurado para percorrer vinte e cinco páginas completas da Hotmart.
     */
    @Test
    void shouldConfigureFirstCycleForTwentyFiveHotmartPages() {
        assertEquals(20, HotmartCollectorService.HOTMART_ROWS_PER_PAGE);
        assertEquals(25, HotmartCollectorService.HOTMART_MAX_PAGES_PER_RUN);
        assertEquals(500, HotmartCollectorService.HOTMART_MAX_PRODUCTS_PER_RUN);
    }

}
