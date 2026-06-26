package com.marketinghub.worker.creative;

import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.experiment.dto.ExperimentDto;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Responsabilidade: consumir e atualizar a fila de geração de criativos exposta pelo backend.
 */
@Component
public class CreativeGenerationBackendClient {
    private static final Logger log = LoggerFactory.getLogger(CreativeGenerationBackendClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final String backendBaseUrl;

    /** Inicializa o cliente HTTP com a URL base do backend principal. */
    public CreativeGenerationBackendClient(
            WebClient.Builder builder,
            @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl
    ) {
        this.webClient = builder.build();
        this.backendBaseUrl = normalizeBaseUrl(backendBaseUrl);
    }

    /** Busca experimentos com criativos pendentes para geração. */
    public List<ExperimentDto> listPending(int limit) {
        String url = backendBaseUrl + "/api/experiments/creatives/stage-executions/pending?limit=" + Math.max(1, limit);
        log.info("Buscando criativos pendentes em {}", url);
        List<ExperimentDto> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ExperimentDto>>() {})
                .block(TIMEOUT);
        return response == null ? List.of() : response;
    }

    /** Informa ao backend que o worker iniciou a geração de criativos. */
    public void markStarted(Long experimentId) {
        webClient.post()
                .uri(backendBaseUrl + "/api/experiments/{id}/creatives/stage-execution/start", experimentId)
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }

    /** Persiste um criativo gerado no backend. */
    public void createCreative(Long experimentId, CreateCreativeRequest request) {
        webClient.post()
                .uri(backendBaseUrl + "/api/experiments/{id}/creatives", experimentId)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }

    /** Informa ao backend que a geração de criativos foi concluída. */
    public void markCompleted(Long experimentId) {
        webClient.post()
                .uri(backendBaseUrl + "/api/experiments/{id}/creatives/stage-execution/complete", experimentId)
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }

    /** Informa ao backend que a geração de criativos falhou com uma mensagem operacional. */
    public void markFailed(Long experimentId, String error) {
        webClient.post()
                .uri(backendBaseUrl + "/api/experiments/{id}/creatives/stage-execution/fail", experimentId)
                .bodyValue(new CreativeGenerationFailureRequest(error))
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }

    /** Normaliza a URL base evitando barras duplicadas na montagem dos endpoints. */
    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://191.252.181.168" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** Payload de falha enviado ao backend. */
    private record CreativeGenerationFailureRequest(String error) {
    }
}
