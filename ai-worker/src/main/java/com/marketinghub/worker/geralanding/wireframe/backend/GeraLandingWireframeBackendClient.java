package com.marketinghub.worker.geralanding.wireframe.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.wireframe.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.wireframe.response.RecordWireframeResponse;
import com.marketinghub.worker.util.UrlUtils;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: encapsular as integrações HTTP da etapa wireframe com o backend sem dependências cruzadas de outras etapas. */
@Component
public class GeraLandingWireframeBackendClient {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingWireframeBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final ObjectMapper objectMapper;

    /** Inicializa o client HTTP com a URL base do backend e o mapper usado para reidratar artefatos JSON. */
    public GeraLandingWireframeBackendClient(
            WebClient.Builder builder,
            @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
            @Value("${backend.api-prefix:/api}") String apiPrefix,
            ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
        this.objectMapper = objectMapper;
    }

    /** Lista execuções pendentes da etapa wireframe usando a fila interna canônica do backend. */
    public List<GeraLandingStageExecutionDetailDto> listPendingExecutions(int limit) {
        int effectiveLimit = Math.max(1, limit);
        String uri = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/wireframe/stage-executions/pending");
        List<Map<String, Object>> payload = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .doOnError(err -> log.error("Falha ao buscar fila interna de pendências wireframe. uri={}", uri, err))
                .onErrorReturn(List.of())
                .block();
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        return payload.stream()
                .map(this::toPendingExecutionDto)
                .filter(item -> item.experimentId() != null && item.idJob() != null)
                .limit(effectiveLimit)
                .toList();
    }

    /** Converte o item da fila interna de wireframe para o DTO consumido pelo scheduler da etapa. */
    private GeraLandingStageExecutionDetailDto toPendingExecutionDto(Map<String, Object> item) {
        return new GeraLandingStageExecutionDetailDto(
                asLong(item.get("experimentId")),
                asString(item.get("stageCode")),
                asString(item.get("jobid")),
                "INICIADO",
                null,
                null,
                null,
                null,
                buildPromptDataFromPending(item));
    }

    /** Carrega os dados de prompt para a geração wireframe a partir do experimento. */
    public Map<String, Object> loadPromptData(Long experimentId) {
        if (experimentId == null) {
            return Map.of();
        }
        String experimentUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/experiments/" + experimentId);
        Map<String, Object> experiment = webClient.get()
                .uri(experimentUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(Map.of())
                .block();
        if (experiment == null || experiment.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignAngle", parseJsonField(experiment.get("campaignAngle")));
        payload.put("adCopy", parseJsonField(experiment.get("adCopy")));
        payload.put("adImageBriefing", parseJsonField(experiment.get("adImageBriefing")));
        payload.put("landingPageWireframe", parseJsonField(experiment.get("landingPageWireframe")));
        payload.put("NICHE_NAME", resolveNicheName(experiment.get("nicheId")));
        populateHypothesisFields(payload, experiment.get("nicheId"), experiment.get("hypothesisId"));
        return payload;
    }

    /** Carrega os dados de prompt preferindo o JSON estruturado recebido da fila interna pending. */
    public Map<String, Object> loadPromptData(GeraLandingStageExecutionDetailDto execution) {
        if (execution != null && execution.promptData() != null && !execution.promptData().isEmpty()) {
            return execution.promptData();
        }
        return execution != null ? loadPromptData(execution.experimentId()) : Map.of();
    }

    /** Monta os dados de prompt a partir do contrato PendingStageExecution enviado pelo backend. */
    private Map<String, Object> buildPromptDataFromPending(Map<String, Object> pending) {
        Map<String, Object> experiment = asMap(pending.get("experiment"));
        Map<String, Object> hypothesis = asMap(pending.get("hypothesis"));
        Map<String, Object> framework = asMap(hypothesis.get("framework"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignAngle", normalizeJsonArtifact(experiment.get("campaignAngle")));
        payload.put("adCopy", normalizeJsonArtifact(experiment.get("adCopy")));
        payload.put("adImageBriefing", normalizeJsonArtifact(experiment.get("adImageBriefing")));
        payload.put("landingPageWireframe", normalizeJsonArtifact(experiment.get("landingPageWireframe")));
        payload.put("NICHE_NAME", firstText(experiment.get("nicheName"), experiment.get("niche"), experiment.get("name")));
        payload.put("PAIN_JSON", framework.getOrDefault("pain", Map.of()));
        payload.put("RESULT_JSON", framework.getOrDefault("result", Map.of()));
        return payload;
    }

    /** Envia estado de falha da execução wireframe ao backend pelo callback específico recebe-resposta. */
    public void receiveFailure(String idJob, Long experimentId, String stageCode, String errorMessage, String errorDetail) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/wireframe/stage-executions");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("errorMessage", errorMessage);
        body.put("errorDetail", errorDetail);
        webClient.post().uri(baseUrl + "/{idJob}/receive-result", idJob).bodyValue(body).retrieve().bodyToMono(Void.class).block();
    }

    /** Envia ao backend o prompt despachado para IA e o identificador do job OpenAI conforme contrato recebe-prompt. */
    public void recebePrompt(String idJob, String prompt, String openAiJobId) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/wireframe/stage-executions");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", prompt);
        body.put("jobidopenai", openAiJobId);
        webClient.post().uri(baseUrl + "/{idJob}/recebe-prompt", idJob).bodyValue(body).retrieve().bodyToMono(Void.class).block();
    }

    /** Envia ao backend o resultado final da etapa wireframe. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, RecordWireframeResponse payload) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/wireframe/stage-executions");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("modelResponse", payload != null ? payload.responseContent() : null);
        body.put("inputTokens", payload != null ? payload.inputTokens() : null);
        body.put("outputTokens", payload != null ? payload.outputTokens() : null);
        body.put("costUsd", payload != null ? payload.costUsd() : null);
        body.put("openAiJobId", payload != null ? payload.openAiJobId() : null);
        webClient.post().uri(baseUrl + "/{idJob}/recebe-resposta", idJob).bodyValue(body).retrieve().bodyToMono(Void.class).block();
    }

    /** Preenche no payload dados de hipótese usados para montar o prompt da etapa. */
    private void populateHypothesisFields(Map<String, Object> payload, Object nicheId, Object hypothesisId) {
        if (nicheId == null || hypothesisId == null) {
            return;
        }
        String hypothesesUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/niches/" + nicheId + "/hypotheses");
        List<Map<String, Object>> hypotheses = webClient.get()
                .uri(hypothesesUrl)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .onErrorReturn(List.of())
                .block();
        if (hypotheses == null || hypotheses.isEmpty()) {
            return;
        }
        String hypothesisIdText = String.valueOf(hypothesisId);
        hypotheses.stream()
                .filter(item -> hypothesisIdText.equalsIgnoreCase(String.valueOf(item.get("id"))))
                .findFirst()
                .ifPresent(hypothesis -> {
                    Object framework = hypothesis.get("framework");
                    if (framework instanceof Map<?, ?> map) {
                        Object pain = map.get("pain");
                        Object result = map.get("result");
                        if (pain != null) {
                            payload.put("PAIN_JSON", pain);
                        }
                        if (result != null) {
                            payload.put("RESULT_JSON", result);
                        }
                    }
                });
    }

    /** Resolve o nome do nicho usando o endpoint de nichos do backend. */
    private String resolveNicheName(Object nicheId) {
        if (nicheId == null) {
            return "";
        }
        String nicheUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/niches/" + nicheId);
        Map<String, Object> niche = webClient.get().uri(nicheUrl).retrieve().bodyToMono(Map.class).onErrorReturn(Map.of()).block();
        return niche != null && niche.get("name") != null ? String.valueOf(niche.get("name")) : "";
    }

    /** Converte objetos genéricos de JSON em mapa quando possível, ou retorna mapa vazio. */
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> converted = new LinkedHashMap<>();
            rawMap.forEach((key, rawValue) -> {
                if (key != null) {
                    converted.put(String.valueOf(key), rawValue);
                }
            });
            return converted;
        }
        return Map.of();
    }

    /** Normaliza artefatos recebidos da fila pending preservando JSON estruturado e convertendo strings JSON legadas. */
    private Object normalizeJsonArtifact(Object value) {
        if (value instanceof String text) {
            return parseJsonField(text);
        }
        return value != null ? value : Map.of();
    }

    /** Retorna o primeiro valor textual preenchido entre possíveis nomes vindos do contrato do backend. */
    private String firstText(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return "";
    }

    /** Tenta converter campos JSON serializados para mapa, mantendo texto cru quando inválido. */
    private Object parseJsonField(Object value) {
        if (!(value instanceof String raw) || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (Exception ex) {
            log.warn("Falha ao converter campo JSON de prompt wireframe; mantendo valor textual bruto (length={})", raw.length(), ex);
            return raw;
        }
    }

    /** Converte valores numéricos vindos de payloads genéricos em Long. */
    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    /** Converte valores textuais vindos de payloads genéricos em String segura. */
    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    /** Converte valores de data/hora vindos de payloads genéricos em Instant. */
    private Instant asInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Instant.parse(text.trim());
        }
        return null;
    }
}
