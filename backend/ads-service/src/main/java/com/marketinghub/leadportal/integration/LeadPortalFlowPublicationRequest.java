package com.marketinghub.leadportal.integration;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;

import java.util.List;
import java.util.Objects;

/**
 * Payload sent to the public lead portal when a flow becomes available.
 */
public record LeadPortalFlowPublicationRequest(
        String slug,
        String name,
        String description,
        String model,
        String prompt,
        List<Question> questions,
        SimpleFormStylePayload simpleFormStyle) {

    public static LeadPortalFlowPublicationRequest from(LeadPortalFlow flow) {
        LeadPortalSimpleFormStyle style = flow.getSimpleFormStyle();
        SimpleFormStylePayload stylePayload = style == null ? null : new SimpleFormStylePayload(
                style.getSlug(),
                style.getName(),
                style.getDefinition(),
                style.getPreviewImageUrl());
        return new LeadPortalFlowPublicationRequest(
                flow.getSlug(),
                flow.getName(),
                flow.getDescription(),
                flow.getModel(),
                flow.getPrompt(),
                flow.getQuestions().stream()
                        .map(LeadPortalFlowPublicationRequest::toQuestion)
                        .toList(),
                stylePayload);
    }

    private static Question toQuestion(LeadPortalFlowQuestion question) {
        return new Question(
                question.getTitle(),
                question.getDataKey(),
                question.getType(),
                question.isRequired(),
                question.getDescription(),
                question.getPlaceholder(),
                List.copyOf(Objects.requireNonNullElse(question.getOptions(), List.of())));
    }

    public record Question(
            String title,
            String dataKey,
            LeadPortalQuestionType type,
            boolean required,
            String description,
            String placeholder,
            List<String> options) {
    }

    public record SimpleFormStylePayload(
            String slug,
            String name,
            LeadPortalSimpleFormStyleDefinition definition,
            String previewImageUrl) {
    }
}
