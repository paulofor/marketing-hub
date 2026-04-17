package com.marketinghub.leadportal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

public class UpsertFlowRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    private String description;

    private String customFormHtml;

    private String facebookPixelId;

    private String facebookPixelCode;

    private Instant facebookPixelCreatedAt;

    private String model;

    private String prompt;

    private String imagePromptModel;

    private String imagePromptTemplate;

    @JsonProperty("imagePromptBatchSize")
    private Integer imageBatchSize;

    private SimpleFormStylePayload simpleFormStyle;

    @Valid
    private List<FlowQuestionRequest> questions = new ArrayList<>();

    @AssertTrue(message = "Ao menos uma pergunta é obrigatória quando o template personalizado está vazio")
    public boolean isCustomFormOrQuestionsPresent() {
        if (StringUtils.hasText(customFormHtml)) {
            return true;
        }
        return questions != null && !questions.isEmpty();
    }

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

    public String getCustomFormHtml() {
        return customFormHtml;
    }

    public void setCustomFormHtml(String customFormHtml) {
        this.customFormHtml = customFormHtml;
    }

    public String getFacebookPixelId() {
        return facebookPixelId;
    }

    public void setFacebookPixelId(String facebookPixelId) {
        this.facebookPixelId = facebookPixelId;
    }

    public String getFacebookPixelCode() {
        return facebookPixelCode;
    }

    public void setFacebookPixelCode(String facebookPixelCode) {
        this.facebookPixelCode = facebookPixelCode;
    }

    public Instant getFacebookPixelCreatedAt() {
        return facebookPixelCreatedAt;
    }

    public void setFacebookPixelCreatedAt(Instant facebookPixelCreatedAt) {
        this.facebookPixelCreatedAt = facebookPixelCreatedAt;
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

    public String getImagePromptModel() {
        return imagePromptModel;
    }

    public void setImagePromptModel(String imagePromptModel) {
        this.imagePromptModel = imagePromptModel;
    }

    public String getImagePromptTemplate() {
        return imagePromptTemplate;
    }

    public void setImagePromptTemplate(String imagePromptTemplate) {
        this.imagePromptTemplate = imagePromptTemplate;
    }

    public Integer getImageBatchSize() {
        return imageBatchSize;
    }

    public void setImageBatchSize(Integer imageBatchSize) {
        this.imageBatchSize = imageBatchSize;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
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
