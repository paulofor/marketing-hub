package com.marketinghub.moisclickbank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionRequest;
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
                "workspace-001",
                "marketing-digital",
                "ofertas-clickbank"
        );

        var response = service.collect(new ClickbankCollectionRequest("clickbank-market", 10));

        assertEquals("COLLECTION_ERROR", response.status());
    }
}
