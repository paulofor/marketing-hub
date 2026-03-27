package com.marketinghub.worker.learning.exception;

/**
 * Exceção simples para sinalizar erros nas chamadas ao backend do Marketing Hub.
 */
public class BackendClientException extends RuntimeException {
    public BackendClientException(String message) {
        super(message);
    }
}
