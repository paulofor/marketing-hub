package com.marketinghub.moishotmart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartProductSnapshot;
import java.time.Instant;
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
     * Garante que o ciclo 1 esteja configurado para percorrer vinte páginas completas da Hotmart.
     */
    @Test
    void shouldConfigureFirstCycleForTwentyHotmartPages() {
        assertEquals(20, HotmartCollectorService.HOTMART_ROWS_PER_PAGE);
        assertEquals(20, HotmartCollectorService.HOTMART_MAX_PAGES_PER_RUN);
        assertEquals(400, HotmartCollectorService.HOTMART_MAX_PRODUCTS_PER_RUN);
    }

    /**
     * Garante que pedidos sem limite explícito usem o alvo operacional de quatrocentos produtos.
     */
    @Test
    void shouldUseFourHundredProductsWhenRequestLimitIsMissing() {
        assertEquals(400, HotmartCollectorService.boundedFirstCycleProductLimit(0));
        assertEquals(400, HotmartCollectorService.boundedFirstCycleProductLimit(-1));
        assertEquals(400, HotmartCollectorService.boundedFirstCycleProductLimit(500));
    }

    /**
     * Garante que a deduplicação priorize o identificador da Hotmart antes do nome comercial.
     */
    @Test
    void shouldBuildStableDeduplicationKeyFromUcode() {
        assertEquals(
                "ucode:abc-123",
                HotmartCollectorService.buildHotmartProductDeduplicationKey(
                        " ABC-123 ",
                        "Produto Teste",
                        "Produtor",
                        "https://example.com"
                )
        );
    }

    /**
     * Garante que a deduplicação use nome e produtor quando a Hotmart não enviar ucode.
     */
    @Test
    void shouldBuildStableDeduplicationKeyFromTitleAndProducer() {
        assertEquals(
                "title:produto teste|producer:produtor oficial",
                HotmartCollectorService.buildHotmartProductDeduplicationKey(
                        "",
                        " Produto   Teste ",
                        " Produtor Oficial ",
                        "https://example.com"
                )
        );
    }

    /**
     * Garante que o mesmo produto mantenha referenceId estável em coletas futuras diferentes.
     */
    @Test
    void shouldBuildStableReferenceIdFromHotmartProductCode() {
        HotmartProductSnapshot product = new HotmartProductSnapshot(
                " ABC-123 ",
                "Produto Teste",
                null,
                "4.8",
                100,
                90.0,
                "N/A",
                199.0,
                "Negócios",
                "Curso",
                "Descrição comercial",
                "Produtor Oficial",
                "https://app.hotmart.com/products/abc",
                88.5,
                "https://example.com/sales",
                Instant.parse("2026-06-13T12:00:00Z")
        );

        assertEquals("hotmart-abc-123", HotmartCollectorService.buildStableHotmartReferenceId(1, product));
    }

}
