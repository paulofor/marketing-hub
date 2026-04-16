package com.marketinghub.videomanagement.exception;

/**
 * Exceção lançada quando a integração com o backend falha.
 */
public class BackendIntegrationException extends RuntimeException {
    private final Integer statusCode;

    public BackendIntegrationException(String message) {
        this(message, null, null);
    }

    public BackendIntegrationException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public BackendIntegrationException(String message, Integer statusCode) {
        this(message, statusCode, null);
    }

    public BackendIntegrationException(String message,
                                       Integer statusCode,
                                       Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
