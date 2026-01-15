package com.marketinghub.worker.prompt;

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
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PromptTemplateRenderer {
    private static final Logger log = LoggerFactory.getLogger(PromptTemplateRenderer.class);
    private static final int PREVIEW_LIMIT = 400;
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
            log.error("Failed to render prompt template with keys {} | context preview {} | template preview {}", safeContext.keySet(), safeContextPreview(safeContext), preview(templateSource), e);
            throw new IllegalStateException("Failed to render prompt template", e);
        }
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
