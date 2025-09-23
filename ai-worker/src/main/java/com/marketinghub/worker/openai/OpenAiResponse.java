package com.marketinghub.worker.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiResponse(
        String id,
        @JsonProperty("output_text") String outputText,
        List<OpenAiOutput> output,
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

    public List<OpenAiToolCall> firstToolCalls() {
        if (output == null) {
            return List.of();
        }
        for (OpenAiOutput item : output) {
            List<OpenAiToolCall> calls = item.combinedToolCalls();
            if (!calls.isEmpty()) {
                return calls;
            }
        }
        return List.of();
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
            List<OpenAiContent> content,
            @JsonProperty("tool_call") OpenAiToolCall toolCall,
            @JsonProperty("tool_calls") List<OpenAiToolCall> toolCalls) {

        public List<OpenAiToolCall> combinedToolCalls() {
            if (toolCalls != null && !toolCalls.isEmpty()) {
                return toolCalls;
            }
            if (toolCall != null) {
                return List.of(toolCall);
            }
            return Collections.emptyList();
        }

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
    public record OpenAiToolCall(
            String id,
            @JsonProperty("call_id") String callId,
            String type,
            OpenAiFunctionCall function) {

        public String effectiveCallId() {
            if (callId != null && !callId.isBlank()) {
                return callId;
            }
            return id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAiFunctionCall(String name, String arguments) {
    }
}
