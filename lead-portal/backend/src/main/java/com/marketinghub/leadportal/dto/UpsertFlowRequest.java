package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class UpsertFlowRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    private String description;

    private String model;

    private String prompt;

    private SimpleFormStylePayload simpleFormStyle;

    @NotEmpty(message = "Ao menos uma pergunta é obrigatória")
    @Valid
    private List<FlowQuestionRequest> questions = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<FlowQuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<FlowQuestionRequest> questions) {
        this.questions = questions;
    }

    public SimpleFormStylePayload getSimpleFormStyle() {
        return simpleFormStyle;
    }

    public void setSimpleFormStyle(SimpleFormStylePayload simpleFormStyle) {
        this.simpleFormStyle = simpleFormStyle;
    }

    public static class SimpleFormStylePayload {
        private String slug;
        private String name;
        @Valid
        private SimpleFormStyleDefinition definition;

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SimpleFormStyleDefinition getDefinition() {
            return definition;
        }

        public void setDefinition(SimpleFormStyleDefinition definition) {
            this.definition = definition;
        }
    }
}
