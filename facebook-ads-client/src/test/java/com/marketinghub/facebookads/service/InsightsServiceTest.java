package com.marketinghub.facebookads.service;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class InsightsServiceTest {
    @Test
    void fetchAndPostReadsMetrics() throws Exception {
        MockWebServer fb = new MockWebServer();
        fb.enqueue(new MockResponse().setBody("{\"data\":[{\"impressions\":10,\"clicks\":1,\"spend\":\"2\",\"website_purchase_roas\":\"3\"}]}"));
        fb.start();
        MockWebServer backend = new MockWebServer();
        backend.enqueue(new MockResponse().setResponseCode(200));
        backend.start();
        InsightsService service = new InsightsService("TOKEN", "v19.0", fb.url("/").toString(), backend.url("/").toString());
        service.fetchAndPost("1");
        assertThat(fb.getRequestCount()).isEqualTo(1);
        assertThat(backend.getRequestCount()).isEqualTo(1);
        fb.shutdown();
        backend.shutdown();
    }
}
