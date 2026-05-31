package com.marketinghub.worker.openai.core.imagegeneration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.frameworkimage.FrameworkImageJobDto;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StagePromptBuilder;
import java.util.LinkedHashMap;
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

    /** Monta o request bruto para o endpoint de geração de imagens da OpenAI. */
    @Override
    public OpenAiRequest build(StageExecution<ImageGenerationInput> execution) {
        FrameworkImageJobDto job = execution.input().job();
        String selectedModel = resolveModel(job.model());
        String prompt = job.prompt();
        String requestBodyJson = buildImagesApiRequest(selectedModel, prompt);

        return new OpenAiRequest(
                selectedModel,
                prompt,
                requestBodyJson,
                "framework_image_generation",
                "{}",
                prompt,
                Map.of(
                        "stageCode", execution.stageCode(),
                        "idJob", execution.idJob(),
                        "experimentId", execution.aggregateId(),
                        "assetId", job.assetId() == null ? "" : job.assetId()
                )
        );
    }

    /** Serializa o corpo compatível com a Images API preservando o prompt funcional da etapa. */
    private String buildImagesApiRequest(String selectedModel, String prompt) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", selectedModel);
            payload.put("prompt", prompt);
            if (supportsResponseFormat(selectedModel)) {
                payload.put("response_format", "b64_json");
            }
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException error) {
            throw new StageWorkerException("Could not build OpenAI Images API request", error);
        }
    }

    /** Resolve o modelo efetivo de imagem usando o padrão configurado quando o job não informa modelo atual. */
    private String resolveModel(String requestedModel) {
        if (!StringUtils.hasText(requestedModel)
                || "gpt-image-1".equalsIgnoreCase(requestedModel.trim())
                || "gpt-image-1.0".equalsIgnoreCase(requestedModel.trim())) {
            return properties.imageModel().trim();
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
}
