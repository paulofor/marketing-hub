package com.marketinghub.worker.prompt;

import java.util.List;

/**
 * Exception thrown when a prompt template cannot be rendered.
 */
public class PromptTemplateException extends IllegalStateException {
    private final List<String> missingVariables;
    private final List<String> availableVariables;
    private final String templatePreview;

    public PromptTemplateException(String message,
                                   Throwable cause,
                                   List<String> missingVariables,
                                   List<String> availableVariables,
                                   String templatePreview) {
        super(message, cause);
        this.missingVariables = missingVariables != null ? List.copyOf(missingVariables) : List.of();
        this.availableVariables = availableVariables != null ? List.copyOf(availableVariables) : List.of();
        this.templatePreview = templatePreview;
    }

    public List<String> getMissingVariables() {
        return missingVariables;
    }

    public List<String> getAvailableVariables() {
        return availableVariables;
    }

    public String getTemplatePreview() {
        return templatePreview;
    }
}
