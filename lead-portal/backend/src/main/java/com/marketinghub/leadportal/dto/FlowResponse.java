package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import java.util.List;

public record FlowResponse(
        String slug,
        String name,
        String description,
        List<QuestionResponse> questions,
        SimpleFormStyleResponse simpleFormStyle) {

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
                questions,
                style);
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
