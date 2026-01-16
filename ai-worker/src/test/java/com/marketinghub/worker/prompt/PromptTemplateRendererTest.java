package com.marketinghub.worker.prompt;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PromptTemplateRendererTest {

    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer();

    @Test
    void exposesMissingVariableAndAvailableOptions() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("niche", Map.of("name", "Escolas"));
        context.put("technology", Map.of("name", "CRM"));

        PromptTemplateException ex = catchThrowableOfType(
                () -> renderer.render("Teste ${missingVar} com ${technology.name}", context),
                PromptTemplateException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getMissingVariables()).contains("missingVar");
        assertThat(ex.getAvailableVariables()).anyMatch(v -> v.startsWith("technology"));
        assertThat(ex.getMessage()).contains("missingVar");
    }
}
