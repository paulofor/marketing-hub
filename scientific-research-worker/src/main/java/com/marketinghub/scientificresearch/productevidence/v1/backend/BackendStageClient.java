package com.marketinghub.scientificresearch.productevidence.v1.backend;

import com.marketinghub.scientificresearch.config.ScientificResearchProperties;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageBackendPort;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageCode;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageContext;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementa a comunicação HTTP com o backend principal.
 */
@Component
public class BackendStageClient implements StageBackendPort {

    private static final Logger log = LoggerFactory.getLogger(BackendStageClient.class);

    private final WebClient webClient;
    private final ScientificResearchProperties properties;

    /**
     * Configura o client apontando para o backend principal.
     */
    public BackendStageClient(WebClient.Builder builder, ScientificResearchProperties properties) {
        this.webClient = builder.baseUrl(properties.getBackendBaseUrl()).build();
        this.properties = properties;
    }

    /**
     * Busca execuções pendentes no endpoint canônico da etapa.
     */
    @Override
    public List<StageContext> fetchPending(StageCode stageCode) {
        String path = "/api/internal/scientific-research/product-evidence/v1/"
                + stageCode.code()
                + "/stage-executions/pending";
        try {
            List<StageContext> pending = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(path).queryParam("limit", properties.getPendingLimit()).build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<StageContext>>() {})
                    .block(properties.getRequestTimeout());
            return pending == null ? List.of() : pending;
        } catch (Exception ex) {
            log.error("Falha ao buscar pendências do backend stage={} endpoint={}", stageCode.code(), path, ex);
            return List.of();
        }
    }

    /**
     * Envia o resultado funcional da etapa para o backend.
     */
    @Override
    public void reportResult(StageContext context, StageResult result) {
        webClient.post()
                .uri(context.callbackUrl())
                .bodyValue(result)
                .retrieve()
                .toBodilessEntity()
                .block(properties.getRequestTimeout());
    }

    /**
     * Envia uma falha técnica da etapa para o backend.
     */
    @Override
    public void reportFailure(StageContext context, Exception exception) {
        BackendFailureRequest request = new BackendFailureRequest(
                context.jobId(),
                context.executionId(),
                exception.getClass().getName(),
                exception.getMessage());
        webClient.post()
                .uri(context.callbackUrl())
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block(properties.getRequestTimeout());
    }

    /**
     * Representa o payload de falha enviado ao backend.
     */
    public record BackendFailureRequest(String jobId, String executionId, String errorType, String errorMessage) {
    }
}
