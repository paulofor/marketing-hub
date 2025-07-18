package com.marketinghub.facebookads.service;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Interceptor applying exponential back-off for 429 and 5xx responses.
 */
public class RetryInterceptor implements Interceptor {
    private final int maxRetries;
    private final long baseDelayMs;

    public RetryInterceptor() {
        this(3, 1000);
    }

    public RetryInterceptor(int maxRetries, long baseDelayMs) {
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        int attempt = 0;
        IOException last = null;
        while (attempt <= maxRetries) {
            try {
                Response response = chain.proceed(chain.request());
                if (!shouldRetry(response)) {
                    return response;
                }
                response.close();
            } catch (IOException ex) {
                last = ex;
            }
            try {
                Thread.sleep((long) Math.pow(2, attempt) * baseDelayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Retry interrupted", ie);
            }
            attempt++;
        }
        if (last != null) {
            throw last;
        }
        throw new IOException("Failed after retries");
    }

    private boolean shouldRetry(Response response) {
        int code = response.code();
        return code == 429 || (code >= 500 && code < 600);
    }
}
