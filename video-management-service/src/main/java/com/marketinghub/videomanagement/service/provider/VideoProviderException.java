package com.marketinghub.videomanagement.service.provider;

/**
 * Exceção lançada pelos providers em caso de falha de renderização.
 */
public class VideoProviderException extends RuntimeException {
    private final String code;

    public VideoProviderException(String message) {
        super(message);
        this.code = "VIDEO_PROVIDER_ERROR";
    }

    public VideoProviderException(String code,
                                  String message) {
        super(message);
        this.code = code;
    }

    public VideoProviderException(String message, Throwable cause) {
        super(message, cause);
        this.code = "VIDEO_PROVIDER_ERROR";
    }

    public VideoProviderException(String code,
                                  String message,
                                  Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
