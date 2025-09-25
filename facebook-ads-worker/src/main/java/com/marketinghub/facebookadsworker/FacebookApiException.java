package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * Exception that enriches Facebook API errors with structured metadata exposed in the error payload.
 */
public class FacebookApiException extends RuntimeException {
    private final int status;
    private final String path;
    private final ErrorDetails errorDetails;

    public FacebookApiException(String message,
                                int status,
                                String path,
                                ErrorDetails errorDetails,
                                Throwable cause) {
        super(message, cause);
        this.status = status;
        this.path = path;
        this.errorDetails = errorDetails;
    }

    public int getStatus() {
        return status;
    }

    public String getPath() {
        return path;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }

    public boolean isPermissionsError() {
        return errorDetails != null
            && Objects.equals(errorDetails.code(), 200)
            && Objects.equals(errorDetails.errorSubcode(), 1815066);
    }

    public record ErrorDetails(
        String type,
        Integer code,
        Integer errorSubcode,
        String message,
        String errorUserTitle,
        String errorUserMsg,
        String fbtraceId
    ) {
        public static ErrorDetails from(ObjectNode node) {
            if (node == null) {
                return null;
            }
            return new ErrorDetails(
                textValue(node, "type"),
                intValue(node, "code"),
                intValue(node, "error_subcode"),
                textValue(node, "message"),
                textValue(node, "error_user_title"),
                textValue(node, "error_user_msg"),
                textValue(node, "fbtrace_id")
            );
        }

        private static String textValue(ObjectNode node, String field) {
            return node.hasNonNull(field) ? node.get(field).asText() : null;
        }

        private static Integer intValue(ObjectNode node, String field) {
            return node.hasNonNull(field) ? node.get(field).asInt() : null;
        }

        public String summary() {
            if (errorUserMsg != null && !errorUserMsg.isBlank()) {
                if (errorUserTitle != null && !errorUserTitle.isBlank()) {
                    return errorUserTitle + ": " + errorUserMsg;
                }
                return errorUserMsg;
            }
            if (message != null && !message.isBlank()) {
                return message;
            }
            return null;
        }
    }
}
