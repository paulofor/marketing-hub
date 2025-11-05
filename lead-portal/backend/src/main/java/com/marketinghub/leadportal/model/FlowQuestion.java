package com.marketinghub.leadportal.model;

import java.util.List;

/**
 * Individual question inside a flow.
 */
public record FlowQuestion(
        String title,
        String dataKey,
        FlowQuestionType type,
        boolean required,
        String description,
        String placeholder,
        List<String> options) {
}
