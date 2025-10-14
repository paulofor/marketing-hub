package com.marketinghub.worker.openai;

import com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AiGenerationRecorder {
    private static final Logger log = LoggerFactory.getLogger(AiGenerationRecorder.class);

    private final AiWorkerGenerationService service;

    public AiGenerationRecorder(AiWorkerGenerationService service) {
        this.service = service;
    }

    public void record(String domain,
                       String referenceId,
                       String prompt,
                       String rawResponse,
                       String model,
                       OpenAiResponse.OpenAiUsage usage) {
        BigDecimal cost = OpenAiCostEstimator.estimateUsd(model, usage);
        Integer inputTokens = usage != null ? usage.effectiveInputTokens() : null;
        Integer outputTokens = usage != null ? usage.effectiveOutputTokens() : null;
        try {
            service.recordGeneration(AiWorkerGenerationRequest.builder()
                    .domain(domain)
                    .referenceId(referenceId)
                    .prompt(prompt)
                    .rawResponse(rawResponse)
                    .model(model)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .costUsd(cost)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to persist AI generation for domain {} and reference {}", domain, referenceId, ex);
            throw ex;
        }
    }
}
