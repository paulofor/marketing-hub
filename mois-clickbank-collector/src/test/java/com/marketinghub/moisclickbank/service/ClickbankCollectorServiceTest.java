package com.marketinghub.moisclickbank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionRequest;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Valida os contratos de coleta e persistência do coletor ClickBank. */
class ClickbankCollectorServiceTest {

    @Test
    void shouldReturnCollectionErrorWhenPublicPageIsUnavailable() {
        ClickbankCollectorService service = new ClickbankCollectorService(
                true,
                "",
                "https://app.clickbank.com/market/search",
                "",
                "",
                "",
                "",
                "",
                "http://127.0.0.1:1/unreachable-top-offers",
                "",
                "",
                false,
                "http://localhost:8000",
                "clickbank_access_token_jwt",
                "https://accounts.clickbank.com/graphql",
                "workspace-001",
                "marketing-digital",
                "ofertas-clickbank"
        );

        var response = service.collectFirstCycle(new ClickbankCollectionRequest("clickbank-market", 10));

        assertEquals("COLLECTION_ERROR", response.status());
    }

    @Test
    void shouldExtractNicknameCategoryAndLandingPageFromTopOffersHtml() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/top-offers", exchange -> {
            String html = """
                    <html><body>
                      <h2>1) <a href="/market/product/vin-checkup">VIN Checkup</a></h2>
                      <p><strong>Nickname:</strong> vincheckup</p>
                      <p><strong>Category:</strong> Software &amp; Services</p>
                      <p><a href="https://get.vincheckup.com/">Check out their landing page here.</a></p>
                    </body></html>
                    """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        String topOffersUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/top-offers";
        try {
            ClickbankCollectorService service = new ClickbankCollectorService(
                    true, "", "https://app.clickbank.com/market/search", "", "", "", "", "",
                    topOffersUrl, "", "", false, "http://127.0.0.1:1",
                    "clickbank_access_token_jwt", "https://accounts.clickbank.com/graphql",
                    "workspace-001", "marketing-digital", "ofertas-clickbank"
            );
            var response = service.collectFirstCycle(new ClickbankCollectionRequest("clickbank-market", 10));

            assertEquals("COLLECTION_EXECUTED", response.status());
            assertEquals(1, response.products().size());
            var product = response.products().getFirst();
            assertEquals("VIN Checkup", product.title());
            assertEquals("vincheckup", product.rating());
            assertEquals("Software &amp; Services", product.commission());
            assertEquals("https://www.clickbank.com/market/product/vin-checkup", product.detailsUrl());
            assertEquals("https://get.vincheckup.com/", product.salesPageUrl());
            assertTrue(product.collectedAt() != null);
        } finally {
            server.stop(0);
        }
    }

    /** Garante que identidades repetidas não descartem um lote inteiro no backend. */
    @Test
    void shouldPersistOnlyOneReferenceWhenTopOffersShareTheSameStableIdentity() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<String> persistedPayload = new AtomicReference<>();
        server.createContext("/top-offers", exchange -> {
            String html = """
                    <html><body>
                      <h2>1) <a href="/market/product/shared-offer">Primeira descrição</a></h2>
                      <p><strong>Nickname:</strong> first</p>
                      <p><strong>Category:</strong> Health</p>
                      <h2>2) <a href="/market/product/shared-offer">Segunda descrição</a></h2>
                      <p><strong>Nickname:</strong> second</p>
                      <p><strong>Category:</strong> Health</p>
                    </body></html>
                    """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.createContext("/api/v1/mois/persistence/collection-jobs", exchange -> {
            persistedPayload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            ClickbankCollectorService service = new ClickbankCollectorService(
                    true, "", "https://app.clickbank.com/market/search", "", "", "", "", "",
                    base + "/top-offers", "", "", false, base,
                    "clickbank_access_token_jwt", "https://accounts.clickbank.com/graphql",
                    "workspace-001", "marketing-digital", "ofertas-clickbank"
            );

            var response = service.collectFirstCycle(new ClickbankCollectionRequest("clickbank-market", 10));

            assertEquals("COLLECTION_EXECUTED", response.status());
            assertEquals(2, response.products().size());
            assertEquals(
                    1,
                    new ObjectMapper().readTree(persistedPayload.get()).path("references").size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnSkippedWithJwtAbsentReasonOnThirdCycle() {
        ClickbankCollectorService service = new ClickbankCollectorService(
                true, "", "https://app.clickbank.com/market/search", "", "", "", "", "",
                "http://127.0.0.1:1", "", "", false, "http://127.0.0.1:1",
                "clickbank_access_token_jwt", "https://accounts.clickbank.com/graphql",
                "workspace-001", "marketing-digital", "ofertas-clickbank"
        );

        var response = service.collectThirdCycleGraphql(new ClickbankCollectionRequest("clickbank-market", 10));

        assertEquals("COLLECTION_SKIPPED", response.status());
        assertTrue(response.message().contains("motivo=JWT_ABSENT"));
    }

    @Test
    void shouldReturnSkippedWithJwtExpiredReasonWhenGraphqlReturns401() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/settings/clickbank_access_token_jwt", exchange -> {
            String body = "{\"name\":\"clickbank_access_token_jwt\",\"value\":\"valid-looking-token\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/graphql", exchange -> {
            String body = "{\"errors\":[{\"message\":\"token expired\"}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            ClickbankCollectorService service = new ClickbankCollectorService(
                    true, "", "https://app.clickbank.com/market/search", "", "", "", "", "",
                    "http://127.0.0.1:1", "", "", false, base,
                    "clickbank_access_token_jwt", base + "/graphql",
                    "workspace-001", "marketing-digital", "ofertas-clickbank"
            );

            var response = service.collectThirdCycleGraphql(new ClickbankCollectionRequest("clickbank-market", 10));

            assertEquals("COLLECTION_SKIPPED", response.status());
            assertTrue(response.message().contains("motivo=JWT_EXPIRED_OR_INVALID"));
        } finally {
            server.stop(0);
        }
    }
}
