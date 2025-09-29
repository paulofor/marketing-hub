package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Signals that the Facebook Graph API rejected the request because the access token has expired.
 */
public class FacebookAccessTokenExpiredException extends RuntimeException {
    private final ObjectNode errorDetails;

    public FacebookAccessTokenExpiredException(String message, ObjectNode errorDetails, Throwable cause) {
        super(message, cause);
        this.errorDetails = errorDetails;
    }

    public ObjectNode getErrorDetails() {
        return errorDetails;
    }
}
