package com.marketinghub.worker.geralanding.copy.backend;

import com.marketinghub.worker.geralanding.copy.GeraLandingJobCompletionPayload;
import com.marketinghub.worker.geralanding.copy.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.util.UrlUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Responsabilidade: centralizar a integração HTTP da etapa copy com os endpoints do GeraLanding no backend. */
@Component
public class GeraLandingCopyBackendClient {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingCopyBackendClient.class);

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    /** Configura o cliente HTTP com a URL base e o prefixo de API do backend. */
    public GeraLandingCopyBackendClient(WebClient.Builder builder,
                                    @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                    @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /** Lista execuções pendentes da etapa copy respeitando o limite mínimo de consulta. */
    public List<GeraLandingStageExecutionDetailDto> listPendingExecutions(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/copy/stage-executions/pending");
        String uri = url + "?limit=" + Math.max(1, limit);
        log.info("Fetching pending gera-landing stage executions from {}", uri);
        List<GeraLandingStageExecutionDetailDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleListResponse(uri, response.statusCode(), response))
                .collectList()
                .doOnError(err -> log.error("Failed to fetch pending gera-landing stage executions from {}", uri, err))
                .block();
        List<GeraLandingStageExecutionDetailDto> result = payload != null ? payload : List.of();
        log.info("Backend returned {} pending gera-landing stage execution(s)", result.size());
        return result;
    }

    /** Carrega os dados de experimento necessários para montar o prompt da etapa copy. */
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

    /** Preenche no payload os campos de dor e resultado da hipótese selecionada. */
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

    /** Resolve o nome do nicho vinculado ao experimento para enriquecer o prompt. */
    private String resolveNicheName(Object nicheId) {
        if (nicheId == null) {
            return "";
        }
        String nicheUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/niches/" + nicheId);
        Map<String, Object> niche = webClient.get()
                .uri(nicheUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(Map.of())
                .block();
        return niche != null && niche.get("name") != null ? String.valueOf(niche.get("name")) : "";
    }

    /** Converte campos JSON textuais do experimento em mapas quando o conteúdo é válido. */
    private Object parseJsonField(Object value) {
        if (!(value instanceof String raw) || raw.isBlank()) {
            return Map.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw, Map.class);
        } catch (Exception ex) {
            log.warn("Campo textual do experimento não pôde ser convertido de JSON para a etapa copy; mantendo valor bruto", ex);
            return raw;
        }
    }

    /** Envia ao backend o prompt, o request OpenAI e os metadados usados na etapa copy. */
    public void receivePrompt(String idJob,
                              Long experimentId,
                              String stageCode,
                              String prompt,
                              String openAiRequestBody,
                              String openAiModel,
                              String schemaJson,
                              String promptMarkdownContent) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/copy/stage-executions");
        log.info(
                "Sending gera-landing prompt. endpoint={}/{{idJob}}/receive-prompt, idJob={}, experimentId={}, stageCode={}, promptLength={}",
                baseUrl,
                idJob,
                experimentId,
                stageCode,
                prompt != null ? prompt.length() : 0);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("prompt", prompt);
        body.put("openAiRequestBody", openAiRequestBody);
        body.put("openAiModel", openAiModel);
        body.put("schemaJson", schemaJson);
        body.put("promptMarkdownContent", promptMarkdownContent);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-prompt", idJob)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    /** Envia ao backend o resultado final da etapa copy recebido da OpenAI. */
    public void receiveResult(String idJob, Long experimentId, String stageCode, GeraLandingJobCompletionPayload payload) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/copy/stage-executions");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("modelResponse", payload != null ? payload.responseContent() : null);
        body.put("inputTokens", payload != null ? payload.inputTokens() : null);
        body.put("outputTokens", payload != null ? payload.outputTokens() : null);
        body.put("costUsd", payload != null ? payload.costUsd() : null);
        body.put("openAiJobId", payload != null ? payload.openAiJobId() : null);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-result", idJob)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }


    /** Comunica ao backend a falha de processamento da etapa copy com mensagem e detalhe técnico. */
    public void receiveFailure(String idJob, Long experimentId, String stageCode, String errorMessage, String errorDetail) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/copy/stage-executions");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("errorMessage", errorMessage);
        body.put("errorDetail", errorDetail);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-result", idJob)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    /** Registra no backend o identificador do job OpenAI disparado para a etapa copy. */
    public void receiveDispatch(String idJob, Long experimentId, String stageCode, String openAiJobId) {
        String baseUrl = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/internal/geralanding/copy/stage-executions");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("openAiJobId", openAiJobId);
        webClient.post()
                .uri(baseUrl + "/{idJob}/receive-dispatch", idJob)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    /** Consulta detalhes da execução da etapa copy no controller copy.web. */
    public GeraLandingStageExecutionDetailDto fetchCopyStageExecutionDetail(Long experimentId, String idJob) {
        String uri = UrlUtils.joinPath(backendBaseUrl, apiPrefix,
                "/experiments/" + experimentId + "/geralanding/copy/stage-executions/" + idJob);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(GeraLandingStageExecutionDetailDto.class)
                .onErrorReturn(null)
                .block();
    }

    /** Trata a resposta de listagem de execuções pendentes, propagando erro HTTP com corpo da resposta. */
    private Flux<GeraLandingStageExecutionDetailDto> handleListResponse(String uri,
                                                                  HttpStatusCode status,
                                                                  org.springframework.web.reactive.function.client.ClientResponse response) {
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new IllegalStateException(
                            "GET %s failed with status %s: %s".formatted(uri, status, body))));
        }
        return response.bodyToFlux(GeraLandingStageExecutionDetailDto.class);
    }
}
