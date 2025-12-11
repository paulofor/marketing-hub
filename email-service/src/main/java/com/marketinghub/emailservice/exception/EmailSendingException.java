package com.marketinghub.emailservice.exception;

public class EmailSendingException extends EmailServiceException {
    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
