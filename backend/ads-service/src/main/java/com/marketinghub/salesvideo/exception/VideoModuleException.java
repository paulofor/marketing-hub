package com.marketinghub.salesvideo.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção padronizada para o domínio de vídeos.
 */
public class VideoModuleException extends RuntimeException {
    private final HttpStatus status;
    private final VideoModuleErrorCode errorCode;

    public VideoModuleException(HttpStatus status, VideoModuleErrorCode errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public VideoModuleException(HttpStatus status, VideoModuleErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public VideoModuleErrorCode getErrorCode() {
        return errorCode;
    }

    public static VideoModuleException badRequest(VideoModuleErrorCode code, String message) {
        return new VideoModuleException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static VideoModuleException notFound(VideoModuleErrorCode code, String message) {
        return new VideoModuleException(HttpStatus.NOT_FOUND, code, message);
    }

    public static VideoModuleException forbidden(VideoModuleErrorCode code, String message) {
        return new VideoModuleException(HttpStatus.FORBIDDEN, code, message);
    }

    public static VideoModuleException conflict(VideoModuleErrorCode code, String message) {
        return new VideoModuleException(HttpStatus.CONFLICT, code, message);
    }

    public static VideoModuleException internal(VideoModuleErrorCode code, String message) {
        return new VideoModuleException(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }
}
