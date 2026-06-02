package com.marketinghub.worker.openai.core.imagegeneration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/** Responsabilidade: validar e converter a resposta da OpenAI Images API na etapa imagegeneration. */
public class ImageGenerationResponseValidator implements StageResponseValidator<ImageGenerationOutput> {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationResponseValidator.class);

    private final ObjectMapper objectMapper;

    /** Inicializa o validador com ObjectMapper para leitura da resposta crua. */
    public ImageGenerationResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Valida que a resposta consolidada contém imagens utilizáveis em base64 ou URL. */
    @Override
    public ImageGenerationOutput validateAndParse(String modelResponse) {
        try {
            JsonNode root = objectMapper.readTree(modelResponse);
            JsonNode imagesNode = root.path("images");
            if (imagesNode.isArray()) {
                return parseConsolidatedResponse(root, imagesNode);
            }
            return parseSingleOpenAiResponse(root);
        } catch (InvalidModelResponseException error) {
            log.error("Invalid OpenAI image response contract. rawResponse={}", modelResponse, error);
            throw error;
        } catch (Exception error) {
            log.error("Could not parse OpenAI image response. rawResponse={}", modelResponse, error);
            throw new InvalidModelResponseException("Could not parse OpenAI image response", error);
        }
    }

    /** Interpreta o manifesto consolidado produzido pelo client quando há múltiplos prompts planejados. */
    private ImageGenerationOutput parseConsolidatedResponse(JsonNode root, JsonNode imagesNode) {
        List<ImageGenerationOutput.GeneratedImage> images = new ArrayList<>();
        for (JsonNode imageNode : imagesNode) {
            JsonNode raw = imageNode.path("rawResponse");
            if (raw.isMissingNode() || raw.isNull()) {
                raw = imageNode;
            }
            ImageContent imageContent = extractImageContent(raw);
            images.add(new ImageGenerationOutput.GeneratedImage(
                    text(imageNode.path("planningItemKey")),
                    text(imageNode.path("sectionId")),
                    text(imageNode.path("elementId")),
                    text(imageNode.path("imageGoal")),
                    text(imageNode.path("prompt")),
                    firstText(text(imageNode.path("model")), text(root.path("model"))),
                    imageContent.bytes(),
                    imageContent.url()
            ));
        }
        if (images.isEmpty()) {
            throw new InvalidModelResponseException("OpenAI image response did not include images");
        }
        return new ImageGenerationOutput(images);
    }

    /** Mantém compatibilidade com respostas diretas da Images API contendo data[0]. */
    private ImageGenerationOutput parseSingleOpenAiResponse(JsonNode root) {
        ImageContent imageContent = extractImageContent(root);
        return new ImageGenerationOutput(List.of(new ImageGenerationOutput.GeneratedImage(
                null,
                null,
                null,
                null,
                text(root.path("prompt")),
                text(root.path("model")),
                imageContent.bytes(),
                imageContent.url()
        )));
    }

    /** Extrai bytes ou URL do primeiro item de data retornado pela Images API. */
    private ImageContent extractImageContent(JsonNode root) {
        JsonNode first = root.path("data").isArray() && !root.path("data").isEmpty()
                ? root.path("data").get(0)
                : null;
        if (first == null || first.isMissingNode() || first.isNull()) {
            throw new InvalidModelResponseException("OpenAI image response did not include data[0]");
        }

        String base64 = text(first.path("b64_json"));
        String imageUrl = text(first.path("url"));
        byte[] imageContent = decodeBase64(base64);
        if ((imageContent == null || imageContent.length == 0) && !StringUtils.hasText(imageUrl)) {
            throw new InvalidModelResponseException("OpenAI image response did not include b64_json or url");
        }
        return new ImageContent(imageContent, imageUrl);
    }

    /** Decodifica o conteúdo base64 quando a OpenAI retorna bytes inline. */
    private byte[] decodeBase64(String base64) {
        if (!StringUtils.hasText(base64)) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException error) {
            log.error("OpenAI image response has invalid base64 payload", error);
            throw new InvalidModelResponseException("OpenAI image response has invalid base64 payload", error);
        }
    }

    /** Converte nós textuais em String preservando nulo para campos ausentes. */
    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    /** Retorna o primeiro texto preenchido entre os candidatos informados. */
    private String firstText(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    /** Responsabilidade: transportar o conteúdo extraído da resposta de imagem. */
    private record ImageContent(byte[] bytes, String url) {
        /** Preserva bytes ou URL extraídos sem transformação adicional. */
        private ImageContent {
        }
    }
}
