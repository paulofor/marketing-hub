package com.marketinghub.leadportal.exception;

import java.util.UUID;

public class FlowSubmissionNotFoundException extends RuntimeException {

    public FlowSubmissionNotFoundException(UUID id) {
        super("Submissão não encontrada: " + id);
    }
}
