package com.marketinghub.worker.adset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.audience.Audience;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ChatGPT client responsible for translating audience descriptions into
 * structured ad set plans.
 */
@Component
public class AudienceAdSetChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final AiGenerationRecorder generationRecorder;
    private static final Logger log = LoggerFactory.getLogger(AudienceAdSetChatGptClient.class);
    private static final Pattern NON_NUMERIC = Pattern.compile("[^0-9,.-]");
    private static final String DOMAIN = "ADSET_PLAN";

    public AudienceAdSetChatGptClient(WebClient.Builder builder,
                                      ObjectMapper objectMapper,
                                      @Value("${openai.api-key:}") String apiKey,
                                      @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                      @Value("${openai.model:gpt-3.5-turbo}") String model,
                                      AiGenerationRecorder generationRecorder) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.generationRecorder = generationRecorder;
    }

    public AdSetPlan planAdSet(Experiment experiment, Audience audience) {
        String prompt = buildPrompt(experiment, audience);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em mídia paga para Meta Ads."),
                OpenAiRequestUtils.message("user", prompt)
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);
        log.info("Sending ad set planning prompt for experiment {} audience {}", experiment.getId(), audience.getId());
        log.debug("Ad set planning payload: {}", payload);
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();
        if (response == null) {
            throw new IllegalStateException("ChatGPT returned no choices for ad set planning");
        }
        if (response.hasError()) {
            throw new RuntimeException("OpenAI error: " + response.errorMessage());
        }
        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                audience != null ? String.valueOf(audience.getId()) : null,
                prompt,
                content,
                model,
                response.usage());
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("ChatGPT returned empty content for ad set planning");
        }
        log.info("ChatGPT ad set content: {}", content);
        try {
            return parseContent(content, prompt);
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT ad set response: {}", content, e);
            String unescaped = content.replace("\\\"", "\"");
            try {
                return parseContent(unescaped, prompt);
            } catch (Exception ex) {
                log.error("Failed to parse unescaped ChatGPT ad set response: {}", unescaped, ex);
                throw new RuntimeException("Failed to parse ChatGPT ad set response", ex);
            }
        }
    }

    private AdSetPlan parseContent(String content, String prompt) throws Exception {
        JsonNode root = objectMapper.readTree(content);
        if (root.isArray() && root.size() > 0) {
            root = root.get(0);
        }
        if (root.has("adSet")) {
            root = root.get("adSet");
        }
        String location = asText(root, "location");
        List<String> interests = asStringList(root.get("interests"));
        List<String> lookalikes = asStringList(root.get("lookalikes"));
        BigDecimal budget = asBigDecimal(root.get("budget"));
        Integer durationDays = asInteger(root.get("durationDays"));
        String targetingJson = extractTargeting(root.get("targeting"), location, interests, lookalikes);
        return new AdSetPlan(location, interests, lookalikes, budget, durationDays, targetingJson, prompt, model);
    }

    private String extractTargeting(JsonNode targetingNode,
                                    String location,
                                    List<String> interests,
                                    List<String> lookalikes) throws JsonProcessingException {
        if (targetingNode != null && targetingNode.isObject()) {
            return objectMapper.writeValueAsString(targetingNode);
        }
        ObjectNode targeting = objectMapper.createObjectNode();
        ObjectNode geo = targeting.putObject("geo_locations");
        ArrayNode customLocations = geo.putArray("custom_locations");
        if (location != null && !location.isBlank()) {
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("name", location);
            customLocations.add(entry);
        }
        ArrayNode interestArray = targeting.putArray("interests");
        for (String interest : interests) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("name", interest);
            interestArray.add(item);
        }
        ArrayNode lookalikeArray = targeting.putArray("custom_audiences");
        for (String lookalike : lookalikes) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("name", lookalike);
            lookalikeArray.add(item);
        }
        String description = buildDescription(location, interests, lookalikes);
        if (description != null) {
            targeting.put("detailed_targeting_description", description);
        }
        return objectMapper.writeValueAsString(targeting);
    }

    private static String buildDescription(String location, List<String> interests, List<String> lookalikes) {
        StringBuilder sb = new StringBuilder();
        if (location != null && !location.isBlank()) {
            sb.append("Localização: ").append(location.trim());
        }
        if (!interests.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Interesses: ").append(String.join(", ", interests));
        }
        if (!lookalikes.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Públicos semelhantes: ").append(String.join(", ", lookalikes));
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static BigDecimal asBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText(null);
        if (text == null) {
            return null;
        }
        String normalized = NON_NUMERIC.matcher(text).replaceAll("");
        normalized = normalized.replace(',', '.');
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            log.warn("Unable to parse budget value: {}", text, e);
            return null;
        }
    }

    private static Integer asInteger(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isInt()) {
            return node.intValue();
        }
        if (node.isNumber()) {
            return node.numberValue().intValue();
        }
        String text = node.asText(null);
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("[^0-9-]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            log.warn("Unable to parse duration: {}", text, e);
            return null;
        }
    }

    private static List<String> asStringList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode element : node) {
                String value = extractString(element);
                if (value != null) {
                    values.add(value);
                }
            }
        } else {
            String raw = node.asText(null);
            if (raw != null) {
                for (String part : raw.split("[,;\\n]")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        values.add(trimmed);
                    }
                }
            }
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    unique.add(trimmed);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private static String extractString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.has("name")) {
            return node.get("name").asText();
        }
        return node.toString();
    }

    private static String asText(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String buildPrompt(Experiment experiment, Audience audience) {
        MarketNiche niche = experiment.getNiche();
        Hypothesis hypothesis = experiment.getHypothesisRef();
        StringBuilder sb = new StringBuilder();
        sb.append("Planeje um conjunto de anúncios para Meta Ads usando os dados abaixo. ");
        sb.append("Responda somente com um objeto JSON contendo as chaves ");
        sb.append("location, interests, lookalikes, budget, durationDays e targeting.\n");
        sb.append("Regras para o JSON:\n");
        sb.append("- interests e lookalikes devem ser arrays de strings.\n");
        sb.append("- budget deve ser um número decimal em reais (BRL), sem texto adicional.\n");
        sb.append("- durationDays deve ser um número inteiro (quantidade de dias).\n");
        sb.append("- targeting deve ser um objeto seguindo a estrutura padrão do Meta Ads, com as chaves:\n");
        sb.append("  geo_locations, age_min, age_max, genders, languages, interests, custom_audiences, ");
        sb.append("excluded_custom_audiences e detailed_targeting_description. Use arrays vazios quando não houver dados.\n");
        sb.append("Contexto do experimento:\n");
        appendField(sb, "Experimento", experiment.getName());
        if (hypothesis != null) {
            appendField(sb, "Hipótese", hypothesis.getTitle());
            appendField(sb, "Promessa", hypothesis.getPromise());
            appendField(sb, "Persona", hypothesis.getPersona());
            appendField(sb, "Mecanismo", hypothesis.getMechanism());
            appendField(sb, "Mecanismo único", hypothesis.getUniqueMechanism());
        }
        if (niche != null) {
            appendField(sb, "Nicho", niche.getName());
            appendField(sb, "Segmentação base", niche.getBaseSegmentation());
            appendField(sb, "Interesses do nicho", niche.getInterests());
            appendField(sb, "Filtros demográficos", niche.getDemographicFilters());
            appendField(sb, "Dicas extras", niche.getExtraTips());
        }
        sb.append("Público a ser segmentado:\n");
        appendField(sb, "Nome do público", audience.getName());
        appendField(sb, "Descrição do público", audience.getDescription());
        sb.append("Considere que a campanha usará dados do Brasil quando não houver indicação explícita.\n");
        sb.append("Retorne apenas o JSON solicitado.");
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value.trim()).append('\n');
        }
    }
}
