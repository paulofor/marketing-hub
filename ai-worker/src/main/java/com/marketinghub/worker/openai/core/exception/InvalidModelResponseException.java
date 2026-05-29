package com.marketinghub.worker.openai.core.exception;

public class InvalidModelResponseException extends StageWorkerException {

    public InvalidModelResponseException(String message) {
        super(message);
    }

    public InvalidModelResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
