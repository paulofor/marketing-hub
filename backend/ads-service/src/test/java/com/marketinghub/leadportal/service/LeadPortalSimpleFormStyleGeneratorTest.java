package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.service.LeadPortalSimpleFormStyleGenerator.GenerationCommand;
import com.marketinghub.openai.OpenAiBatchClient;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.OpenAiResponse.OpenAiUsage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadPortalSimpleFormStyleGeneratorTest {

    @Mock
    private OpenAiBatchClient batchClient;

    @Captor
    private ArgumentCaptor<Map<String, Object>> bodyCaptor;

    private LeadPortalSimpleFormStyleGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new LeadPortalSimpleFormStyleGenerator(batchClient, new ObjectMapper());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildRequestUsesTextJsonSchemaFormat() {
        String json = """
                {
                  "backgroundColor": "#ffffff",
                  "backgroundGradient": "linear-gradient(90deg,#111,#222)",
                  "backgroundPatternUrl": "",
                  "cardBackground": "#0f0f0f",
                  "cardBorderColor": "#222222",
                  "cardShadow": "0 8px 24px rgba(0,0,0,0.45)",
                  "headingColor": "#f5f5f5",
                  "textColor": "#dddddd",
                  "mutedTextColor": "#888888",
                  "primaryColor": "#ff6600",
                  "accentColor": "#ffd700",
                  "buttonBackground": "#ff6600",
                  "buttonTextColor": "#111111",
                  "buttonShadow": "0 4px 12px rgba(255,102,0,0.45)",
                  "buttonBorderRadius": "32px",
                  "highlightBackground": "rgba(255,255,255,0.08)",
                  "inputBackground": "#1a1a1a",
                  "inputBorderColor": "#333333",
                  "heroLayout": "image-right",
                  "heroImageUrl": "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee",
                  "heroImageBlendColor": "#111111"
                }
                """;
        OpenAiUsage usage = new OpenAiUsage(10, 5, null, null, 15);
        OpenAiResponse response = new OpenAiResponse("resp_123", json, null, usage, null, "completed");
        when(batchClient.executeSingle(bodyCaptor.capture(), anyString())).thenReturn(response);

        GenerationCommand command = new GenerationCommand(
                "gpt-4o-mini",
                "Estilo premium e leve",
                "Hero Gradient",
                "Fluxo para personal trainer"
        );

        generator.generate(command);

        Map<String, Object> body = bodyCaptor.getValue();
        assertThat(body).containsKey("text");
        assertThat(body).doesNotContainKey("response_format");

        Map<String, Object> textConfig = (Map<String, Object>) body.get("text");
        Map<String, Object> format = (Map<String, Object>) textConfig.get("format");
        assertThat(format.get("type")).isEqualTo("json_schema");
        assertThat(format.get("name")).isEqualTo("lead_portal_simple_form_style");
        assertThat(format.get("schema")).isInstanceOf(Map.class);

        Map<String, Object> jsonSchema = (Map<String, Object>) format.get("json_schema");
        assertThat(jsonSchema.get("name")).isEqualTo("lead_portal_simple_form_style");
    }
}
