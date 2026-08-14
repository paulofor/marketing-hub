package com.marketinghub.videomanagement.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: executar e auditar a integração OpenAI usada pelo planejador de Apolo. */
@Component
public class ApolloPlanningAiClient {
    private final Logger log = LoggerFactory.getLogger(ApolloPlanningAiClient.class);
    private final VideoManagementProperties properties;
    private final WebClient webClient;

    /** Configura o endpoint externo mantendo a tecnologia fora do núcleo do pipeline. */
    public ApolloPlanningAiClient(VideoManagementProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.baseUrl(properties.getApolloPlanner().getOpenAiBaseUrl().toString()).build();
    }

    /** Envia o request auditável e devolve a resposta bruta para validação pelo planejador. */
    public JsonNode plan(Long jobId, JsonNode request) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw blocked("Credencial do planejador de IA ausente; provider pago não foi chamado.");
        }
        try {
            log.info("Request do planejador Apolo; jobId={} endpoint=/responses payload={}", jobId, request);
            JsonNode response = webClient.post().uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            log.info("Response do planejador Apolo; jobId={} response={}", jobId, response);
            return response;
        } catch (VideoProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Falha no planejador Apolo; jobId={} endpoint=/responses", jobId, ex);
            throw new VideoProviderException("APOLLO_STORYBOARD_BLOCKED",
                    "Planejamento de IA falhou; provider pago não foi chamado.", ex);
        }
    }

    /** Resolve a credencial por valor direto ou secret montado sem expô-la em logs. */
    private String resolveApiKey() {
        String direct = properties.getApolloPlanner().getApiKey();
        if (StringUtils.hasText(direct)) return direct.trim();
        String file = properties.getApolloPlanner().getApiKeyFile();
        if (!StringUtils.hasText(file)) return null;
        try {
            return Files.readString(Path.of(file), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            log.error("Falha ao ler secret do planejador Apolo; arquivo={}", file, ex);
            return null;
        }
    }

    /** Cria bloqueio funcional anterior a qualquer chamada paga de vídeo. */
    private VideoProviderException blocked(String message) {
        return new VideoProviderException("APOLLO_STORYBOARD_BLOCKED", message);
    }
}
