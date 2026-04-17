package com.marketinghub.mds.search;

public class RecoverableSourceException extends RuntimeException {
    public RecoverableSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecoverableSourceException(String message) {
        super(message);
    }
}
