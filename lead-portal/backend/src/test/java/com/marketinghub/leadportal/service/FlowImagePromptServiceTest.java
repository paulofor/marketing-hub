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
                null,
                null,
                null,
                null,
                List.of(),
                null);

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

    @Test
    void shouldExposeDirectAnswerPlaceholders() {
        Flow flow = new Flow(
                "formulario-simples-personal-trainer",
                "Formulário simples",
                null,
                null,
                null,
                null,
                null,
                "Cliente: {{nome}} | Insta: {{resposta.instagram}} | Especialidade: {{resposta.especialidade}}",
                null,
                List.of(),
                null);

        Map<String, Object> answers = Map.of(
                "nome", "Paulo",
                "email", "paulo@example.com",
                "resposta.instagram", "@meuteste",
                "resposta.especialidade", "Alongamento");

        FlowSubmission submission = new FlowSubmission(
                UUID.randomUUID(),
                flow.slug(),
                "Paulo",
                "paulo@example.com",
                answers,
                null,
                null,
                null,
                null,
                Instant.now(),
                null);

        FlowImagePrompt prompt = service.buildPrompt(flow, submission).orElseThrow();
        String promptText = prompt.prompt();
        assertTrue(promptText.contains("Cliente: Paulo"));
        assertTrue(promptText.contains("Insta: @meuteste"));
        assertTrue(promptText.contains("Especialidade: Alongamento"));
    }

    @Test
    void shouldBuildPromptForFlowWithoutSimplePrefix() {
        Flow flow = new Flow(
                "formpersonal",
                "Form Personal 01",
                null,
                null,
                null,
                null,
                "gpt-image-1.5",
                "Cliente: {{nome}} | Insta: {{resposta.instagram}} | Serviços: {{servicos}}",
                8,
                List.of(),
                null);

        Map<String, Object> answers = Map.of(
                "nome", "Carla",
                "tipo_aulas", List.of("Funcional", "HIIT"),
                "resposta.instagram", "@personalcarla");

        FlowSubmission submission = new FlowSubmission(
                UUID.randomUUID(),
                flow.slug(),
                "Carla",
                "carla@example.com",
                answers,
                null,
                null,
                null,
                null,
                Instant.now(),
                null);

        FlowImagePrompt prompt = service.buildPrompt(flow, submission).orElseThrow();
        assertEquals(Integer.valueOf(8), prompt.plannedOutputs());
        assertEquals("gpt-image-1.5", prompt.model());
        assertTrue(prompt.prompt().contains("Carla"));
        assertTrue(prompt.prompt().contains("@personalcarla"));
        assertTrue(prompt.prompt().contains("Funcional"));
    }

    @Test
    void shouldSkipPromptWhenSubmissionHasReferenceImage() {
        Flow flow = new Flow(
                "formulario-simples-personal-trainer",
                "Formulário simples",
                null,
                null,
                null,
                null,
                null,
                "Template: {{nome}}",
                null,
                List.of(),
                null);

        FlowSubmission submission = new FlowSubmission(
                UUID.randomUUID(),
                flow.slug(),
                "João",
                "joao@example.com",
                Map.of("nome", "João"),
                null,
                "stored-file.png",
                "upload.png",
                "image/png",
                Instant.now(),
                null);

        Optional<FlowImagePrompt> prompt = service.buildPrompt(flow, submission);
        assertTrue(prompt.isEmpty());
    }
}

