package com.marketinghub.moisclickbank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionRequest;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

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

        var response = service.collect(new ClickbankCollectionRequest("clickbank-market", 10));

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
                    true, "", "https://app.clickbank.com/market/search", "", "", "",
                    topOffersUrl, "", "", false, "http://127.0.0.1:1",
                    "clickbank_access_token_jwt", "https://accounts.clickbank.com/graphql",
                    "workspace-001", "marketing-digital", "ofertas-clickbank"
            );
            var response = service.collect(new ClickbankCollectionRequest("clickbank-market", 10));

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
}
