package com.marketinghub.worker.hotmart;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotmartClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void fetchTopProductsReturnsParsedItems() {
        String body = "{\"items\":[{\"id\":\"1\",\"name\":\"Produto A\",\"temperature\":100.0},{\"id\":\"2\",\"name\":\"Produto B\",\"temperature\":95.0}]}";
        server.enqueue(new MockResponse().setBody(body).addHeader("Content-Type", "application/json"));

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();

        HotmartClient client = new HotmartClient(webClient);
        List<HotmartProduct> products = client.fetchTopProducts(2);

        assertEquals(2, products.size());
        assertEquals("Produto A", products.get(0).getName());
    }
}
