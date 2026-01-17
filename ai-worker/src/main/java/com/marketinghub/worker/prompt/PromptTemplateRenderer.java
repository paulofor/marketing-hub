package com.marketinghub.worker.prompt;

import freemarker.core.InvalidReferenceException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PromptTemplateRenderer {
    private static final Logger log = LoggerFactory.getLogger(PromptTemplateRenderer.class);
    private static final int PREVIEW_LIMIT = 400;
    private static final int AVAILABLE_VARIABLE_LIMIT = 120;
    private final Configuration configuration;

    public PromptTemplateRenderer() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_32);
        this.configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.configuration.setLogTemplateExceptions(false);
        this.configuration.setFallbackOnNullLoopVariable(false);
        this.configuration.setWrapUncheckedExceptions(true);
    }

    public String render(String templateSource, Map<String, Object> context) {
        if (templateSource == null || templateSource.isBlank()) {
            throw new IllegalArgumentException("Prompt template is blank");
        }
        Map<String, Object> safeContext = context != null ? context : Map.of();
        log.info("Rendering prompt template ({} chars) with keys {} and context preview {}", templateSource.length(), safeContext.keySet(), safeContextPreview(safeContext));
        try {
            Template template = new Template("prompt-template", new StringReader(templateSource), configuration);
            StringWriter writer = new StringWriter();
            template.process(safeContext, writer);
            String rendered = writer.toString();
            log.info("Rendered prompt template successfully ({} chars). Preview: {}", rendered.length(), preview(rendered));
            return rendered;
        } catch (IOException | TemplateException e) {
            PromptTemplateException friendly = toPromptTemplateException(e, templateSource, safeContext);
            log.error("Failed to render prompt template with keys {} | context preview {} | focused context {} | template preview {} | error: {}",
                    safeContext.keySet(),
                    safeContextPreview(safeContext),
                    focusedContextPreview(safeContext, List.of("technology", "detailedDescription")),
                    preview(templateSource),
                    friendly.getMessage(),
                    e);
            throw friendly;
        }
    }

    private PromptTemplateException toPromptTemplateException(Exception error,
                                                              String templateSource,
                                                              Map<String, Object> safeContext) {
        List<String> availableVariables = availableVariables(safeContext);
        String templatePreview = preview(templateSource);
        if (error instanceof InvalidReferenceException invalid) {
            String missing = sanitizeExpression(invalid.getBlamedExpressionString());
            String message = "Variável ausente ou inválida no prompt: " + missing
                    + ". Variáveis disponíveis: " + availableVariables;
            return new PromptTemplateException(message, error,
                    missing != null ? List.of(missing) : List.of(),
                    availableVariables,
                    templatePreview);
        }
        if (error instanceof TemplateException) {
            String message = "Falha ao processar o template do prompt: " + error.getMessage();
            return new PromptTemplateException(message, error, List.of(), availableVariables, templatePreview);
        }
        return new PromptTemplateException("Falha ao renderizar o template do prompt", error, List.of(), availableVariables, templatePreview);
    }

    private Map<String, Object> safeContextPreview(Map<String, Object> context) {
        Map<String, Object> preview = new LinkedHashMap<>();
        if (context == null) {
            return preview;
        }
        context.forEach((key, value) -> preview.put(key, previewValue(value)));
        return preview;
    }

    private Object previewValue(Object value) {
        if (value instanceof String str) {
            return preview(str);
        }
        return value;
    }

    private Map<String, Object> focusedContextPreview(Map<String, Object> context, List<String> keys) {
        Map<String, Object> preview = new LinkedHashMap<>();
        if (context == null || keys == null) {
            return preview;
        }
        for (String key : keys) {
            Object value = context.get(key);
            preview.put(key, previewValueDeep(value, 2));
        }
        return preview;
    }

    private Object previewValueDeep(Object value, int depth) {
        if (depth < 0 || value == null) {
            return value;
        }
        if (value instanceof String str) {
            return preview(str);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    nested.put(key, previewValueDeep(entry.getValue(), depth - 1));
                }
            }
            return nested;
        }
        if (value instanceof List<?> list) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("size", list.size());
            if (!list.isEmpty()) {
                summary.put("first", previewValueDeep(list.get(0), depth - 1));
            }
            return summary;
        }
        return value;
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

    private String preview(String text) {
        if (text == null) {
            return "null";
        }
        String normalized = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (normalized.length() > PREVIEW_LIMIT) {
            return normalized.substring(0, PREVIEW_LIMIT) + "...";
        }
        return normalized;
    }
}
