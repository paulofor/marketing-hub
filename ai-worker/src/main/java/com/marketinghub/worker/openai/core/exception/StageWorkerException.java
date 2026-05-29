package com.marketinghub.worker.openai.core.exception;

public class StageWorkerException extends RuntimeException {

    public StageWorkerException(String message) {
        super(message);
    }

    public StageWorkerException(String message, Throwable cause) {
        super(message, cause);
    }
}
