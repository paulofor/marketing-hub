package com.marketinghub.leadportal.exception;

public class FlowNotFoundException extends RuntimeException {

    public FlowNotFoundException(String flowSlug) {
        super("Flow not found: " + flowSlug);
    }
}

    public FlowNotFoundException(String slug) {
        super("Fluxo não encontrado: " + slug);
    }
}
