package com.marketinghub.worker.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.learning.dto.ExperimentLearningPayloadDto;
import com.marketinghub.experiment.learning.dto.LearningInsightDto;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.worker.learning.exception.BackendClientException;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Cliente responsável por transformar o snapshot do experimento em aprendizados estruturados via OpenAI.
 */
@Component
public class ExperimentLearningChatGptClient {

    private static final Logger log = LoggerFactory.getLogger(ExperimentLearningChatGptClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final int MAX_CREATIVES = 4;
    private static final int MAX_FUNNEL_STAGES = 6;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiGenerationRecorder generationRecorder;
    private final boolean enabled;
    private final String model;
    private final Double temperature;

    public ExperimentLearningChatGptClient(WebClient.Builder builder,
                                           ObjectMapper objectMapper,
                                           AiGenerationRecorder generationRecorder,
                                           ExperimentLearningProperties properties,
                                           @Value("${openai.api-key:}") String apiKey,
                                           @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.generationRecorder = generationRecorder;
        this.model = properties.getModel();
        this.temperature = properties.getTemperature();
        this.enabled = StringUtils.hasText(apiKey) && StringUtils.hasText(this.model);
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
            if (OpenAiRequestUtils.requiresReasoning(this.model)) {
                clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
            }
        } else {
            log.warn("OpenAI não configurado — geração de aprendizados ficará indisponível");
        }
        this.webClient = clientBuilder.build();
    }

    public GenerationResult generateLearning(Long requestId, ExperimentReportMaterialDto material) {
        if (!enabled) {
            throw new IllegalStateException("Geração de aprendizados desabilitada (OpenAI não configurado)");
        }
        String prompt = buildPrompt(material);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        if (temperature != null) {
            payload.put("temperature", temperature);
        }
        payload.put("input", List.of(
                OpenAiRequestUtils.message("system", systemPrompt()),
                OpenAiRequestUtils.message("user", prompt)
        ));
        OpenAiRequestUtils.maybeAddReasoning(payload, model);
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block(REQUEST_TIMEOUT);
        if (response == null) {
            throw new BackendClientException("OpenAI retornou resposta vazia para aprendizado do experimento");
        }
        if (response.hasError()) {
            throw new BackendClientException("OpenAI falhou: " + response.errorMessage());
        }
        String content = response.firstText();
        if (!StringUtils.hasText(content)) {
            throw new BackendClientException("OpenAI não retornou texto com o JSON esperado");
        }
        ExperimentLearningPayloadDto dto = parsePayload(content);
        dto.setOpenAiRequestPayload(payload);
        generationRecorder.record("EXPERIMENT_LEARNING",
                requestId != null ? String.valueOf(requestId) : null,
                prompt,
                content,
                model,
                response.usage());
        return new GenerationResult(dto, prompt, content, model);
    }

    private ExperimentLearningPayloadDto parsePayload(String content) {
        try {
            return objectMapper.readValue(content, ExperimentLearningPayloadDto.class);
        } catch (JsonProcessingException ex) {
            log.error("Falha ao interpretar payload de aprendizado: {}", content, ex);
            throw new BackendClientException("Resposta do OpenAI fora do formato JSON combinado");
        }
    }

    private String systemPrompt() {
        return "Você é um estrategista de growth que sempre estrutura aprendizados usando o framework Dor → Resultado → Mecanismo → Prova → Oferta." +
                " Fale como consultor sênior, priorizando ações práticas e baseadas em dados.";
    }

    private String buildPrompt(ExperimentReportMaterialDto material) {
        StringBuilder sb = new StringBuilder();
        var experiment = material.getExperiment();
        var niche = material.getNiche();
        var hypothesis = material.getHypothesis();
        sb.append("Contexto do experimento: \n");
        if (niche != null) {
            sb.append("- Nicho: ").append(niche.getName());
            if (StringUtils.hasText(niche.getDescription())) {
                sb.append(" — ").append(niche.getDescription());
            }
            sb.append('\n');
        }
        if (hypothesis != null) {
            appendIfPresent(sb, "- Hipótese", hypothesis.getTitle());
            appendIfPresent(sb, "- Dor principal", hypothesis.getProblem());
            appendIfPresent(sb, "- Resultado desejado", hypothesis.getPromise());
            appendIfPresent(sb, "- Mecanismo/Oferta", hypothesis.getMechanism());
        }
        if (experiment != null) {
            appendIfPresent(sb, "- Estágio atual do funil", experiment.getStage());
            appendIfPresent(sb, "- Variável primária testada", experiment.getPrimaryVariable());
            appendIfPresent(sb, "- Métrica primária", experiment.getPrimaryMetric());
            appendIfPresent(sb, "- KPI de CPL", formatDecimal(experiment.getKpiTargetCpl()));
            appendIfPresent(sb, "- Período", formatPeriod(experiment.getStartDate(), experiment.getEndDate()));
        }
        if (material.getCampaignMetric() != null) {
            sb.append("\nMétricas consolidadas da campanha:\n");
            var metric = material.getCampaignMetric();
            appendIfPresent(sb, "- Impressões", safeNumber(metric.getImpressions()));
            appendIfPresent(sb, "- Cliques", safeNumber(metric.getClicks()));
            appendIfPresent(sb, "- Leads", safeNumber(metric.getLeads()));
            appendIfPresent(sb, "- Gasto", formatDecimal(metric.getSpend()));
            appendIfPresent(sb, "- CPC", formatDecimal(metric.getCpc()));
            appendIfPresent(sb, "- CPL", formatDecimal(metric.getCpl()));
        }
        if (!CollectionUtils.isEmpty(material.getFunnelStages())) {
            sb.append("\nFunil monitorado:\n");
            material.getFunnelStages().stream()
                    .limit(MAX_FUNNEL_STAGES)
                    .forEach(stage -> {
                        String stageName = stage.getLabel();
                        if (!StringUtils.hasText(stageName) && stage.getStage() != null) {
                            stageName = stage.getStage().name();
                        }
                        long total = stage.getTotalCount();
                        Long uniques = stage.getUniqueCount();
                        sb.append(String.format("- %s: %d eventos (auto=%d, manual=%d, únicos=%s)\n",
                                stageName != null ? stageName : "Etapa",
                                total,
                                stage.getAutoCount(),
                                stage.getManualCount(),
                                uniques != null ? uniques : "-"));
                    });
        }
        if (!CollectionUtils.isEmpty(material.getCreatives())) {
            sb.append("\nCriativos mais relevantes:\n");
            material.getCreatives().stream()
                    .limit(MAX_CREATIVES)
                    .forEach(creative -> {
                        String angles = creative.getAngles() != null && !creative.getAngles().isEmpty()
                                ? String.join(", ", creative.getAngles())
                                : null;
                        sb.append("- ")
                                .append(truncate(creative.getHeadline(), 120));
                        if (StringUtils.hasText(angles)) {
                            sb.append(" | Ângulos: ").append(truncate(angles, 120));
                        }
                        sb.append('\n');
                    });
        }
        sb.append("\nRegras para a resposta:\n")
                .append("1. Responda apenas com um JSON (sem texto fora dele).\n")
                .append("2. Utilize o formato:\n")
                .append("{\n")
                .append("  \"summary\": string,\n")
                .append("  \"whatWorked\": string,\n")
                .append("  \"whatBlocked\": string,\n")
                .append("  \"nextTest\": string,\n")
                .append("  \"primaryMetric\": string,\n")
                .append("  \"metricSignal\": string,\n")
                .append("  \"stage\": one of [AD, LANDING, SAMPLE, SALES],\n")
                .append("  \"insights\": [ { \"type\": one of [PAIN, RESULT, MECHANISM, PROOF, OFFER], \"statement\": string, \"evidence\": string, \"confidence\": string, \"stage\": stage, \"primaryMetric\": string } ],\n")
                .append("  \"suggestions\": [ { \"title\": string, \"rationale\": string, \"stage\": stage, \"primaryMetric\": string, \"priority\": one of [HIGH, MEDIUM, LOW] } ]\n")
                .append("}\n")
                .append("3. Limite insights a no máximo 5 entradas e sugestões a no máximo 3 entradas.\n")
                .append("4. Use o framework Dor → Resultado → Mecanismo → Prova → Oferta para classificar os insights.\n")
                .append("5. Cite sempre as evidências numéricas (CTR, CPC, CPL, taxas de conversão) nos campos metricSignal/evidence.\n");
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value == null) {
            return;
        }
        String text;
        if (value instanceof String str) {
            text = str;
        } else {
            text = String.valueOf(value);
        }
        if (!StringUtils.hasText(text)) {
            return;
        }
        sb.append(label).append(": ").append(text).append('\n');
    }

    private String truncate(String value, int limit) {
        if (!StringUtils.hasText(value) || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit - 3) + "...";
    }

    private String safeNumber(Number number) {
        if (number == null) {
            return null;
        }
        return String.valueOf(number);
    }

    private String formatDecimal(Number number) {
        if (number == null) {
            return null;
        }
        return String.format("%.2f", number.doubleValue());
    }

    private String formatPeriod(java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null && end == null) {
            return null;
        }
        if (start != null && end != null) {
            return start + " até " + end;
        }
        return start != null ? "desde " + start : "até " + end;
    }

    public record GenerationResult(ExperimentLearningPayloadDto payload,
                                   String prompt,
                                   String rawResponse,
                                   String model) {
    }
}
