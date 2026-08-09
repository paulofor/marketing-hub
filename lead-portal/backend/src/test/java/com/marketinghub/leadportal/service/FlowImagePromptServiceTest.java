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

/** Valida a montagem dos prompts e a política de imagens gratuitas dos fluxos públicos. */
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
                null, null, null, null);

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
        assertEquals("gpt-image-2", prompt.model());
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
                null, null, null, null);

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
                null, null, null, null);

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
        assertEquals("gpt-image-2", prompt.model());
        assertTrue(prompt.prompt().contains("Carla"));
        assertTrue(prompt.prompt().contains("@personalcarla"));
        assertTrue(prompt.prompt().contains("Funcional"));
    }

    /** Garante que a microamostra personalizada entregue gratuitamente todas as saídas prometidas. */
    @Test
    void shouldReleaseAllImagesForPersonalizedSampleFunnel() {
        Flow flow = new Flow(
                "product-ai-exp-84-personalized-sample",
                "Microamostra personalizada",
                null,
                null,
                "AI_PERSONALIZED_SAMPLE_FUNNEL",
                null,
                "gpt-image-1.5",
                null,
                2,
                List.of(),
                null, null, null, null);

        FlowSubmission submission = new FlowSubmission(
                UUID.randomUUID(),
                flow.slug(),
                "Studio Premium",
                "teste@sandbox.local",
                Map.of(
                        "nome_profissional", "Studio Premium",
                        "servico_divulgado", "Alongamento em gel",
                        "estilo_visual", "Elegante e minimalista"),
                null,
                null,
                null,
                null,
                Instant.now(),
                null);

        FlowImagePrompt prompt = service.buildPrompt(flow, submission).orElseThrow();

        assertEquals(Integer.valueOf(2), prompt.plannedOutputs());
        assertEquals(Integer.valueOf(2), prompt.freeImages());
    }

    /** Garante que o contrato publicado pelo GeraSalesPage use respostas reais e libere a amostra. */
    @Test
    void shouldBuildCommercialPromptForGeraSalesPagePersonalizedSample() {
        Flow flow = new Flow(
                "product-ai-exp-88-personalized-sample",
                "Produto IA - amostra personalizada - exp 88",
                null,
                null,
                "AI_PERSONALIZED_SAMPLE_GERA_SALES_PAGE",
                null,
                null,
                null,
                null,
                List.of(),
                null, null, null, null);

        FlowSubmission submission = new FlowSubmission(
                UUID.randomUUID(),
                flow.slug(),
                "Studio Homologação Exp88",
                "teste+exp88@sandbox.local",
                Map.of(
                        "nome_profissional", "Studio Homologação Exp88",
                        "servico_divulgado", "Alongamento em gel delicado",
                        "estilo_visual", "Elegante, rosé e minimalista"),
                null,
                null,
                null,
                null,
                Instant.now(),
                null);

        FlowImagePrompt prompt = service.buildPrompt(flow, submission).orElseThrow();

        assertEquals(Integer.valueOf(6), prompt.plannedOutputs());
        assertEquals(Integer.valueOf(6), prompt.freeImages());
        assertTrue(prompt.prompt().contains("Alongamento em gel delicado"));
        assertTrue(prompt.prompt().contains("Elegante, rosé e minimalista"));
        assertFalse(prompt.prompt().contains("um(a) product ai exp 88 personalized sample"));
        assertFalse(prompt.prompt().contains("serviços principais (product ai exp 88 personalized sample)"));
    }

    @Test
    void shouldBuildPromptWhenSubmissionHasReferenceImage() {
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
                null, null, null, null);

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
        assertTrue(prompt.isPresent());
        assertEquals(Integer.valueOf(6), prompt.get().plannedOutputs());
        assertEquals(Integer.valueOf(1), prompt.get().freeImages());
        assertTrue(prompt.get().prompt().contains("Template: João"));
        assertTrue(prompt.get().prompt().contains("foto enviada pelo lead"));
        assertTrue(prompt.get().prompt().contains("upload.png"));
    }
}
