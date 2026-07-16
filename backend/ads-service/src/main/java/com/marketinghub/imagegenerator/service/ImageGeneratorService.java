package com.marketinghub.imagegenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.imagegenerator.dto.ImageGeneratorRequest;
import com.marketinghub.imagegenerator.dto.ImageGeneratorResponse;
import com.marketinghub.openai.OpenAiProperties;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: gerar imagens por IA em modo flex e auditar request/response da OpenAI. */
@Service
public class ImageGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(ImageGeneratorService.class);
    private static final String SERVICE_TIER = "flex";
    private static final String OUTPUT_FORMAT = "png";
    private static final String PROMPT_TEMPLATE_PATH = "prompts/image-generator/user-image-generation.md";

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;
    private final ImageGenerationRequestRepository repository;
    private final ObjectMapper objectMapper;
    private final String model;

    /** Inicializa o serviço com cliente OpenAI autenticado e repositório de auditoria. */
    public ImageGeneratorService(
            @Qualifier("openAiWebClient") WebClient openAiWebClient,
            OpenAiProperties openAiProperties,
            ImageGenerationRequestRepository repository,
            ObjectMapper objectMapper,
            @Value("${image-generator.openai.model:gpt-5.6}") String model) {
        this.openAiWebClient = openAiWebClient;
        this.openAiProperties = openAiProperties;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    /** Gera uma imagem a partir do prompt do usuário usando Responses API com ferramenta de imagem. */
    @Transactional
    public ImageGeneratorResponse generate(ImageGeneratorRequest request) {
        if (!openAiProperties.isEnabled()) {
            throw new IllegalStateException("OpenAI não está configurada no backend.");
        }

        String jobId = "img-" + UUID.randomUUID();
        Instant startedAt = Instant.now();
        String finalPrompt = buildPrompt(request.prompt());
        Map<String, Object> requestBody = buildRequestBody(finalPrompt);
        ImageGenerationRequest audit = repository.save(ImageGenerationRequest.builder()
                .jobId(jobId)
                .status("RUNNING")
                .model(model)
                .serviceTier(SERVICE_TIER)
                .outputFormat(OUTPUT_FORMAT)
                .prompt(request.prompt())
                .openAiRequestBody(writeJson(requestBody))
                .startedAt(startedAt)
                .build());

        try {
            JsonNode responseBody = openAiWebClient.post()
                    .uri("/responses")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String rawResponse = writeJson(responseBody);
            String imageBase64 = extractImageBase64(responseBody);
            audit.setStatus("COMPLETED");
            audit.setOpenAiResponseBody(rawResponse);
            audit.setOpenAiResponseId(responseBody != null && responseBody.hasNonNull("id")
                    ? responseBody.get("id").asText()
                    : null);
            audit.setFinishedAt(Instant.now());
            repository.save(audit);

            return new ImageGeneratorResponse(
                    jobId,
                    model,
                    SERVICE_TIER,
                    OUTPUT_FORMAT,
                    imageBase64,
                    audit.getFinishedAt());
        } catch (RuntimeException ex) {
            audit.setStatus("FAILED");
            audit.setErrorMessage(ex.getMessage());
            audit.setFinishedAt(Instant.now());
            repository.save(audit);
            log.error("Falha ao gerar imagem por IA. modulo=image-generator operacao=generate jobId={}", jobId, ex);
            throw ex;
        }
    }

    /** Monta o prompt operacional versionado com o texto informado pelo usuário. */
    private String buildPrompt(String userPrompt) {
        try {
            ClassPathResource resource = new ClassPathResource(PROMPT_TEMPLATE_PATH);
            String template = resource.getContentAsString(StandardCharsets.UTF_8);
            return template.replace("{{USER_PROMPT}}", userPrompt.trim());
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível carregar o prompt operacional de geração de imagem.", ex);
        }
    }

    /** Monta o corpo da Responses API com service_tier flex e ferramenta image_generation. */
    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "model", model,
                "input", prompt,
                "service_tier", SERVICE_TIER,
                "tools", List.of(Map.of(
                        "type", "image_generation",
                        "output_format", OUTPUT_FORMAT)));
    }

    /** Extrai a primeira imagem base64 retornada pela chamada image_generation_call. */
    String extractImageBase64(JsonNode responseBody) {
        JsonNode output = responseBody == null ? null : responseBody.get("output");
        if (output == null || !output.isArray()) {
            throw new IllegalStateException("OpenAI não retornou saída de imagem.");
        }
        for (JsonNode item : output) {
            if ("image_generation_call".equals(item.path("type").asText()) && StringUtils.hasText(item.path("result").asText())) {
                return item.path("result").asText();
            }
        }
        throw new IllegalStateException("OpenAI não retornou imagem base64.");
    }

    /** Serializa objetos de auditoria em JSON para persistência e diagnóstico. */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível serializar payload de auditoria.", ex);
        }
    }
}
