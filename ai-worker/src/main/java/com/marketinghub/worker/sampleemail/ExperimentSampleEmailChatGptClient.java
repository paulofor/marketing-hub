package com.marketinghub.worker.sampleemail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente responsável por solicitar ao ChatGPT o copy dos e-mails de envio de amostras.
 */
@Component
public class ExperimentSampleEmailChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentSampleEmailChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final AiGenerationRecorder generationRecorder;

    public ExperimentSampleEmailChatGptClient(WebClient.Builder builder,
                                              ObjectMapper objectMapper,
                                              @Value("${openai.api-key:}") String apiKey,
                                              @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                              @Value("${openai.model:gpt-3.5-turbo}") String model,
                                              AiGenerationRecorder generationRecorder) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.generationRecorder = generationRecorder;
        this.enabled = StringUtils.hasText(apiKey);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(REQUEST_TIMEOUT)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler((int) REQUEST_TIMEOUT.getSeconds()))
                        .addHandlerLast(new WriteTimeoutHandler((int) REQUEST_TIMEOUT.getSeconds())));
        WebClient.Builder clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient));
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            if (OpenAiRequestUtils.requiresReasoning(model)) {
                clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
            }
        } else {
            log.warn("OpenAI API key não configurada; geração de e-mails de amostra ficará desativada");
        }
        this.webClient = clientBuilder.build();
    }

    public Generation generateSampleEmails(Experiment experiment, int quantity) {
        if (!enabled) {
            return Generation.disabled(model);
        }
        String prompt = buildPrompt(experiment, quantity);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um copywriter especialista em nutrição de leads."),
                OpenAiRequestUtils.message("user", prompt)
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.info("Enviando prompt de e-mails de amostra para experimento {}", experiment.getId());
        OpenAiResponse response;
        try {
            response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block(REQUEST_TIMEOUT);
        } catch (Exception ex) {
            log.error("Falha ao consultar OpenAI para e-mails de amostra do experimento {}", experiment.getId(), ex);
            throw new RuntimeException("Falha ao consultar ChatGPT para e-mails de amostra", ex);
        }
        if (response == null) {
            log.warn("OpenAI retornou resposta nula para experimento {}", experiment.getId());
            return Generation.empty(model, prompt, null);
        }
        if (response.hasError()) {
            throw new RuntimeException("Erro OpenAI: " + response.errorMessage());
        }
        String content = response.firstText();
        generationRecorder.record("EXPERIMENT_SAMPLE_EMAIL",
                experiment != null ? String.valueOf(experiment.getId()) : null,
                prompt,
                content,
                model,
                response.usage());
        log.info("Resposta OpenAI para e-mails de amostra: {}", content);
        List<SampleEmailPlan> plans = parseContent(content);
        return new Generation(plans, prompt, content, model);
    }

    private List<SampleEmailPlan> parseContent(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        try {
            SampleEmailPlan[] array = objectMapper.readValue(content, SampleEmailPlan[].class);
            List<SampleEmailPlan> result = new ArrayList<>();
            for (SampleEmailPlan plan : array) {
                if (plan != null && StringUtils.hasText(plan.subject()) && StringUtils.hasText(plan.body())) {
                    result.add(plan);
                }
            }
            return result;
        } catch (Exception ex) {
            log.error("Falha ao interpretar resposta de e-mails de amostra: {}", content, ex);
            try {
                String unescaped = content.replace("\\\"", "\"");
                SampleEmailPlan[] array = objectMapper.readValue(unescaped, SampleEmailPlan[].class);
                List<SampleEmailPlan> result = new ArrayList<>();
                for (SampleEmailPlan plan : array) {
                    if (plan != null && StringUtils.hasText(plan.subject()) && StringUtils.hasText(plan.body())) {
                        result.add(plan);
                    }
                }
                return result;
            } catch (Exception retry) {
                log.error("Falha ao interpretar resposta de e-mails de amostra após normalização: {}", content, retry);
                throw new RuntimeException("Não foi possível interpretar a resposta do ChatGPT para e-mails de amostra", retry);
            }
        }
    }

    private String buildPrompt(Experiment experiment, int quantity) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gere até ").append(quantity).append(" e-mails em português do Brasil no formato JSON.");
        sb.append(" Retorne somente um array JSON, sem texto adicional.");
        sb.append(" Cada objeto deve conter as chaves: \"subject\", \"previewText\", \"body\" (texto em Markdown ou texto puro com quebras de linha) e \"callToAction\" (frase curta).\n\n");
        sb.append("Esses e-mails serão enviados com um arquivo ZIP contendo as imagens do experimento com marca d'água.");
        sb.append(" Garanta que o corpo explique que o ZIP contém prévias com marca d'água, incentive o lead a avaliar as imagens e dê instruções claras de como adquirir o pacote completo com os arquivos originais sem marca d'água.\n\n");
        if (experiment != null) {
            if (StringUtils.hasText(experiment.getName())) {
                sb.append("Experimento: ").append(experiment.getName()).append("\n");
            }
            if (StringUtils.hasText(experiment.getHypothesis())) {
                sb.append("Resumo do experimento: ").append(experiment.getHypothesis()).append("\n");
            }
        }
        Hypothesis hypothesis = experiment != null ? experiment.getHypothesisRef() : null;
        if (hypothesis != null) {
            if (StringUtils.hasText(hypothesis.getTitle())) {
                sb.append("Hipótese: ").append(hypothesis.getTitle()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getPersona())) {
                sb.append("Persona: ").append(hypothesis.getPersona()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getProblem())) {
                sb.append("Problema: ").append(hypothesis.getProblem()).append("\n");
            }
            if (StringUtils.hasText(hypothesis.getPromise())) {
                sb.append("Promessa: ").append(hypothesis.getPromise()).append("\n");
            }
        }
        sb.append("\nInclua gatilhos de urgência suave, destaque o valor das imagens originais e reforce que basta responder ao e-mail ou clicar no CTA para finalizar a compra.\n");
        sb.append("Formato de resposta esperado: JSON com os objetos solicitados.");
        return sb.toString();
    }

    public record SampleEmailPlan(String subject, String previewText, String body, String callToAction) {
    }

    public record Generation(List<SampleEmailPlan> plans, String prompt, String rawResponse, String model) {
        public Generation {
            plans = plans != null ? List.copyOf(plans) : List.of();
        }

        public static Generation disabled(String model) {
            return new Generation(List.of(), "", null, model);
        }

        public static Generation empty(String model, String prompt, String rawResponse) {
            return new Generation(List.of(), prompt, rawResponse, model);
        }

        public String auditTrail() {
            StringBuilder sb = new StringBuilder();
            if (prompt != null && !prompt.isBlank()) {
                sb.append("PROMPT:\n").append(prompt).append("\n\n");
            }
            if (rawResponse != null && !rawResponse.isBlank()) {
                sb.append("RAW_RESPONSE:\n").append(rawResponse);
            }
            return sb.toString();
        }
    }
}
