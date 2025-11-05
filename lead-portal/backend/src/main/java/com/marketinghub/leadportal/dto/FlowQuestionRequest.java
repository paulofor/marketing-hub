package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.model.FlowQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class FlowQuestionRequest {

    @NotBlank(message = "Título é obrigatório")
    private String title;

    @NotBlank(message = "Chave de dados é obrigatória")
    private String dataKey;

    @NotNull(message = "Tipo é obrigatório")
    private FlowQuestionType type;

    private boolean required;

    private String description;

    private String placeholder;

    private List<String> options = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }

    public FlowQuestionType getType() {
        return type;
    }

    public void setType(FlowQuestionType type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
