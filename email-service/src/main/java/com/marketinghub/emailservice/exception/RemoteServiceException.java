package com.marketinghub.emailservice.exception;

public class RemoteServiceException extends EmailServiceException {

    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
