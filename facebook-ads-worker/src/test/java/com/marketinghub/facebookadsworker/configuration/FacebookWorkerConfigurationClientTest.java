package com.marketinghub.facebookadsworker.configuration;

import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class FacebookWorkerConfigurationClientTest {

    private MockWebServer backend;

    @BeforeEach
    void setUp() throws IOException {
        backend = new MockWebServer();
        backend.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        backend.shutdown();
    }

    @Test
    void shouldReturnEmptyWhenBackendClosesConnectionBeforeResponding() throws InterruptedException {
        backend.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

        FacebookWorkerConfigurationClient client = new FacebookWorkerConfigurationClient(
            WebClient.builder(),
            backend.url("/").toString(),
            "/api"
        );

        Optional<FacebookWorkerConfiguration> configuration = client.fetchConfiguration();

        assertThat(configuration).isEmpty();
        var recordedRequest = backend.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getPath()).isEqualTo("/api/accounts/facebook/worker-config");
    }
}

