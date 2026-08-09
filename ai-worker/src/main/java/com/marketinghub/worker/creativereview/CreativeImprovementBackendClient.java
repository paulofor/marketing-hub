package com.marketinghub.worker.creativereview;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: consumir e concluir correções de anúncios controladas pelo backend. */
@Component
public class CreativeImprovementBackendClient {
    private static final Logger log = LoggerFactory.getLogger(CreativeImprovementBackendClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private final WebClient webClient;
    private final String backendBaseUrl;

    /** Inicializa o cliente usando exclusivamente os endpoints internos da etapa. */
    public CreativeImprovementBackendClient(WebClient.Builder builder,
                                            @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl.replaceAll("/+$", "");
    }

    /** Busca correções pendentes no ponto inicial canônico publicado pelo backend. */
    public List<Map<String, Object>> listPending(int limit) {
        String url = backendBaseUrl + "/api/internal/creatives/agent-improvement/stage-executions/pending?limit="
                + Math.max(1, limit);
        log.info("Buscando correções de anúncios pendentes. url={}", url);
        List<Map<String, Object>> result = webClient.get().uri(url).retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block(TIMEOUT);
        return result == null ? List.of() : result;
    }

    /** Reporta a imagem corrigida ou a falha para o backend decidir o próximo passo. */
    public void report(Long creativeId, Map<String, Object> result) {
        webClient.post()
                .uri(backendBaseUrl + "/api/internal/creatives/{id}/agent-improvement/result", creativeId)
                .bodyValue(result).retrieve().toBodilessEntity().block(TIMEOUT);
    }
}
