package com.marketinghub.facebookadsworker.configuration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.testsupport.FailFastMockWebServer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class FacebookWorkerConfigurationClientTest {

    private FailFastMockWebServer backend;

    @BeforeEach
    void setUp() throws IOException {
        backend = new FailFastMockWebServer();
        backend.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        backend.assertNoUnmatchedRequests();
        backend.shutdown();
    }

    @Test
    void shouldReturnEmptyWhenBackendClosesConnectionBeforeResponding() throws InterruptedException {
        backend.enqueueResponse(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

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

    @Test
    void logsMissingConfigurationWarningOnlyOncePerOutage() {
        backend.enqueueResponse(new MockResponse().setResponseCode(404));
        backend.enqueueResponse(new MockResponse().setResponseCode(404));
        backend.enqueueResponse(new MockResponse()
            .setBody("{\"accountId\":1,\"adAccountId\":\"act_1\",\"accessToken\":\"token\",\"appId\":\"app\",\"appSecret\":\"secret\","
                + "\"defaultPageId\":\"42\",\"defaultInstagramActorId\":\"24\",\"defaultWebsiteUrl\":\"https://example.com\","
                + "\"defaultCreativeMessageTemplate\":\"Conheça %s\",\"defaultCallToActionType\":\"LEARN_MORE\",\"adSetDailyBudget\":\"2000\","
                + "\"adSetBillingEvent\":\"IMPRESSIONS\",\"adSetOptimizationGoal\":\"LINK_CLICKS\",\"adSetDestinationType\":\"WEBSITE\","
                + "\"adSetBidStrategy\":\"LOWEST_COST_WITHOUT_CAP\",\"adSetBidAmount\":\"150\",\"adSetTargetCountry\":\"BR\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setResponseCode(404));

        FacebookWorkerConfigurationClient client = new FacebookWorkerConfigurationClient(
            WebClient.builder(),
            backend.url("/").toString(),
            "/api"
        );

        Logger logger = (Logger) LoggerFactory.getLogger(FacebookWorkerConfigurationClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThat(client.fetchConfiguration()).isEmpty();
            assertThat(client.fetchConfiguration()).isEmpty();
            assertThat(client.fetchConfiguration()).isPresent();
            assertThat(client.fetchConfiguration()).isEmpty();

            List<String> warnings = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

            assertThat(warnings)
                .hasSize(2)
                .allMatch(message -> message.equals("Facebook worker configuration not found in backend; skipping Facebook automation"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
