package com.marketinghub.worker.openai.core.imagegeneration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StagePromptBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/** Responsabilidade: montar o request OpenAI da etapa imagegeneration no formato do core OpenAI. */
public class ImageGenerationPromptBuilder implements StagePromptBuilder<ImageGenerationInput> {

    private final ObjectMapper objectMapper;
    private final ImageGenerationWorkerProperties properties;

    /** Inicializa o builder com serializador e propriedades específicas da etapa imagegeneration. */
    public ImageGenerationPromptBuilder(ObjectMapper objectMapper, ImageGenerationWorkerProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** Monta o request bruto com todos os prompts de imagens planejadas para o endpoint novo do GeraLanding. */
    @Override
    public OpenAiRequest build(StageExecution<ImageGenerationInput> execution) {
        ImageGenerationInput input = execution.input();
        String selectedModel = resolveModel(properties.imageModel());
        List<Map<String, Object>> imagePayload = input.images().stream()
                .filter(item -> StringUtils.hasText(item.prompt()))
                .map(this::toImageRequestItem)
                .toList();
        if (imagePayload.isEmpty()) {
            throw new StageWorkerException("GeraLanding image generation has no planned image prompts");
        }

        String prompt = buildPromptSummary(imagePayload);
        String requestBodyJson = buildImagesApiRequest(selectedModel, imagePayload);

        return new OpenAiRequest(
                selectedModel,
                prompt,
                requestBodyJson,
                "geralanding_image_generation",
                "{}",
                prompt,
                Map.of(
                        "stageCode", execution.stageCode(),
                        "idJob", execution.idJob(),
                        "experimentId", execution.aggregateId(),
                        "imageCount", imagePayload.size()
                )
        );
    }

    /** Converte um item planejado no formato auditável consumido pelo client de imagens. */
    private Map<String, Object> toImageRequestItem(ImageGenerationInput.ImageGenerationPromptItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("planningItemKey", item.effectiveKey());
        putIfPresent(payload, "sectionId", item.sectionId());
        putIfPresent(payload, "elementId", item.elementId());
        putIfPresent(payload, "imageGoal", item.imageGoal());
        payload.put("prompt", item.prompt());
        return payload;
    }

    /** Serializa o corpo auditável contendo uma requisição lógica por imagem planejada. */
    private String buildImagesApiRequest(String selectedModel, List<Map<String, Object>> images) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", selectedModel);
            payload.put("images", images);
            payload.put("responseFormat", supportsResponseFormat(selectedModel) ? "b64_json" : "default");
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException error) {
            throw new StageWorkerException("Could not build OpenAI Images API request", error);
        }
    }

    /** Monta um resumo textual dos prompts para auditoria no detalhe da execução GeraLanding. */
    private String buildPromptSummary(List<Map<String, Object>> images) {
        StringBuilder builder = new StringBuilder("Geração de imagens GeraLanding:\n");
        for (Map<String, Object> image : images) {
            builder.append("\n- ")
                    .append(image.get("planningItemKey"))
                    .append(": ")
                    .append(image.get("prompt"));
        }
        return builder.toString();
    }

    /** Resolve o modelo efetivo de imagem usando o padrão configurado quando o valor está ausente. */
    private String resolveModel(String requestedModel) {
        if (!StringUtils.hasText(requestedModel)
                || "gpt-image-1".equalsIgnoreCase(requestedModel.trim())
                || "gpt-image-1.0".equalsIgnoreCase(requestedModel.trim())) {
            return "gpt-image-2";
        }
        return requestedModel.trim();
    }

    /** Indica se o modelo aceita o parâmetro explícito response_format no payload de geração. */
    private boolean supportsResponseFormat(String selectedModel) {
        if (!StringUtils.hasText(selectedModel)) {
            return true;
        }
        return !selectedModel.toLowerCase(java.util.Locale.ROOT).startsWith("gpt-image-");
    }

    /** Adiciona campos opcionais ao payload somente quando há conteúdo textual útil. */
    private void putIfPresent(Map<String, Object> payload, String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            payload.put(fieldName, value.trim());
        }
    }
}
