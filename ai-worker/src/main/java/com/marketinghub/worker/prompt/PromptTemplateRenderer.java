package com.marketinghub.worker.prompt;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class PromptTemplateRenderer {
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
        try {
            Template template = new Template("prompt-template", new StringReader(templateSource), configuration);
            StringWriter writer = new StringWriter();
            template.process(context, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Failed to render prompt template", e);
        }
    }
}
