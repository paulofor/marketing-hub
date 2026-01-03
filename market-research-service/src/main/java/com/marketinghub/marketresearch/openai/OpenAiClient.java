package com.marketinghub.marketresearch.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final WebClient webClient;
    private final String model;

    public OpenAiClient(WebClient.Builder builder,
                        @Value("${openai.api-key:}") String apiKey,
                        @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                        @Value("${openai.model:o3}") String model) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured. Requests will fail until OPENAI_API_KEY is set.");
        }
        WebClient.Builder configured = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            configured.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = configured.build();
        this.model = model;
    }

    public String summarize(String query, String goal, Map<String, String> contexts) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Você é um analista de mercado com autonomia para resumir fontes públicas. ");
        prompt.append("Resuma as ideias principais e destaque oportunidades, riscos e próximos passos.\n");
        prompt.append("Hipótese de pesquisa: ").append(query).append("\n\n");
        if (goal != null && !goal.isBlank()) {
            prompt.append("Objetivo do relatório: ").append(goal).append("\n\n");
        }
        if (contexts != null && !contexts.isEmpty()) {
            contexts.forEach((source, text) -> {
                prompt.append("Fonte: ").append(source).append("\n");
                prompt.append(text).append("\n\n");
            });
        } else {
            prompt.append("Sem fontes externas enviadas. Gere um checklist do que pesquisar e hipóteses iniciais.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", List.of(
                OpenAiRequestUtils.message("system", "Produza respostas em português do Brasil."),
                OpenAiRequestUtils.message("user", prompt.toString())
        ));
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.info("Enviando prompt para o modelo {}", model);
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Resposta vazia da OpenAI");
        }
        if (response.hasError()) {
            throw new IllegalStateException("Erro da OpenAI: " + response.errorMessage());
        }
        String content = response.firstText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI retornou resposta vazia");
        }
        return content;
    }

    public String getModel() {
        return model;
    }
}
