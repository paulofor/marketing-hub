package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptDomains;
import com.marketinghub.prompt.dto.PromptTemplateValidationRequest;
import com.marketinghub.prompt.dto.PromptTemplateValidationResponse;
import freemarker.core.InvalidReferenceException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PromptTemplateValidationService {
    private static final int AVAILABLE_VARIABLE_LIMIT = 120;
    private final Configuration configuration;

    public PromptTemplateValidationService() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_32);
        this.configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.configuration.setLogTemplateExceptions(false);
        this.configuration.setFallbackOnNullLoopVariable(false);
        this.configuration.setWrapUncheckedExceptions(true);
    }

    public PromptTemplateValidationResponse validate(PromptTemplateValidationRequest request) {
        if (request == null || request.getTemplate() == null || request.getTemplate().isBlank()) {
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("O template não pode estar vazio.")
                    .missingVariables(List.of())
                    .availableVariables(List.of())
                    .build();
        }

        Map<String, Object> context = buildContext(request.getDomain());
        List<String> availableVariables = availableVariables(context);

        if (context.isEmpty()) {
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("Domínio não suportado para validação.")
                    .missingVariables(List.of())
                    .availableVariables(availableVariables)
                    .build();
        }

        try {
            Template template = new Template("prompt-template-validation", new StringReader(request.getTemplate()), configuration);
            template.process(context, new StringWriter());
            return PromptTemplateValidationResponse.builder()
                    .valid(true)
                    .message("Template válido.")
                    .missingVariables(List.of())
                    .availableVariables(availableVariables)
                    .build();
        } catch (InvalidReferenceException invalidReferenceException) {
            String missing = sanitizeExpression(invalidReferenceException.getBlamedExpressionString());
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("Variável ausente ou inválida no template.")
                    .missingVariables(missing == null ? List.of() : List.of(missing))
                    .availableVariables(availableVariables)
                    .build();
        } catch (IOException | TemplateException exception) {
            return PromptTemplateValidationResponse.builder()
                    .valid(false)
                    .message("Erro de sintaxe no template: " + exception.getMessage())
                    .missingVariables(List.of())
                    .availableVariables(availableVariables)
                    .build();
        }
    }

    private Map<String, Object> buildContext(String domain) {
        if (PromptDomains.NICHE_DETAILED_DESCRIPTION.equals(domain)) {
            return buildNicheDetailedDescriptionContext();
        }
        if (PromptDomains.NICHE_HYPOTHESIS.equals(domain)) {
            return buildNicheHypothesisContext();
        }
        return Map.of();
    }

    private Map<String, Object> buildNicheDetailedDescriptionContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("quantity", 1);
        context.put("niche", buildNicheContext(false));
        return context;
    }

    private Map<String, Object> buildNicheHypothesisContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("quantity", 1);
        Map<String, Object> niche = buildNicheContext(true);
        context.put("niche", niche);
        context.put("detailedDescription", buildDetailedDescriptionContext());
        context.put("technology", buildTechnologyContext());
        context.put("attributes", List.of(buildAttributeContext()));
        context.put("attributeNames", List.of("title", "promise"));
        context.put("defaultAttributes", List.of("title", "promise"));
        return context;
    }

    private Map<String, Object> buildNicheContext(boolean includeHypothesisFields) {
        Map<String, Object> niche = new LinkedHashMap<>();
        niche.put("id", 1L);
        niche.put("name", "Exemplo");
        niche.put("description", "Descrição");
        niche.put("baseSegmentation", "Segmentação");
        niche.put("interests", "Interesses");
        niche.put("demographicFilters", "Filtros");
        niche.put("extraTips", "Dicas");
        niche.put("interestCategory", "Categoria");
        niche.put("roleCategory", "Papel");
        if (includeHypothesisFields) {
            List<Map<String, Object>> detailedDescriptions = List.of(buildDetailedDescriptionContext());
            niche.put("detailedDescriptions", detailedDescriptions);
            niche.put("latestDetailedDescription", buildDetailedDescriptionContext());
            niche.put("hypothesisDetailedDescription", buildDetailedDescriptionContext());
            niche.put("differentiatedTechnology", buildDifferentiatedTechnologyContext());
        }
        return niche;
    }

    private Map<String, Object> buildDetailedDescriptionContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", 1L);
        context.put("title", "Título");
        context.put("description", "Descrição detalhada");
        context.put("pains", "Dores");
        context.put("desires", "Desejos");
        context.put("needs", "Necessidades");
        context.put("model", "gpt-4o-mini");
        context.put("prompt", "Prompt");
        context.put("createdAt", Instant.now());
        context.put("updatedAt", Instant.now());
        return context;
    }

    private Map<String, Object> buildTechnologyContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", 1L);
        context.put("name", "Tecnologia");
        context.put("description", "Descrição");
        context.put("promptText", "Texto");
        context.put("createdAt", Instant.now());
        context.put("updatedAt", Instant.now());
        return context;
    }

    private Map<String, Object> buildDifferentiatedTechnologyContext() {
        Map<String, Object> context = buildTechnologyContext();
        context.put("createdAt", Instant.now());
        context.put("updatedAt", Instant.now());
        return context;
    }

    private Map<String, Object> buildAttributeContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", 1L);
        context.put("name", "title");
        context.put("description", "Descrição do campo");
        return context;
    }

    private List<String> availableVariables(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return List.of();
        }
        Set<String> vars = new LinkedHashSet<>();
        collectVariables("", context, vars, 3);
        if (vars.size() > AVAILABLE_VARIABLE_LIMIT) {
            return vars.stream().limit(AVAILABLE_VARIABLE_LIMIT).toList();
        }
        return new ArrayList<>(vars);
    }

    @SuppressWarnings("unchecked")
    private void collectVariables(String prefix, Object value, Set<String> acc, int depth) {
        if (depth < 0 || acc.size() >= AVAILABLE_VARIABLE_LIMIT || value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                String path = prefix.isEmpty() ? key : prefix + "." + key;
                acc.add(path);
                collectVariables(path, entry.getValue(), acc, depth - 1);
                if (acc.size() >= AVAILABLE_VARIABLE_LIMIT) {
                    return;
                }
            }
        } else if (value instanceof List<?> list && !list.isEmpty()) {
            String listPrefix = prefix.isEmpty() ? "[]" : prefix + "[]";
            acc.add(listPrefix);
            Object first = list.get(0);
            if (first instanceof Map<?, ?> firstMap) {
                for (Map.Entry<?, ?> entry : firstMap.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        continue;
                    }
                    String path = listPrefix + "." + key;
                    acc.add(path);
                    collectVariables(path, entry.getValue(), acc, depth - 1);
                    if (acc.size() >= AVAILABLE_VARIABLE_LIMIT) {
                        return;
                    }
                }
            }
        }
    }

    private String sanitizeExpression(String expression) {
        if (expression == null) {
            return null;
        }
        return expression.replace("${", "").replace("}", "").trim();
    }
}
