package com.marketinghub.videomanagement.pdeaudiovisualv1;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import io.netty.channel.ChannelOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

/** Responsabilidade: transportar a atividade audiovisual BPM entre Apolo e o backend canônico. */
@Component
public class ApolloPdeAudiovisualBackendClient {
    static final String AGENT_KEY = "videomaker";
    static final List<String> PROCESS_CODES =
            List.of("pde-construction-approval", "creative-production-approval");
    static final String ACTIVITY_ID = "audiovisual";
    static final String EXECUTION_RESOURCE_CODE = "video-management-service";
    static final String WORKER_CONTRACT = "APOLLO_AUDIOVISUAL_V1";
    static final String PENDING_ENDPOINT =
            "/api/internal/agent-tasks/{agent}/stage-executions/pending"
                    + "?processCode={processCode}&activityId={activityId}"
                    + "&executionResourceCode={executionResourceCode}"
                    + "&workerContract={workerContract}";
    static final String RESULT_ENDPOINT =
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/result";
    static final String FAILURE_ENDPOINT =
            "/api/internal/agent-tasks/{agent}/stage-executions/{taskId}/failure";
    private static final Logger log = LoggerFactory.getLogger(ApolloPdeAudiovisualBackendClient.class);
    private static final ParameterizedTypeReference<List<ApolloPdeAudiovisualTask>> TASK_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private final WebClient backend;
    private final VideoManagementProperties properties;
    private final AtomicInteger nextProcessIndex = new AtomicInteger();

    /** Configura URL, autenticação, timeout e limite de resposta do executor audiovisual. */
    public ApolloPdeAudiovisualBackendClient(
            WebClient.Builder builder, VideoManagementProperties properties) {
        long timeoutMillis = properties.getPdeAudiovisual().getBackendTimeout().toMillis();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(timeoutMillis))
                .responseTimeout(properties.getPdeAudiovisual().getBackendTimeout());
        this.backend = builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(properties.getPdeAudiovisual().getMaxResponseBytes()))
                .baseUrl(properties.getBackendBaseUrl().toString())
                .build();
        this.properties = properties;
    }

    /** Reserva no máximo uma tarefa nas filas audiovisuais canônicas, alternando a prioridade. */
    public ApolloPdeAudiovisualTask claim() {
        int firstIndex = Math.floorMod(nextProcessIndex.getAndIncrement(), PROCESS_CODES.size());
        for (int offset = 0; offset < PROCESS_CODES.size(); offset++) {
            String processCode = PROCESS_CODES.get((firstIndex + offset) % PROCESS_CODES.size());
            ApolloPdeAudiovisualTask task = claim(processCode);
            if (task != null) {
                return task;
            }
        }
        return null;
    }

    /** Consulta uma única fila exata sem repetir uma reserva cuja resposta possa ter se perdido. */
    private ApolloPdeAudiovisualTask claim(String processCode) {
        try {
            log.info(
                    "Consultando fila audiovisual BPM de Apolo. url={} processCode={} activityId={} resource={}",
                    PENDING_ENDPOINT,
                    processCode,
                    ACTIVITY_ID,
                    EXECUTION_RESOURCE_CODE);
            List<ApolloPdeAudiovisualTask> pending = authorized(backend.get()
                            .uri(
                                    PENDING_ENDPOINT,
                                    AGENT_KEY,
                                    processCode,
                                    ACTIVITY_ID,
                                    EXECUTION_RESOURCE_CODE,
                                    WORKER_CONTRACT))
                    .retrieve()
                    .bodyToMono(TASK_LIST_TYPE)
                    .blockOptional()
                    .orElse(Collections.emptyList());
            log.info(
                    "Resposta da fila audiovisual BPM recebida. url={} quantidade={}",
                    PENDING_ENDPOINT,
                    pending.size());
            return pending.isEmpty() ? null : pending.getFirst();
        } catch (RuntimeException ex) {
            log.error(
                    "Falha no video-management-service ao consultar fila audiovisual BPM. url={} processCode={} activityId={}",
                    PENDING_ENDPOINT,
                    processCode,
                    ACTIVITY_ID,
                    ex);
            throw ex;
        }
    }

    /** Envia a decisão conclusiva sem escolher ou disparar a próxima atividade. */
    public void complete(long taskId, Map<String, Object> payload) {
        post(taskId, RESULT_ENDPOINT, payload, "concluir");
    }

    /** Envia o bloqueio funcional antes de qualquer chamada paga ou interpretação ambígua. */
    public void block(long taskId, Map<String, Object> payload) {
        post(taskId, FAILURE_ENDPOINT, payload, "bloquear");
    }

    /** Executa o callback oficial e registra URL, tarefa e resposta sem expor conteúdo sensível. */
    private void post(long taskId, String endpoint, Map<String, Object> payload, String operation) {
        try {
            log.info("Enviando callback audiovisual BPM. url={} taskId={} operacao={}", endpoint, taskId, operation);
            Mono<?> request = authorized(backend.post()
                            .uri(endpoint, AGENT_KEY, taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(payload))
                    .retrieve()
                    .toBodilessEntity();
            withRetry(request, operation, taskId).block();
            log.info("Resposta do callback audiovisual BPM recebida. url={} taskId={}", endpoint, taskId);
        } catch (RuntimeException ex) {
            log.error(
                    "Falha no video-management-service ao {} atividade audiovisual. url={} taskId={}",
                    operation,
                    endpoint,
                    taskId,
                    ex);
            throw ex;
        }
    }

    /** Aplica o token interno somente quando ele estiver realmente configurado. */
    private WebClient.RequestHeadersSpec<?> authorized(WebClient.RequestHeadersSpec<?> request) {
        if (StringUtils.hasText(properties.getAuthToken())) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthToken());
        }
        return request;
    }

    /** Repete somente indisponibilidade transitória dentro do limite já governado para o backend. */
    private <T> Mono<T> withRetry(Mono<T> request, String operation, Long taskId) {
        int retries = Math.max(0, properties.getJobs().getBackendCallMaxAttempts() - 1);
        if (retries == 0) {
            return request;
        }
        return request.retryWhen(Retry.fixedDelay(
                        retries,
                        properties.getJobs().getBackendCallBackoff())
                .filter(this::isRetryable)
                .doBeforeRetry(signal -> log.warn(
                        "Falha transitória no backend; repetindo operação BPM. operacao={} taskId={} tentativa={}",
                        operation,
                        taskId,
                        signal.totalRetries() + 2,
                        signal.failure())));
    }

    /** Distingue falha de transporte e status transitório de rejeição contratual definitiva. */
    private boolean isRetryable(Throwable error) {
        if (error instanceof WebClientRequestException) {
            return true;
        }
        if (error instanceof WebClientResponseException response) {
            int status = response.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }
}
