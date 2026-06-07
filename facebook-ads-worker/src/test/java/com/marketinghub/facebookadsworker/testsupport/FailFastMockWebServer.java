package com.marketinghub.facebookadsworker.testsupport;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Provides a MockWebServer wrapper that fails tests when unexpected HTTP calls are received.
 */
public final class FailFastMockWebServer {
    private final MockWebServer server = new MockWebServer();
    private final Queue<MockResponse> responses = new ArrayDeque<>();
    private final List<ConditionalResponse> priorityConditionalResponses = new ArrayList<>();
    private final List<ConditionalResponse> conditionalResponses = new ArrayList<>();
    private final AtomicReference<RecordedRequest> unmatchedRequest = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    /**
     * Configures the dispatcher with priority conditionals, queued responses and fallback conditionals.
     */
    public FailFastMockWebServer() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                requestCount.incrementAndGet();
                for (ConditionalResponse conditionalResponse : priorityConditionalResponses) {
                    if (conditionalResponse.matches(request)) {
                        return conditionalResponse.response();
                    }
                }
                MockResponse response = responses.poll();
                if (response != null) {
                    return response;
                }
                for (ConditionalResponse conditionalResponse : conditionalResponses) {
                    if (conditionalResponse.matches(request)) {
                        return conditionalResponse.response();
                    }
                }
                unmatchedRequest.compareAndSet(null, request);
                return new MockResponse()
                    .setResponseCode(599)
                    .setBody("No mock response enqueued for " + request.getMethod() + " " + request.getPath());
            }
        });
    }

    /**
     * Enqueues the next strict FIFO response for a request expected by the scenario.
     */
    public void enqueueResponse(MockResponse response) {
        responses.add(response);
    }

    /**
     * Registers a fallback response used only when no queued response is available.
     */
    public void enqueueConditionalResponse(Predicate<RecordedRequest> predicate, Supplier<MockResponse> responseSupplier) {
        conditionalResponses.add(new ConditionalResponse(predicate, responseSupplier));
    }

    /**
     * Registers an auxiliary response that must not consume strict FIFO scenario stubs.
     */
    public void enqueuePriorityConditionalResponse(Predicate<RecordedRequest> predicate, Supplier<MockResponse> responseSupplier) {
        priorityConditionalResponses.add(new ConditionalResponse(predicate, responseSupplier));
    }

    /**
     * Starts the wrapped mock server.
     */
    public void start() throws IOException {
        server.start();
    }

    /**
     * Shuts down the wrapped mock server.
     */
    public void shutdown() throws IOException {
        server.shutdown();
    }

    /**
     * Builds a URL for the wrapped mock server.
     */
    public HttpUrl url(String path) {
        return server.url(path);
    }

    /**
     * Takes the next recorded request within the provided timeout.
     */
    public RecordedRequest takeRequest(long timeout, TimeUnit unit) throws InterruptedException {
        return server.takeRequest(timeout, unit);
    }

    /**
     * Returns the number of requests dispatched by the wrapped server.
     */
    public int getRequestCount() {
        return requestCount.get();
    }

    /**
     * Fails the test when any request did not match a queued or conditional response.
     */
    public void assertNoUnmatchedRequests() {
        RecordedRequest request = unmatchedRequest.get();
        if (request != null) {
            fail("No mock response enqueued for request: " + request.getMethod() + " " + request.getPath());
        }
    }

    private record ConditionalResponse(Predicate<RecordedRequest> predicate, Supplier<MockResponse> responseSupplier) {
        /**
         * Checks whether this conditional response applies to the request.
         */
        boolean matches(RecordedRequest request) {
            return predicate.test(request);
        }

        /**
         * Builds the mock response for a matching request.
         */
        MockResponse response() {
            return responseSupplier.get();
        }
    }
}
