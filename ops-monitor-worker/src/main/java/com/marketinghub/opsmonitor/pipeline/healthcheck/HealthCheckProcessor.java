package com.marketinghub.opsmonitor.pipeline.healthcheck;

import com.marketinghub.opsmonitor.pipeline.StageContext;
import com.marketinghub.opsmonitor.pipeline.StageProcessor;
import java.time.Duration;
import java.time.Instant;
import org.springframework.web.reactive.function.client.WebClient;

/** Executa a verificação ativa de disponibilidade técnica de um módulo. */
public class HealthCheckProcessor implements StageProcessor<HealthCheckInput, HealthCheckOutput> {
    private final WebClient webClient;

    /** Recebe o cliente HTTP usado exclusivamente por esta etapa concreta. */
    public HealthCheckProcessor(WebClient webClient) {
        this.webClient = webClient;
    }

    /** Chama o endpoint de saúde e classifica sucesso, falha HTTP ou timeout. */
    @Override
    public HealthCheckOutput process(StageContext context, HealthCheckInput input) {
        long started = System.nanoTime();
        Instant checkedAt = Instant.now();
        try {
            var entity = webClient.get().uri(input.url()).exchangeToMono(response -> response.toEntity(String.class))
                    .timeout(timeout(input)).block();
            long elapsed = elapsedMs(started);
            int statusCode = entity.getStatusCode().value();
            String status = statusCode >= 200 && statusCode < 300 ? statusForSuccessfulBody(input, entity.getBody()) : "DEGRADED";
            return new HealthCheckOutput(input.moduleCode(), checkedAt, status, statusCode, elapsed, entity.getBody(), null);
        } catch (RuntimeException ex) {
            return new HealthCheckOutput(input.moduleCode(), checkedAt, "OFFLINE", null, elapsedMs(started), null,
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /** Resolve o timeout efetivo da chamada. */
    private Duration timeout(HealthCheckInput input) {
        return input.timeout() == null ? Duration.ofSeconds(5) : input.timeout();
    }

    /** Calcula o tempo de resposta em milissegundos. */
    private long elapsedMs(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    /** Valida contratos específicos quando a resposta HTTP principal foi bem-sucedida. */
    private String statusForSuccessfulBody(HealthCheckInput input, String body) {
        if (input.url() == null || !input.url().contains(".m3u8")) {
            return "ONLINE";
        }
        String firstSegment = firstHlsSegment(body);
        if (firstSegment == null || firstSegment.isBlank()) {
            return "DEGRADED";
        }
        try {
            var segment = webClient.get().uri(resolveSiblingUrl(input.url(), firstSegment))
                    .exchangeToMono(response -> response.toBodilessEntity()).timeout(timeout(input)).block();
            return segment.getStatusCode().is2xxSuccessful() ? "ONLINE" : "DEGRADED";
        } catch (RuntimeException ex) {
            return "DEGRADED";
        }
    }

    /** Extrai o primeiro segmento real de um manifesto HLS. */
    private String firstHlsSegment(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        for (String line : body.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank() && !trimmed.startsWith("#")) {
                return trimmed;
            }
        }
        return null;
    }

    /** Resolve um segmento relativo ao diretório da playlist HLS. */
    private String resolveSiblingUrl(String manifestUrl, String segmentPath) {
        if (segmentPath.startsWith("http://") || segmentPath.startsWith("https://")) {
            return segmentPath;
        }
        int directoryEnd = manifestUrl.lastIndexOf('/');
        String directory = directoryEnd >= 0 ? manifestUrl.substring(0, directoryEnd + 1) : manifestUrl;
        return directory + segmentPath;
    }
}
