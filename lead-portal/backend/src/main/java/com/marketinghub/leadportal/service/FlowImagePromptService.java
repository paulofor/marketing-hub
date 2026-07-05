package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowImagePrompt;
import com.marketinghub.leadportal.model.FlowSubmission;
import com.marketinghub.leadportal.model.SimpleImageBriefing;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Monta prompts de imagem do Lead Portal a partir do fluxo, respostas do formulário e imagem enviada.
 */
@Service
public class FlowImagePromptService {

    private static final int DEFAULT_BATCH_SIZE = 6;
    private static final String DEFAULT_IMAGE_MODEL = "gpt-image-1";
    private static final int DEFAULT_REFERENCE_IMAGE_FREE_IMAGES = 1;
    private static final String DEFAULT_TEMPLATE = String.join("\n",
            "Gere materiais de divulgação premium em português para {{profissional}}, um(a) {{atividade}} que atua em {{local}}.",
            "Requisitos obrigatórios:",
            "1. Visual bonito, atraente e com atmosfera profissional, destacando o universo de {{atividade}}.",
            "2. Valorize os serviços principais ({{servicos}}) com chamadas claras, pensadas para redes sociais.",
            "3. Mostre formas de contato visíveis adicionando {{contato}} no design.",
            "4. Use cores vivas, iluminação moderna e elementos que façam referência ao ambiente de estúdio ou atendimento personalizado.",
            "5. Entregue um pacote em lote (batch) com pelo menos {{batch_size}} variações quadradas (1:1), prontas para feed e fáceis de adaptar para stories.",
            "",
            "Dados coletados no formulário. Use-os para definir copy, cenário, elementos visuais e público-alvo:",
            "{{dados_json}}",
            "");
    private static final String DEFAULT_REFERENCE_IMAGE_TEMPLATE = String.join("\n",
            "Gere uma amostra visual personalizada premium em português usando a imagem enviada pelo lead como referência principal.",
            "Preserve a estrutura, proporções e elementos importantes da foto original. Aplique somente as melhorias solicitadas nas respostas do formulário.",
            "A saída deve funcionar como prévia gratuita de alto valor, mostrando transformação realista e desejável sem prometer reforma completa.",
            "",
            "Dados coletados no formulário:",
            "{{dados_json}}",
            "");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\}");

    private final SimpleImageBriefingMapper briefingMapper;
    private final ObjectMapper objectMapper;

    public FlowImagePromptService(SimpleImageBriefingMapper briefingMapper, ObjectMapper objectMapper) {
        this.briefingMapper = briefingMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Cria o prompt de geração de imagem quando o fluxo possui briefing suficiente para o worker.
     */
    public Optional<FlowImagePrompt> buildPrompt(Flow flow, FlowSubmission submission) {
        if (hasReferenceImage(submission)) {
            return buildReferenceImagePrompt(flow, submission);
        }

        Optional<SimpleImageBriefing> simpleBriefing = briefingMapper.map(flow.slug(), submission);
        if (simpleBriefing.isPresent()) {
            return simpleBriefing.map(briefing -> buildSimpleFormPrompt(flow, briefing));
        }

        if (!StringUtils.hasText(flow.prompt()) && !StringUtils.hasText(flow.model())) {
            return Optional.empty();
        }

        return Optional.of(new FlowImagePrompt(
                Optional.ofNullable(flow.prompt()).orElse(""),
                flow.model(),
                null,
                null));
    }

    /**
     * Monta prompt para fluxos em que a foto enviada deve guiar a amostra personalizada.
     */
    private Optional<FlowImagePrompt> buildReferenceImagePrompt(Flow flow, FlowSubmission submission) {
        if (!hasReferenceImageConfiguration(flow)) {
            return Optional.empty();
        }

        int batchSize = resolveBatchSize(flow.imageBatchSize());
        String template = firstText(flow.imagePromptTemplate(), flow.prompt(), DEFAULT_REFERENCE_IMAGE_TEMPLATE);
        String prompt = renderTemplate(template, submission, batchSize);
        if (!StringUtils.hasText(prompt)) {
            prompt = renderTemplate(DEFAULT_REFERENCE_IMAGE_TEMPLATE, submission, batchSize);
        }
        prompt = appendReferenceImageInstructions(prompt, submission);
        String model = firstText(flow.imagePromptModel(), flow.model(), DEFAULT_IMAGE_MODEL);
        return Optional.of(new FlowImagePrompt(prompt, model, batchSize, DEFAULT_REFERENCE_IMAGE_FREE_IMAGES));
    }

    private FlowImagePrompt buildSimpleFormPrompt(Flow flow, SimpleImageBriefing briefing) {
        int batchSize = resolveBatchSize(flow.imageBatchSize());
        String template = StringUtils.hasText(flow.imagePromptTemplate()) ? flow.imagePromptTemplate() : DEFAULT_TEMPLATE;
        String prompt = renderTemplate(template, briefing, batchSize);
        if (!StringUtils.hasText(prompt)) {
            prompt = renderTemplate(DEFAULT_TEMPLATE, briefing, batchSize);
        }
        if (!StringUtils.hasText(prompt)) {
            prompt = buildFallbackPrompt(briefing, batchSize);
        }
        String model = StringUtils.hasText(flow.imagePromptModel()) ? flow.imagePromptModel() : DEFAULT_IMAGE_MODEL;
        return new FlowImagePrompt(prompt, model, batchSize, 0);
    }

    private boolean hasReferenceImage(FlowSubmission submission) {
        if (submission == null) {
            return false;
        }
        return StringUtils.hasText(submission.storedFileName())
                || StringUtils.hasText(submission.originalFileName());
    }

    private boolean hasReferenceImageConfiguration(Flow flow) {
        if (flow == null) {
            return false;
        }
        return StringUtils.hasText(flow.imagePromptTemplate())
                || StringUtils.hasText(flow.prompt())
                || StringUtils.hasText(flow.imagePromptModel())
                || StringUtils.hasText(flow.model())
                || flow.imageBatchSize() != null;
    }

    private String serializeBriefing(SimpleImageBriefing briefing) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("atividade", briefing.activityType());
        payload.put("profissional", briefing.professionalName());
        payload.put("studio", briefing.studioName());
        payload.put("local", briefing.resolvedLocation());
        payload.put("contato", briefing.contactSummary());
        payload.put("servicos", briefing.resolvedServices());
        payload.put("respostas", briefing.answers());
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return payload.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n"));
        }
    }

    private int resolveBatchSize(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_BATCH_SIZE;
        }
        return Math.min(requested, 20);
    }

    private String renderTemplate(String template, SimpleImageBriefing briefing, int batchSize) {
        if (!StringUtils.hasText(template)) {
            return null;
        }
        Map<String, String> variables = buildTemplateVariables(briefing, batchSize);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.getOrDefault(key, "");
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    private String renderTemplate(String template, FlowSubmission submission, int batchSize) {
        if (!StringUtils.hasText(template)) {
            return null;
        }
        Map<String, String> variables = buildSubmissionTemplateVariables(submission, batchSize);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.getOrDefault(key, "");
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString().trim();
    }

    private Map<String, String> buildTemplateVariables(SimpleImageBriefing briefing, int batchSize) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("slug", safeOrDefault(briefing.flowSlug(), ""));
        variables.put("atividade", safeOrDefault(briefing.activityType(), "profissional"));
        variables.put("profissional", safeOrDefault(briefing.professionalName(), "Profissional"));
        variables.put("nome", safeOrDefault(briefing.professionalName(), "Profissional"));
        variables.put("studio", safeOrDefault(briefing.studioName(), "estúdio ou atendimento personalizado"));
        variables.put("local", safeOrDefault(briefing.resolvedLocation(), "sua região"));
        variables.put("contato", safeOrDefault(briefing.contactSummary(), "Contato não informado"));
        variables.put("email", safeOrDefault(briefing.email(), ""));
        variables.put("servicos", joinList(briefing.resolvedServices(), ", "));
        variables.put("servicos_lista", joinList(briefing.resolvedServices(), "\n"));
        variables.put("dados_json", serializeBriefing(briefing));
        variables.put("batch_size", Integer.toString(batchSize));
        addAnswerAliases(briefing.answers(), variables);
        flattenAnswers("respostas", briefing.answers(), variables);
        flattenAnswers("answers", briefing.answers(), variables);
        return variables;
    }

    private Map<String, String> buildSubmissionTemplateVariables(FlowSubmission submission, int batchSize) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("slug", safeOrDefault(submission.flowSlug(), ""));
        variables.put("nome", safeOrDefault(submission.name(), ""));
        variables.put("name", safeOrDefault(submission.name(), ""));
        variables.put("email", safeOrDefault(submission.email(), ""));
        variables.put("image_key", safeOrDefault(submission.imageQuestionKey(), ""));
        variables.put("stored_file_name", safeOrDefault(submission.storedFileName(), ""));
        variables.put("original_file_name", safeOrDefault(submission.originalFileName(), ""));
        variables.put("content_type", safeOrDefault(submission.contentType(), ""));
        variables.put("batch_size", Integer.toString(batchSize));
        variables.put("dados_json", serializeSubmission(submission));
        addAnswerAliases(submission.answers(), variables);
        flattenAnswers("respostas", submission.answers(), variables);
        flattenAnswers("answers", submission.answers(), variables);
        return variables;
    }

    private String serializeSubmission(FlowSubmission submission) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nome", submission.name());
        payload.put("email", submission.email());
        payload.put("imageQuestionKey", submission.imageQuestionKey());
        payload.put("originalFileName", submission.originalFileName());
        payload.put("respostas", submission.answers());
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return payload.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n"));
        }
    }

    private String appendReferenceImageInstructions(String prompt, FlowSubmission submission) {
        String basePrompt = StringUtils.hasText(prompt) ? prompt.trim() : DEFAULT_REFERENCE_IMAGE_TEMPLATE;
        return String.join("\n",
                basePrompt,
                "",
                "Instrução obrigatória sobre a imagem de referência:",
                "- Use a foto enviada pelo lead como referência visual principal.",
                "- Não ignore a imagem original; preserve composição, perspectiva e elementos estruturais relevantes.",
                "- Gere uma amostra personalizada coerente com as respostas do formulário.",
                "- Arquivo original recebido: " + safeOrDefault(submission.originalFileName(), "imagem enviada pelo lead") + ".");
    }

    private void addAnswerAliases(Map<String, Object> answers, Map<String, String> target) {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        answers.forEach((key, value) -> addAliasValue(key, value, target));
    }

    private void addAliasValue(String key, Object value, Map<String, String> target) {
        if (!StringUtils.hasText(key) || value == null || target.containsKey(key)) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.forEach((nestedKey, nestedValue) -> {
                if (nestedKey == null) {
                    return;
                }
                String nestedPath = key + "." + nestedKey;
                addAliasValue(nestedPath, nestedValue, target);
            });
            return;
        }
        if (value instanceof List<?> list) {
            String joined = joinList(list, ", ");
            if (StringUtils.hasText(joined)) {
                target.put(key, joined);
            }
            return;
        }
        String textValue = stringifyValue(value);
        if (StringUtils.hasText(textValue)) {
            target.put(key, textValue);
        }
    }

    private void flattenAnswers(String prefix, Map<String, Object> answers, Map<String, String> target) {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        answers.forEach((key, value) -> addFlattenedValue(prefix + "." + key, value, target));
    }

    private void addFlattenedValue(String path, Object value, Map<String, String> target) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            map.forEach((k, v) -> nested.put(String.valueOf(k), v));
            nested.forEach((k, v) -> addFlattenedValue(path + "." + k, v, target));
            return;
        }
        if (value instanceof List<?> list) {
            String joined = joinList(list, ", ");
            if (StringUtils.hasText(joined)) {
                target.put(path, joined);
            }
            return;
        }
        String textValue = stringifyValue(value);
        if (StringUtils.hasText(textValue)) {
            target.put(path, textValue);
        }
    }

    private String stringifyValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }

    private String joinList(List<?> values, String separator) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(this::stringifyValue)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(separator));
    }

    private String safeOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String firstText(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private String buildFallbackPrompt(SimpleImageBriefing briefing, int batchSize) {
        String services = joinList(briefing.resolvedServices(), ", ");
        String location = safeOrDefault(briefing.resolvedLocation(), "sua região");
        String contact = safeOrDefault(briefing.contactSummary(), "Contato não informado");
        String professional = safeOrDefault(briefing.professionalName(), "Profissional");
        String activityType = safeOrDefault(briefing.activityType(), "profissional");
        String studio = safeOrDefault(briefing.studioName(), "estúdio ou atendimento personalizado");
        String dataBlock = serializeBriefing(briefing);

        String fallbackTemplate = String.join("\n",
                "Gere materiais de divulgação premium em português para %s, um(a) %s que atua em %s.",
                "Requisitos obrigatórios:",
                "1. Visual bonito, atraente e com atmosfera profissional, destacando o universo de %s.",
                "2. Valorize os serviços principais (%s) com chamadas claras, pensadas para redes sociais.",
                "3. Mostre formas de contato visíveis adicionando %s no design.",
                "4. Use cores vivas, iluminação moderna e elementos que façam referência ao ambiente de %s.",
                "5. Entregue um pacote em lote (batch) com pelo menos %d variações quadradas (1:1), prontas para feed e fáceis de adaptar para stories.",
                "",
                "Dados coletados no formulário. Use-os para definir copy, cenário, elementos visuais e público-alvo:",
                "%s",
                "");

        return fallbackTemplate.formatted(
                professional,
                activityType,
                location,
                activityType,
                services,
                contact,
                studio,
                batchSize,
                dataBlock);
    }
}
