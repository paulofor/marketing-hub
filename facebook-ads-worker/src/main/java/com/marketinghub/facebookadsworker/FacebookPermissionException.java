package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Exception raised when the Facebook API denies the requested operation due to missing permissions.
 */
public class FacebookPermissionException extends RuntimeException {
    private final ObjectNode errorDetails;

    public FacebookPermissionException(String message, ObjectNode errorDetails, Throwable cause) {
        super(message, cause);
        this.errorDetails = errorDetails == null ? null : errorDetails.deepCopy();
    }

    public ObjectNode getErrorDetails() {
        return errorDetails;
    }
}

