package com.marketinghub.leadportal.integration;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;

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
        List<Question> questions) {

    public static LeadPortalFlowPublicationRequest from(LeadPortalFlow flow) {
        return new LeadPortalFlowPublicationRequest(
                flow.getSlug(),
                flow.getName(),
                flow.getDescription(),
                flow.getModel(),
                flow.getPrompt(),
                flow.getQuestions().stream()
                        .map(LeadPortalFlowPublicationRequest::toQuestion)
                        .toList());
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
}
