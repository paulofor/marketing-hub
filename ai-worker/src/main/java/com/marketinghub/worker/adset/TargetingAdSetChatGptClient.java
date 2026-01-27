package com.marketinghub.worker.adset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.facebookads.dto.TargetingPackageDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.dto.TargetingElementDto;
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
 * ChatGPT client responsible for translating approved targeting elements into
 * structured ad set plans.
 */
@Component
public class TargetingAdSetChatGptClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final AiGenerationRecorder generationRecorder;
    private static final Logger log = LoggerFactory.getLogger(TargetingAdSetChatGptClient.class);
    private static final Pattern NON_NUMERIC = Pattern.compile("[^0-9,.-]");
    private static final String DOMAIN = "ADSET_PLAN";
    private static final String DEFAULT_LOCATION = "Brasil";

    public TargetingAdSetChatGptClient(WebClient.Builder builder,
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

    public AdSetPlan planAdSet(Experiment experiment, TargetingPackageDto targeting) {
        String prompt = buildPrompt(experiment, targeting);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em mídia paga para Meta Ads."),
                OpenAiRequestUtils.message("user", prompt)
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        OpenAiRequestUtils.maybeAddReasoning(payload, model);
        log.info("Sending ad set planning prompt for experiment {}", experiment.getId());
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
                experiment != null ? String.valueOf(experiment.getId()) : null,
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
        String location = normalizeLocation(asText(root, "location"));
        List<String> interests = asStringList(root.get("interests"));
        List<String> jobTitles = asStringList(root.get("jobTitles"));
        List<String> behaviors = asStringList(root.get("behaviors"));
        BigDecimal budget = asBigDecimal(root.get("budget"));
        Integer durationDays = asInteger(root.get("durationDays"));
        String targetingJson = extractTargeting(root.get("targeting"), location, interests, jobTitles, behaviors);
        return new AdSetPlan(location, interests, jobTitles, behaviors, budget, durationDays, targetingJson, prompt, model);
    }

    private static String normalizeLocation(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LOCATION;
        }
        return value.trim();
    }

    private String extractTargeting(JsonNode targetingNode,
                                    String location,
                                    List<String> interests,
                                    List<String> jobTitles,
                                    List<String> behaviors) throws JsonProcessingException {
        ObjectNode targeting = targetingNode != null && targetingNode.isObject()
                ? targetingNode.deepCopy()
                : objectMapper.createObjectNode();
        enforceBrazilGeo(targeting);
        mergeTargetingList(targeting, "interests", interests);
        mergeTargetingList(targeting, "work_positions", jobTitles);
        mergeTargetingList(targeting, "behaviors", behaviors);
        if (!targeting.has("detailed_targeting_description")) {
            String description = buildDescription(location, interests, jobTitles, behaviors);
            if (description != null) {
                targeting.put("detailed_targeting_description", description);
            }
        }
        sanitizeTargeting(targeting);
        return objectMapper.writeValueAsString(targeting);
    }

    private void enforceBrazilGeo(ObjectNode targeting) {
        ObjectNode geo = getOrCreateObject(targeting, "geo_locations");
        ArrayNode countries = geo.putArray("countries");
        countries.add("BR");
        geo.remove("custom_locations");
        geo.remove("regions");
        geo.remove("cities");
        geo.remove("zips");
        geo.remove("location_types");
    }

    private ObjectNode getOrCreateObject(ObjectNode parent, String fieldName) {
        JsonNode existing = parent.get(fieldName);
        if (existing instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return parent.putObject(fieldName);
    }

    private void mergeTargetingList(ObjectNode targeting, String fieldName, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        JsonNode existing = targeting.get(fieldName);
        if (existing instanceof ArrayNode arrayNode && arrayNode.size() > 0) {
            return;
        }
        ArrayNode array = targeting.putArray(fieldName);
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ObjectNode item = objectMapper.createObjectNode();
            item.put("name", trimmed);
            array.add(item);
        }
    }

    private void sanitizeTargeting(ObjectNode targeting) {
        JsonNode languages = targeting.remove("languages");
        if ((languages != null && !languages.isNull()) && !targeting.has("locales")) {
            ArrayNode locales = targeting.putArray("locales");
            if (languages.isArray()) {
                for (JsonNode language : languages) {
                    addLocaleValue(locales, language);
                }
            } else {
                addLocaleValue(locales, languages);
            }
        }
    }

    private static void addLocaleValue(ArrayNode locales, JsonNode value) {
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isNumber()) {
            locales.add(value.intValue());
            return;
        }
        String text = value.asText(null);
        if (text != null && !text.isBlank()) {
            locales.add(text.trim());
        }
    }

    private static String buildDescription(String location, List<String> interests, List<String> jobTitles, List<String> behaviors) {
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
        if (!jobTitles.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Cargos: ").append(String.join(", ", jobTitles));
        }
        if (!behaviors.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Comportamentos: ").append(String.join(", ", behaviors));
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

    private String buildPrompt(Experiment experiment, TargetingPackageDto targeting) {
        MarketNiche niche = experiment.getNiche();
        Hypothesis hypothesis = experiment.getHypothesisRef();
        StringBuilder sb = new StringBuilder();
        sb.append("Planeje um conjunto de anúncios para Meta Ads usando os dados abaixo. ");
        sb.append("Responda somente com um objeto JSON contendo as chaves ");
        sb.append("location, interests, jobTitles, behaviors, budget, durationDays e targeting.\n");
        sb.append("Regras para o JSON:\n");
        sb.append("- location deve ser sempre \"Brasil\".\n");
        sb.append("- interests, jobTitles e behaviors devem ser arrays de strings.\n");
        sb.append("- budget deve ser um número decimal em reais (BRL), sem texto adicional.\n");
        sb.append("- durationDays deve ser um número inteiro (quantidade de dias).\n");
        sb.append("- targeting deve ser um objeto seguindo a estrutura padrão do Meta Ads, com as chaves:\n");
        sb.append("  geo_locations, age_min, age_max, genders, locales, interests, work_positions, behaviors ");
        sb.append("e detailed_targeting_description. Use arrays vazios quando não houver dados.\n");
        sb.append("- Não inclua a chave languages. Caso seja necessário indicar idioma, utilize locales com os códigos aceitos pelo Meta Ads.\n");
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
        sb.append("Elementos de segmentação aprovados:\n");
        appendTargeting(sb, "Interesses", targeting != null ? targeting.getInterests() : List.of());
        appendTargeting(sb, "Cargos", targeting != null ? targeting.getJobTitles() : List.of());
        appendTargeting(sb, "Comportamentos", targeting != null ? targeting.getBehaviors() : List.of());
        sb.append("Considere que a campanha sempre terá localização no Brasil.\n");
        sb.append("Use apenas os termos aprovados acima para preencher interests, jobTitles (work_positions), behaviors e demais campos.\n");
        sb.append("Retorne apenas o JSON solicitado.");
        return sb.toString();
    }

    private static void appendTargeting(StringBuilder sb, String label, List<TargetingElementDto> elements) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        sb.append(label).append(": ");
        List<String> values = elements.stream()
                .map(TargetingAdSetChatGptClient::formatTargetingElement)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        sb.append(String.join("; ", values)).append('\n');
    }

    private static String formatTargetingElement(TargetingElementDto element) {
        if (element == null || element.getTerm() == null || element.getTerm().isBlank()) {
            return null;
        }
        String term = element.getTerm().trim();
        if (element.getDescription() == null || element.getDescription().isBlank()) {
            return term;
        }
        return term + " (" + element.getDescription().trim() + ")";
    }

    private static void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value.trim()).append('\n');
        }
    }
}
