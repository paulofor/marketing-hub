package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowImagePrompt;
import com.marketinghub.leadportal.model.FlowSubmission;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlowImagePromptServiceTest {

    private final FlowImagePromptService service =
            new FlowImagePromptService(new SimpleImageBriefingMapper(), new ObjectMapper());

    @Test
    void shouldBuildPromptForSimpleForm() {
        Flow flow = new Flow(
                "formulario-simples-personal-trainer",
                "Formulário simples",
                null,
                null,
                null,
                List.of(), null);

        Map<String, Object> answers = Map.of(
                "nome", "Ana",
                "forma_contato", "WhatsApp",
                "tipo_aulas", List.of("Musculação", "Yoga"),
                "academia_ou_studio", "Studio Movimento",
                "outras_aulas", "HIIT, Pilates");

        FlowSubmission submission = new FlowSubmission(
                UUID.randomUUID(),
                flow.slug(),
                "Ana",
                "ana@teste.com",
                answers,
                null,
                null,
                null,
                null,
                Instant.now(),
                null);

        Optional<FlowImagePrompt> result = service.buildPrompt(flow, submission);
        assertTrue(result.isPresent());
        FlowImagePrompt prompt = result.get();
        assertEquals(Integer.valueOf(6), prompt.plannedOutputs());
        assertEquals("gpt-image-1", prompt.model());
        String promptText = prompt.prompt();
        assertTrue(promptText.contains("WhatsApp"));
        assertTrue(promptText.contains("Musculação"));
        assertTrue(promptText.contains("Studio Movimento"));
        assertTrue(promptText.contains("lote"));
    }
}
