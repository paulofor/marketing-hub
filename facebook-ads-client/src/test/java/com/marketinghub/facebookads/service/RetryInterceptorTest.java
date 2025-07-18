package com.marketinghub.facebookads.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class RetryInterceptorTest {
    @Test
    void retriesOn429() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(new MockResponse().setBody("ok"));
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .build();
        Request req = new Request.Builder().url(server.url("/")).build();
        String body = client.newCall(req).execute().body().string();
        assertThat(body).isEqualTo("ok");
        assertThat(server.getRequestCount()).isEqualTo(2);
        server.shutdown();
    }
}
