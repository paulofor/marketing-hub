package com.marketinghub.marketresearch.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiResponse(
        String id,
        @JsonProperty("output_text") String outputText,
        List<OpenAiOutput> output,
        OpenAiUsage usage,
        OpenAiError error,
        String status) {

    public String firstText() {
        if (outputText != null && !outputText.isBlank()) {
            return outputText;
        }
        if (output != null) {
            for (OpenAiOutput item : output) {
                String text = item.firstText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    public boolean hasError() {
        return error != null && error.message() != null && !error.message().isBlank();
    }

    public String errorMessage() {
        if (!hasError()) {
            return null;
        }
        if (error.code() != null && !error.code().isBlank()) {
            return error.code() + ": " + error.message();
        }
        if (error.type() != null && !error.type().isBlank()) {
            return error.type() + ": " + error.message();
        }
        return error.message();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAiError(String message, String type, String code) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAiOutput(
            String id,
            String type,
            String role,
            List<OpenAiContent> content) {

        public String firstText() {
            if (content == null) {
                return null;
            }
            for (OpenAiContent item : content) {
                if (item.text() != null && !item.text().isBlank()) {
                    return item.text();
                }
            }
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAiContent(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAiUsage(
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("output_tokens") Integer outputTokens,
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {

        public Integer effectiveInputTokens() {
            if (inputTokens != null) {
                return inputTokens;
            }
            return promptTokens;
        }

        public Integer effectiveOutputTokens() {
            if (outputTokens != null) {
                return outputTokens;
            }
            return completionTokens;
        }
    }
}
