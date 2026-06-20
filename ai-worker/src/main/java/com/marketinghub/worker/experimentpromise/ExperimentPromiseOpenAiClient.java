package com.marketinghub.worker.experimentpromise;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsável por transformar o prompt salvo no backend em opções de promessa usando a OpenAI. */
@Component
public class ExperimentPromiseOpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPromiseOpenAiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    /** Inicializa o cliente da OpenAI com modelo dedicado ao contrato de promessa. */
    public ExperimentPromiseOpenAiClient(WebClient.Builder builder,
                                         ObjectMapper objectMapper,
                                         @Value("${openai.api-key:}") String apiKey,
                                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                         @Value("${openai.experiment-promise-model:gpt-5.2}") String model) {
        WebClient.Builder clientBuilder = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    /** Gera exatamente três opções comerciais para a solicitação assumida. */
    public List<ExperimentPromiseOptionDto> generate(ExperimentPromiseOptionsResponse request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", List.of(
                OpenAiRequestUtils.message("system", "Você cria contratos de promessa única para experimentos de marketing. Responda somente JSON válido."),
                OpenAiRequestUtils.message("user", buildPrompt(request))));
        payload.put("text", Map.of("format", Map.of("type", "json_object")));
        if (OpenAiRequestUtils.supportsTemperature(model)) {
            payload.put("temperature", 0.4);
        }
        OpenAiRequestUtils.maybeAddReasoning(payload, model);
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();
        String content = response != null ? response.firstText() : null;
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI não retornou conteúdo para opções de promessa");
        }
        log.info("OpenAI retornou opções de promessa; requestId={} model={}", request.requestId(), model);
        return parseOptions(content);
    }

    /** Monta o prompt final com contrato de saída rígido. */
    private String buildPrompt(ExperimentPromiseOptionsResponse request) {
        return "Gere exatamente 3 opções diferentes para o contrato de promessa única. "
                + "Cada opção deve ter dor específica, recompensa gratuita, promessa do funil, CTA e motivo. "
                + "Retorne no formato: {\"options\":[{\"singlePain\":\"...\",\"freeReward\":\"...\",\"funnelPromise\":\"...\",\"primaryCta\":\"...\",\"reason\":\"...\"}]}. "
                + "A resposta deve estar em português do Brasil, objetiva e pronta para anúncio/landing. "
                + "Contexto persistido da solicitação:\n" + request.prompt();
    }

    /** Converte o JSON da OpenAI para a lista de opções esperada pelo backend. */
    private List<ExperimentPromiseOptionDto> parseOptions(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode optionsNode = root.path("options");
            List<ExperimentPromiseOptionDto> options = objectMapper.readerForListOf(ExperimentPromiseOptionDto.class)
                    .readValue(optionsNode);
            if (options.size() != 3) {
                throw new IllegalStateException("OpenAI retornou " + options.size() + " opções; esperado=3");
            }
            return options;
        } catch (Exception ex) {
            log.error("Falha ao interpretar JSON de opções de promessa; operation=experiment-promise-parse", ex);
            throw new IllegalStateException("Resposta da OpenAI inválida para opções de promessa", ex);
        }
    }
}
