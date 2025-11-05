package com.marketinghub.leadportal.dto;

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
}
