package com.marketinghub.leadportal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marketinghub.leadportal.model.CustomFormRenderMode;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

public record FlowResponse(
        String slug,
        String name,
        String description,
        String customFormHtml,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        CustomFormRenderMode customFormRenderMode,
        List<QuestionResponse> questions,
        SimpleFormStyleResponse simpleFormStyle,
        String facebookPixelId,
        String facebookPixelCode,
        Instant facebookPixelCreatedAt) {

    public static FlowResponse from(Flow flow) {
        List<QuestionResponse> questions = flow.questions() == null
                ? List.of()
                : flow.questions().stream().map(QuestionResponse::from).toList();

        SimpleFormStyleResponse style = flow.simpleFormStyle() == null ? null :
                new SimpleFormStyleResponse(
                        flow.simpleFormStyle().slug(),
                        flow.simpleFormStyle().name(),
                        flow.simpleFormStyle().definition());
        return new FlowResponse(
                flow.slug(),
                flow.name(),
                flow.description(),
                flow.customFormHtml(),
                determineCustomFormRenderMode(flow),
                questions,
                style,
                flow.facebookPixelId(),
                flow.facebookPixelCode(),
                flow.facebookPixelCreatedAt());
    }

    private static CustomFormRenderMode determineCustomFormRenderMode(Flow flow) {
        if (flow == null || !StringUtils.hasText(flow.customFormHtml())) {
            return null;
        }
        String normalized = flow.customFormHtml().trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("<!doctype")
                || normalized.startsWith("<html")
                || normalized.contains("<body")) {
            return CustomFormRenderMode.STANDALONE_PAGE;
        }
        return CustomFormRenderMode.IFRAME;
    }

    public record QuestionResponse(
            String title,
            String dataKey,
            FlowQuestionType type,
            boolean required,
            String description,
            String placeholder,
            List<String> options) {

        public static QuestionResponse from(FlowQuestion question) {
            return new QuestionResponse(
                    question.title(),
                    question.dataKey(),
                    question.type(),
                    question.required(),
                    question.description(),
                    question.placeholder(),
                    question.options());
        }
    }

    public record SimpleFormStyleResponse(
            String slug,
            String name,
            SimpleFormStyleDefinition definition) {
    }
}
