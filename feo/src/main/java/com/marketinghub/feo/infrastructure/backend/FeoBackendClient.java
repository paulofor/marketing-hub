package com.marketinghub.feo.infrastructure.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.feo.fabricacaov1.contract.FabricationContext;
import com.marketinghub.feo.fabricacaov1.contract.PackageAssemblyInput;
import com.marketinghub.feo.fabricacaov1.pipeline.StageBackendPort;
import com.marketinghub.feo.fabricacaov1.pipeline.StageCode;
import com.marketinghub.feo.fabricacaov1.pipeline.StageExecution;
import com.marketinghub.feo.fabricacaov1.pipeline.StageResult;
import com.marketinghub.feo.infrastructure.config.FeoProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Integra a FEO com o backend principal por contratos HTTP.
 */
@Component
public class FeoBackendClient implements StageBackendPort {

    private static final Logger log = LoggerFactory.getLogger(FeoBackendClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final FeoProperties properties;

    /**
     * Recebe dependencias de HTTP e serializacao para consumir o backend.
     */
    public FeoBackendClient(WebClient feoBackendWebClient, ObjectMapper objectMapper, FeoProperties properties) {
        this.webClient = feoBackendWebClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Busca pendencias pelo endpoint pending canonico da etapa.
     */
    @Override
    public <I> List<StageExecution<I>> fetchPending(StageCode stageCode, int limit) {
        String uri = "/api/internal/feo/fabricacao/v1/" + stageCode.code() + "/stage-executions/pending?limit=" + limit;
        try {
            List<BackendStageExecution> pending = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> response.releaseBody()
                            .thenReturn(new FeoBackendUnavailableException("Endpoint FEO nao disponivel: " + uri)))
                    .bodyToMono(new ParameterizedTypeReference<List<BackendStageExecution>>() {})
                    .block(REQUEST_TIMEOUT);
            if (pending == null) {
                return List.of();
            }
            return pending.stream().map(item -> this.<I>toStageExecution(stageCode, item)).toList();
        } catch (Exception ex) {
            log.warn("FEO sem pendencias ou backend indisponivel stage={} endpoint={} causa={}",
                    stageCode.code(),
                    uri,
                    ex.getMessage());
            return List.of();
        }
    }

    /**
     * Reporta resultado funcional para o backend.
     */
    @Override
    public <O> void reportResult(StageExecution<?> execution, StageResult<O> result) {
        String uri = "/api/internal/feo/fabricacao/v1/" + execution.stageCode().code()
                + "/stage-executions/" + execution.executionId() + "/complete";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("workerId", properties.workerId());
        body.put("jobId", execution.jobId());
        body.put("status", result.status().name());
        body.put("output", result.output());
        body.put("artifacts", result.artifacts());
        body.put("metrics", result.metrics());
        body.put("blockReason", result.blockReason());
        body.put("nextStageCode", result.nextStageCode() == null ? null : result.nextStageCode().code());
        postResult(uri, body);
    }

    /**
     * Reporta falha tecnica para o backend.
     */
    @Override
    public void reportFailure(StageExecution<?> execution, Exception error) {
        String uri = "/api/internal/feo/fabricacao/v1/" + execution.stageCode().code()
                + "/stage-executions/" + execution.executionId() + "/fail";
        postResult(uri, Map.of(
                "workerId", properties.workerId(),
                "jobId", execution.jobId(),
                "error", error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
    }

    /**
     * Converte o payload do backend na entrada tipada da etapa.
     */
    @SuppressWarnings("unchecked")
    private <I> StageExecution<I> toStageExecution(StageCode stageCode, BackendStageExecution item) {
        Object input = switch (stageCode) {
            case PLANEJAMENTO_ENTREGAVEIS -> objectMapper.convertValue(item.input(), FabricationContext.class);
            case REDACAO_ENTREGAVEIS -> objectMapper.convertValue(item.input(), PackageAssemblyInput.class);
            case GERACAO_ATIVOS_VISUAIS -> objectMapper.convertValue(item.input(), PackageAssemblyInput.class);
            case MONTAGEM_PACOTE -> objectMapper.convertValue(item.input(), PackageAssemblyInput.class);
        };
        return new StageExecution<>(
                item.jobId(),
                item.executionId(),
                stageCode,
                (I) input,
                item.config() == null ? Map.of() : item.config());
    }

    /**
     * Envia callback ao backend registrando log completo em caso de falha.
     */
    private void postResult(String uri, Map<String, Object> body) {
        try {
            webClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block(REQUEST_TIMEOUT);
        } catch (Exception ex) {
            log.error("Falha ao publicar callback FEO endpoint={} jobId={}", uri, body.get("jobId"), ex);
        }
    }

    /**
     * Representa a pendencia crua recebida do backend.
     */
    private record BackendStageExecution(String jobId, String executionId, Map<String, Object> input, Map<String, Object> config) {
    }

    /**
     * Sinaliza indisponibilidade esperada enquanto o backend ainda nao expuser a FEO.
     */
    private static class FeoBackendUnavailableException extends RuntimeException {

        /**
         * Cria excecao com mensagem operacional.
         */
        FeoBackendUnavailableException(String message) {
            super(message);
        }
    }
}
