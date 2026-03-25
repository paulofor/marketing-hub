package com.marketinghub.videomanagement.service.provider;

/**
 * Exceção lançada pelos providers em caso de falha de renderização.
 */
public class VideoProviderException extends RuntimeException {
    public VideoProviderException(String message) {
        super(message);
    }

    public VideoProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
