package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import com.marketinghub.worker.pipeline.StageBackendPort;
import com.marketinghub.worker.pipeline.StageExecution;
import com.marketinghub.worker.pipeline.StageResult;
import com.marketinghub.worker.util.UrlUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: consumir os contratos backend da etapa Texto do GeracaoAnuncios v1. */
public class GeraAnuncioTextoBackendClient implements StageBackendPort<GeraAnuncioTextoInput, GeraAnuncioTextoOutput> {
    public static final String PENDING_ENDPOINT = "/internal/aiworker/geracaoanuncios/v1/texto/stage-executions/pending";

    private static final Logger log = LoggerFactory.getLogger(GeraAnuncioTextoBackendClient.class);

    private final WebClient webClient;
    private final GeraAnuncioTextoWorkerProperties properties;

    /** Inicializa o cliente HTTP com URL base e propriedades oficiais da etapa. */
    public GeraAnuncioTextoBackendClient(WebClient.Builder builder, GeraAnuncioTextoWorkerProperties properties) {
        this.webClient = builder.build();
        this.properties = properties;
    }

    /** Busca execuções pendentes pelo endpoint pending canônico da etapa Texto no backend. */
    public List<GeraAnuncioTextoInput> fetchPending() {
        return listPending(properties.pendingLimit()).stream().map(StageExecution::input).toList();
    }

    /** Lista execuções pendentes e adapta o contrato do backend ao worker genérico. */
    @Override
    public List<StageExecution<GeraAnuncioTextoInput>> listPending(int limit) {
        String uri = stageExecutionBaseUrl() + "/pending";
        log.info("Buscando pending GeracaoAnuncios v1 Texto. endpoint={}", uri);
        List<GeraAnuncioTextoInput> response = webClient.post()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GeraAnuncioTextoInput>>() {})
                .block(properties.timeout());
        List<GeraAnuncioTextoInput> pending = response != null ? response : List.of();
        log.info("Resposta pending GeracaoAnuncios v1 Texto recebida. endpoint={} quantidade={}", uri, pending.size());
        return pending.stream().limit(Math.max(1, limit)).map(this::toExecution).toList();
    }

    /** Registra em log o início local da execução antes do processamento da etapa. */
    @Override
    public void markRunning(StageExecution<GeraAnuncioTextoInput> execution) {
        log.info("Iniciando GeracaoAnuncios v1 Texto. jobId={} stageExecutionId={}", execution.idJob(), execution.input().stageExecutionId());
    }

    /** Envia ao backend a saída funcional estruturada produzida pela etapa. */
    @Override
    public void markCompleted(StageExecution<GeraAnuncioTextoInput> execution, StageResult<GeraAnuncioTextoOutput> result) {
        String uri = stageExecutionBaseUrl() + "/" + execution.input().stageExecutionId() + "/response";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", execution.idJob());
        body.put("receivedAt", java.time.Instant.now().toString());
        body.put("status", "COMPLETED");
        body.put("response", result.output());
        body.put("responsePayload", Map.of("artifacts", result.artifacts(), "metrics", result.metrics()));
        body.put("structuredOutput", Map.of("output", result.output()));
        body.put("error", null);
        body.put("descricaoErro", null);
        body.put("quantidadeTokenEntrada", null);
        body.put("quantidadeTokenSaida", null);
        body.put("custo", null);
        body.put("modelo", "deterministic-geracaoanuncios-v1-texto");
        log.info("Enviando resultado GeracaoAnuncios v1 Texto ao backend. endpoint={} jobId={} payload={}", uri, execution.idJob(), body);
        webClient.post().uri(uri).bodyValue(body).retrieve().bodyToMono(Void.class).block(properties.timeout());
    }

    /** Registra falha com contexto suficiente para diagnóstico operacional da etapa. */
    @Override
    public void markFailed(StageExecution<GeraAnuncioTextoInput> execution, Throwable error) {
        log.error(
                "Falha no processamento GeracaoAnuncios v1 Texto. jobId={} stageExecutionId={}",
                execution.idJob(),
                execution.input().stageExecutionId(),
                error);
    }

    /** Adapta a entrada pendente para o contrato genérico de execução de etapa. */
    private StageExecution<GeraAnuncioTextoInput> toExecution(GeraAnuncioTextoInput input) {
        return new StageExecution<>(
                input.jobId(),
                input.experimentId(),
                "texto",
                "PENDING",
                input.requestedAt(),
                input,
                Map.of());
    }

    /** Monta a URL base dos endpoints internos da etapa no backend. */
    private String stageExecutionBaseUrl() {
        return UrlUtils.joinPath(properties.backendBaseUrl(), properties.apiPrefix(), PENDING_ENDPOINT.replace("/pending", ""));
    }
}
