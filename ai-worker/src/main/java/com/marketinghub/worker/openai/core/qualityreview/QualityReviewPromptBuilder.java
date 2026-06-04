package com.marketinghub.worker.openai.core.qualityreview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.StageExecution;
import com.marketinghub.worker.openai.core.port.StagePromptBuilder;
import com.marketinghub.worker.openai.core.prompt.PromptTemplateResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: montar prompt, schema e request multimodal OpenAI da revisão visual. */
public class QualityReviewPromptBuilder implements StagePromptBuilder<QualityReviewInput> {

    private static final Logger log = LoggerFactory.getLogger(QualityReviewPromptBuilder.class);

    private final ObjectMapper objectMapper;
    private final QualityReviewWorkerProperties properties;
    private final QualityReviewScreenshotService screenshotService;
    private final PromptTemplateResolver promptTemplateResolver;

    /** Inicializa o builder com serializador, propriedades OpenAI, screenshots e configurações da revisão visual. */
    public QualityReviewPromptBuilder(
            ObjectMapper objectMapper,
            QualityReviewWorkerProperties properties,
            QualityReviewScreenshotService screenshotService) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.screenshotService = screenshotService;
        this.promptTemplateResolver = new PromptTemplateResolver(this::loadResource, this::toJsonOrText);
    }

    /** Monta o request multimodal com texto e screenshots renderizados da landing para a Responses API. */
    @Override
    public OpenAiRequest build(StageExecution<QualityReviewInput> execution) {
        List<QualityReviewScreenshotEvidence> screenshots = screenshotService.renderScreenshots(execution.input());
        if (screenshots.isEmpty()) {
            throw new StageWorkerException("Quality review screenshot rendering did not produce images");
        }
        String promptResource = properties.promptResource();
        String promptMarkdownContent = loadResource(promptResource);
        List<String> screenshotUrls = screenshots.stream().map(QualityReviewScreenshotEvidence::publicUrl).toList();
        Map<String, Object> promptData = withScreenshotContext(execution.input().promptData(), screenshotUrls);
        String prompt = promptTemplateResolver.resolve(promptMarkdownContent, promptData, promptResource);
        String schemaJson = loadResource(properties.schemaResource());
        String requestBodyJson = buildResponsesApiRequest(prompt, schemaJson, screenshotUrls);
        return new OpenAiRequest(
                properties.visionModel(),
                prompt,
                requestBodyJson,
                properties.schemaName(),
                schemaJson,
                promptMarkdownContent,
                auditMetadata(execution, screenshots, prompt, requestBodyJson));
    }


    /** Monta metadados de auditoria para rastrear HTML, screenshots e versão operacional do Quality Review. */
    private Map<String, Object> auditMetadata(
            StageExecution<QualityReviewInput> execution,
            List<QualityReviewScreenshotEvidence> screenshots,
            String prompt,
            String requestBodyJson
    ) {
        Map<String, Object> audit = new LinkedHashMap<>();
        String landingHtml = execution.input().landingHtml();
        audit.put("landingHtmlSha256", sha256Hex(landingHtml));
        audit.put("landingHtmlLength", landingHtml != null ? landingHtml.length() : 0);
        audit.put("promptSha256", sha256Hex(prompt));
        audit.put("openAiRequestBodySha256", sha256Hex(requestBodyJson));
        audit.put("promptResource", properties.promptResource());
        audit.put("schemaName", properties.schemaName());
        audit.put("visionModel", properties.visionModel());
        audit.put("imageDetail", properties.imageDetail());
        audit.put("screenshots", screenshots.stream().map(this::toScreenshotAudit).toList());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("stageCode", execution.stageCode());
        metadata.put("idJob", execution.idJob());
        metadata.put("experimentId", execution.aggregateId());
        metadata.put("qualityReviewAudit", audit);
        return metadata;
    }

    /** Converte uma evidência de screenshot para mapa serializável em JSON de auditoria. */
    private Map<String, Object> toScreenshotAudit(QualityReviewScreenshotEvidence evidence) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("viewport", evidence.viewport());
        item.put("publicUrl", evidence.publicUrl());
        item.put("sha256", evidence.sha256());
        item.put("bytes", evidence.bytes());
        return item;
    }

    /** Calcula SHA-256 hexadecimal para rastrear conteúdo textual sem persistir duplicações extras. */
    private String sha256Hex(String text) {
        if (text == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new StageWorkerException("SHA-256 algorithm unavailable for quality-review audit", error);
        }
    }

    /** Acrescenta as URLs dos screenshots renderizados ao contexto textual auditável do prompt. */
    private Map<String, Object> withScreenshotContext(Map<String, Object> originalPromptData, List<String> screenshotUrls) {
        Map<String, Object> promptData = new LinkedHashMap<>(originalPromptData);
        promptData.put("renderedLandingScreenshots", screenshotUrls);
        Object caseDataBlock = promptData.get("CASE_DATA_BLOCK");
        String screenshotBlock = "\nrenderedLandingScreenshots: " + toJsonOrText(screenshotUrls).trim();
        promptData.put("CASE_DATA_BLOCK", (caseDataBlock != null ? caseDataBlock.toString() : "") + screenshotBlock);
        return promptData;
    }

    /** Serializa o corpo compatível com Responses API usando schema estrito e screenshots renderizados por URL. */
    private String buildResponsesApiRequest(String prompt, String schemaJson, List<String> screenshotUrls) {
        try {
            Object schema = objectMapper.readValue(schemaJson, Object.class);
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", properties.schemaName());
            format.put("schema", schema);
            format.put("strict", true);

            Map<String, Object> text = new LinkedHashMap<>();
            text.put("format", format);

            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "input_text", "text", prompt));
            for (String screenshotUrl : screenshotUrls) {
                content.add(Map.of("type", "input_image", "image_url", screenshotUrl, "detail", properties.imageDetail()));
            }

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "user");
            message.put("content", content);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.visionModel());
            body.put("input", List.of(message));
            body.put("text", text);
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            throw new StageWorkerException("Could not build quality review OpenAI request", error);
        }
    }

    /** Converte valores de placeholder para texto ou JSON formatado antes da substituição. */
    private String toJsonOrText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException error) {
            log.warn("Falha ao renderizar placeholder do prompt quality-review; usando toString como fallback", error);
            return value.toString();
        }
    }

    /** Lê recursos do classpath usados como prompt markdown ou schema da revisão visual. */
    private String loadResource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new StageWorkerException("Could not load resource from classpath: " + path, error);
        }
    }
}
