package com.marketinghub.videomanagement.exception;

/**
 * Exceção lançada quando a integração com o backend falha.
 */
public class BackendIntegrationException extends RuntimeException {
    public BackendIntegrationException(String message) {
        super(message);
    }

    public BackendIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
