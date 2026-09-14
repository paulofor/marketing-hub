package com.marketinghub.videomanagement.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Responsabilidade: executar e auditar a integração OpenAI usada pelo planejador de Apolo. */
@Component
public class ApolloPlanningAiClient {
    private static final Logger log = LoggerFactory.getLogger(ApolloPlanningAiClient.class);
    private static final Set<String> TEMPORARY = Set.of("resource_unavailable", "rate_limit_exceeded", "slow_down");
    private static final Set<String> ACCOUNT_BLOCKS = Set.of("insufficient_quota", "credit_balance_exhausted",
            "organization_spend_limit_exceeded", "project_spend_limit_exceeded", "organization_usage_limit_exceeded");
    private final VideoManagementProperties properties;
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Sleeper sleeper;

    /** Configura a integração com espera limitada somente após rejeição temporária explícita. */
    @Autowired
    public ApolloPlanningAiClient(VideoManagementProperties properties, WebClient.Builder builder) {
        this(properties, builder, Thread::sleep);
    }

    /** Permite simular a passagem do tempo sem rede externa nos testes do contrato HTTP. */
    ApolloPlanningAiClient(VideoManagementProperties properties, WebClient.Builder builder, Sleeper sleeper) {
        this.properties = properties;
        this.webClient = builder.baseUrl(properties.getApolloPlanner().getOpenAiBaseUrl().toString())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().disableRetry(true))).build();
        this.sleeper = sleeper;
    }

    /** Persiste cada interação e repete somente rejeições conhecidas, mantendo modelo e Flex. */
    public JsonNode plan(Long jobId, JsonNode request, Consumer<JsonNode> audit) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new VideoProviderException("APOLLO_PLANNING_AUTH_ERROR", "Credencial do planejador ausente; vídeo não iniciado.");
        }
        String endpoint = properties.getApolloPlanner().getOpenAiBaseUrl().toString().replaceAll("/$", "") + "/responses";
        for (int attempt = 1; attempt <= 3; attempt++) {
            var event = mapper.createObjectNode();
            event.put("eventType", "APOLLO_PLANNING_HTTP");
            event.put("jobId", jobId);
            event.put("attempt", attempt);
            event.put("endpoint", endpoint);
            event.put("model", request.path("model").asText());
            event.put("serviceTier", request.path("service_tier").asText());
            event.set("request", request);
            event.put("status", "REQUESTED");
            audit.accept(event.deepCopy());
            try {
                log.info("Request do planejador Apolo; jobId={} tentativa={} endpoint={} payload={}", jobId, attempt, endpoint, request);
                var response = webClient.post().uri("/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .bodyValue(request)
                        .exchangeToMono(result -> result.toEntity(String.class))
                        .block(Duration.ofMinutes(10));
                if (response == null) throw new IllegalStateException("Resposta HTTP ausente");
                String raw = response.getBody() == null ? "" : response.getBody().replace(apiKey, "[CREDENCIAL_REMOVIDA]");
                int status = response.getStatusCode().value();
                event.put("httpStatus", status);
                event.put("requestId", response.getHeaders().getFirst("x-request-id"));
                event.put("retryAfter", response.getHeaders().getFirst("Retry-After"));
                event.put("rawResponse", raw);
                event.put("status", response.getStatusCode().is2xxSuccessful() ? "RECEIVED" : "REJECTED");
                audit.accept(event.deepCopy());
                log.info("Response do planejador Apolo; jobId={} tentativa={} endpoint={} status={} response={}", jobId, attempt, endpoint, status, raw);
                JsonNode body = readResponse(raw, jobId);
                if (response.getStatusCode().is2xxSuccessful()) {
                    if (body == null) throw new VideoProviderException("APOLLO_PLANNING_RESPONSE_INVALID", "Resposta de planejamento inválida; vídeo não iniciado.");
                    return body;
                }
                String code = errorCode(body);
                Long delay = retryDelay(status, code, response.getHeaders().getFirst("Retry-After"), attempt, Instant.now());
                if (delay != null) {
                    event.put("status", "RETRY_WAIT");
                    event.put("retryDelayMillis", delay);
                    audit.accept(event.deepCopy());
                    sleeper.sleep(delay);
                    continue;
                }
                String failure = ACCOUNT_BLOCKS.contains(code) ? "APOLLO_PLANNING_ACCOUNT_BLOCKED"
                        : status == 401 || status == 403 ? "APOLLO_PLANNING_AUTH_ERROR"
                        : status == 429 && TEMPORARY.contains(code) ? "APOLLO_PLANNING_UNAVAILABLE"
                        : "APOLLO_PLANNING_HTTP_ERROR";
                throw new VideoProviderException(failure, "Planejador recusado pelo fornecedor (HTTP " + status
                        + "); consulte a resposta auditada antes de nova solicitação. Vídeo não iniciado.");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("Espera de Apolo interrompida; jobId={} endpoint={}", jobId, endpoint, ex);
                throw new VideoProviderException("APOLLO_PLANNING_INTERRUPTED", "Planejamento interrompido; sem nova tentativa.", ex);
            } catch (VideoProviderException ex) {
                log.error("Planejador Apolo bloqueado; jobId={} tentativa={} endpoint={} code={}", jobId, attempt, endpoint, ex.getCode(), ex);
                throw ex;
            } catch (RuntimeException ex) {
                log.error("Falha de integração do planejador; jobId={} tentativa={} endpoint={}", jobId, attempt, endpoint, ex);
                event.put("status", "TRANSPORT_OR_AUDIT_ERROR");
                event.put("errorType", ex.getClass().getSimpleName());
                audit.accept(event.deepCopy());
                throw new VideoProviderException("APOLLO_PLANNING_TRANSPORT_ERROR", "Falha de integração sem resposta confirmada; não repetir sem conciliar. Vídeo não iniciado.", ex);
            }
        }
        throw new IllegalStateException("Limite de tentativas inconsistente");
    }

    /** Retorna o código explícito, usando o tipo somente quando não há código específico. */
    private String errorCode(JsonNode body) {
        if (body == null) return "unknown";
        String code = body.path("error").path("code").asText("");
        return code.isBlank() ? body.path("error").path("type").asText("unknown") : code;
    }

    /** Respeita Retry-After e limita a três envios, sem repetir quota ou transporte ambíguo. */
    static Long retryDelay(int status, String code, String retryAfter, int attempt, Instant now) {
        if (status != 429 || !TEMPORARY.contains(code) || attempt >= 3) return null;
        long delay = 5000L * (1L << (attempt - 1)) + ThreadLocalRandom.current().nextLong(1000L);
        if (StringUtils.hasText(retryAfter)) {
            try {
                long required = retryAfter.matches("[0-9]+(?:\\.[0-9]+)?")
                        ? (long) Math.ceil(Double.parseDouble(retryAfter) * 1000)
                        : Math.max(0, Duration.between(now, ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toMillis());
                if (required > 60000) return null;
                delay = Math.max(delay, required);
            } catch (DateTimeParseException | NumberFormatException ex) {
                log.warn("Retry-After inválido no planejador; status={} tentativa={}", status, attempt, ex);
                return null;
            }
        }
        return delay;
    }

    /** Analisa o corpo após preservá-lo, sem converter resposta inválida em aprovação. */
    private JsonNode readResponse(String raw, Long jobId) {
        try {
            return mapper.readTree(raw);
        } catch (IOException ex) {
            log.error("Resposta do planejador não é JSON; jobId={} endpoint=/responses", jobId, ex);
            return null;
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

    /** Responsabilidade: abstrair somente a espera entre rejeições temporárias explícitas. */
    @FunctionalInterface
    interface Sleeper {
        /** Aguarda o intervalo validado ou propaga a interrupção do executor. */
        void sleep(long millis) throws InterruptedException;
    }
}
