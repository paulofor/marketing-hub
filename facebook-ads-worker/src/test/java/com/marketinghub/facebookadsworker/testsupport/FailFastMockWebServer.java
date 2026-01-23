package com.marketinghub.facebookadsworker.testsupport;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.fail;

public final class FailFastMockWebServer extends MockWebServer {
    private final Queue<MockResponse> responses = new ArrayDeque<>();
    private final AtomicReference<RecordedRequest> unmatchedRequest = new AtomicReference<>();

    public FailFastMockWebServer() {
        setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
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

    public void assertNoUnmatchedRequests() {
        RecordedRequest request = unmatchedRequest.get();
        if (request != null) {
            fail("No mock response enqueued for request: " + request.getMethod() + " " + request.getPath());
        }
    }
}
