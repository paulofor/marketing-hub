package com.marketinghub.opsmonitor.pipeline.healthcheck;

import com.marketinghub.opsmonitor.pipeline.StageContext;
import com.marketinghub.opsmonitor.pipeline.StageProcessor;
import java.time.Duration;
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
        try {
            var entity = webClient.get().uri(input.url()).retrieve().toEntity(String.class)
                    .timeout(timeout(input)).block();
            long elapsed = elapsedMs(started);
            int statusCode = entity.getStatusCode().value();
            String status = statusCode >= 200 && statusCode < 300 ? "ONLINE" : "DEGRADED";
            return new HealthCheckOutput(input.moduleCode(), status, statusCode, elapsed, entity.getBody(), null);
        } catch (RuntimeException ex) {
            return new HealthCheckOutput(input.moduleCode(), "OFFLINE", null, elapsedMs(started), null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
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
}
