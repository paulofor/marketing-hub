package com.marketinghub.payments.integration.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/** Integra o pipeline Agenda Cheia à geração fotográfica versionada da OpenAI. */
public class OpenAiAgendaCheiaPhotoGenerator implements AgendaCheiaPhotoGenerator {
    private static final Logger log = LoggerFactory.getLogger(OpenAiAgendaCheiaPhotoGenerator.class);
    private static final List<String> STYLES = List.of(
            "clean girl leitoso", "french moderno", "cat-eye vinho", "chrome rosé", "jelly nude",
            "micro french dourado", "baby boomer sofisticado", "vermelho cereja glossy", "nude mocha", "azul profundo minimalista");
    private static final List<String> SCENES = List.of(
            "mão apoiada em tecido de linho", "close editorial em mesa de salão",
            "mão segurando delicadamente um frasco sem marca", "mão sobre pedra clara",
            "gesto natural com joia minimalista", "mãos sobre bolsa de couro neutra",
            "detalhe das unhas ao segurar uma xícara de cerâmica", "mão apoiada em vestido acetinado",
            "close lateral com fundo de salão desfocado", "mãos em pose natural sobre mesa de madeira clara");
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String model;
    private final String promptTemplate;

    /** Configura cliente, modelo e prompt versionado da geração fotográfica. */
    public OpenAiAgendaCheiaPhotoGenerator(@Value("${agenda-cheia.production.openai-api-key:${OPENAI_API_KEY:}}") String apiKey,
                                           @Value("${agenda-cheia.production.openai-base-url:https://api.openai.com/v1}") String baseUrl,
                                           @Value("${agenda-cheia.production.image-model:gpt-image-2-2026-04-21}") String model,
                                           ObjectMapper mapper) throws IOException {
        this.mapper = mapper;
        this.model = model;
        this.client = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).build();
        this.promptTemplate = new ClassPathResource("prompts/agenda-cheia/nail-photo.md")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    /** Gera uma fotografia e valida a resposta binária recebida do provedor. */
    @Override
    public BufferedImage generate(String executionId, int variant) {
        String prompt = promptTemplate.replace("{{NAIL_STYLE}}", STYLES.get(variant % STYLES.size()))
                .replace("{{SCENE}}", SCENES.get(variant % SCENES.size()));
        try {
            log.info("Gerando fotografia Agenda Cheia. executionId={}, variant={}, model={}", executionId, variant, model);
            String raw = client.post().uri("/images/generations")
                    .body(Map.of("model", model, "prompt", prompt, "size", "1024x1024", "quality", "high"))
                    .retrieve().body(String.class);
            JsonNode response = mapper.readTree(raw);
            String encoded = response.path("data").path(0).path("b64_json").asText();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)));
            if (image == null) throw new IllegalStateException("O provedor não retornou uma imagem válida");
            log.info("Fotografia Agenda Cheia recebida. executionId={}, variant={}, width={}, height={}", executionId, variant, image.getWidth(), image.getHeight());
            return image;
        } catch (Exception ex) {
            log.error("Falha na geração fotográfica Agenda Cheia. executionId={}, variant={}, endpoint=/images/generations", executionId, variant, ex);
            throw new IllegalStateException("Não foi possível gerar a fotografia premium", ex);
        }
    }
}
