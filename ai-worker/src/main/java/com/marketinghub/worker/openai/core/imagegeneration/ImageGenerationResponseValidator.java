package com.marketinghub.worker.openai.core.imagegeneration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import com.marketinghub.worker.openai.core.port.StageResponseValidator;
import java.util.Base64;
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

    /** Valida que a resposta contém uma imagem utilizável em base64 ou URL. */
    @Override
    public ImageGenerationOutput validateAndParse(String modelResponse) {
        try {
            JsonNode root = objectMapper.readTree(modelResponse);
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

            return new ImageGenerationOutput(
                    text(root.path("model")),
                    text(root.path("prompt")),
                    imageContent,
                    imageUrl
            );
        } catch (InvalidModelResponseException error) {
            log.error("Invalid OpenAI image response contract. rawResponse={}", modelResponse, error);
            throw error;
        } catch (Exception error) {
            log.error("Could not parse OpenAI image response. rawResponse={}", modelResponse, error);
            throw new InvalidModelResponseException("Could not parse OpenAI image response", error);
        }
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
}
