package com.marketinghub.moishotmart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import org.junit.jupiter.api.Test;

class HotmartCollectorServiceTest {

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
                "hotmart_access_token_jwt"
        );

        var response = service.collect(new HotmartCollectionRequest("hotmart-market", 10));

        assertEquals("COLLECTION_SKIPPED", response.status());
    }
}
