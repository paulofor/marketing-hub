package com.marketinghub.worker.openai.core.qualityreview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;
import java.util.List;
import java.util.Map;

/** Responsabilidade: validar e converter a resposta JSON da OpenAI para o contrato da revisão visual. */
public class QualityReviewResponseValidator implements StageResponseValidator<QualityReviewOutput> {

    private final ObjectMapper objectMapper;

    /** Inicializa o validador com ObjectMapper para parse seguro do JSON da revisão visual. */
    public QualityReviewResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida os campos obrigatórios do diagnóstico visual antes de confirmar a etapa no backend. */
    @Override
    public QualityReviewOutput validateAndParse(String modelResponse) {
        if (modelResponse == null || modelResponse.isBlank()) {
            throw new InvalidModelResponseException("Quality review model response is blank");
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(modelResponse, new TypeReference<Map<String, Object>>() {});
            require(payload, "score", Number.class);
            require(payload, "targetAudienceSpecificity", String.class);
            require(payload, "blockingIssues", List.class);
            require(payload, "recommendedRegeneration", List.class);
            require(payload, "approvalRecommendation", String.class);
            return new QualityReviewOutput(payload);
        } catch (InvalidModelResponseException error) {
            throw error;
        } catch (Exception error) {
            throw new InvalidModelResponseException("Quality review model response is not valid JSON", error);
        }
    }

    /** Garante que um campo obrigatório exista e tenha o tipo esperado. */
    private void require(Map<String, Object> payload, String fieldName, Class<?> expectedType) {
        Object value = payload.get(fieldName);
        if (!expectedType.isInstance(value)) {
            throw new InvalidModelResponseException("Quality review response missing field: " + fieldName);
        }
    }
}
