package com.marketinghub.facebookadsworker.testsupport;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.fail;

public final class FailFastMockWebServer {
    private final MockWebServer server = new MockWebServer();
    private final Queue<MockResponse> responses = new ArrayDeque<>();
    private final AtomicReference<RecordedRequest> unmatchedRequest = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    public FailFastMockWebServer() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                requestCount.incrementAndGet();
                MockResponse response = responses.poll();
                if (response == null) {
                    unmatchedRequest.compareAndSet(null, request);
                    return new MockResponse()
                        .setResponseCode(599)
                        .setBody("No mock response enqueued for " + request.getMethod() + " " + request.getPath());
                }
                return response;
            }
        });
    }

    public void enqueueResponse(MockResponse response) {
        responses.add(response);
    }

    public void start() throws IOException {
        server.start();
    }

    public void shutdown() throws IOException {
        server.shutdown();
    }

    public HttpUrl url(String path) {
        return server.url(path);
    }

    public RecordedRequest takeRequest(long timeout, TimeUnit unit) throws InterruptedException {
        return server.takeRequest(timeout, unit);
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public void assertNoUnmatchedRequests() {
        RecordedRequest request = unmatchedRequest.get();
        if (request != null) {
            fail("No mock response enqueued for request: " + request.getMethod() + " " + request.getPath());
        }
    }
}
