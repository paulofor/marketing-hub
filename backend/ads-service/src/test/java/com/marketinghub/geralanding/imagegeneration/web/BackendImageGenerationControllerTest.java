package com.marketinghub.geralanding.imagegeneration.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.imagegeneration.service.BackendImageGenerationService;
import com.marketinghub.geralanding.imagegeneration.service.GeraLandingImageGenerationStartResponse;
import com.marketinghub.geralanding.imagegeneration.service.pending.RecordImageGenerationExperiment;
import com.marketinghub.geralanding.imagegeneration.service.pending.RecordImageGenerationHypothesis;
import com.marketinghub.geralanding.imagegeneration.service.pending.RecordImageGenerationPending;
import com.marketinghub.geralanding.imagegeneration.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.imagegeneration.service.recebeResposta.RecebeRespostaRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Valida o contrato do controller de backend da etapa image generation. */
@ExtendWith(OutputCaptureExtension.class)
class BackendImageGenerationControllerTest {

    /** Deve delegar o início da etapa diretamente para o BackendImageGenerationService. */
    @Test
    void startShouldDelegateToBackendImageGenerationService() {
        BackendImageGenerationService executionService = mock(BackendImageGenerationService.class);
        BackendImageGenerationController controller = new BackendImageGenerationController(executionService);
        GeraLandingImageGenerationStartResponse startResponse =
                new GeraLandingImageGenerationStartResponse("job-start", "INICIADO");
        when(executionService.start(44L)).thenReturn(startResponse);

        var response = controller.start(44L);

        assertEquals(202, response.getStatusCode().value());
        assertEquals(startResponse, response.getBody());
        verify(executionService).start(44L);
    }

    /** Deve delegar a listagem pendente para a etapa canônica de image generation. */
    @Test
    void pendingShouldReturnStartedImageGenerationJobs() {
        BackendImageGenerationService executionService = mock(BackendImageGenerationService.class);
        BackendImageGenerationController controller = new BackendImageGenerationController(executionService);
        List<RecordImageGenerationPending> pending = List.of(
                new RecordImageGenerationPending(
                        12L,
                        "job-12",
                        "landing-page-image-generation",
                        pendingExperiment(12L, "Experimento 12", "Hipótese"),
                        pendingHypothesis()));
        when(executionService.listPending("landing-page-image-generation")).thenReturn(pending);

        List<RecordImageGenerationPending> response = controller.pending();

        assertEquals(pending, response);
        verify(executionService).listPending("landing-page-image-generation");
    }

    /** Deve receber o prompt enviado para IA e delegar marcação de espera pelo retorno OpenAI. */
    @Test
    void recebePromptShouldMarkExecutionWaitingOpenAiDispatch(CapturedOutput output) {
        BackendImageGenerationService executionService = mock(BackendImageGenerationService.class);
        BackendImageGenerationController controller = new BackendImageGenerationController(executionService);
        RecebePromptRequest payload = new RecebePromptRequest(
                "Prompt para IA",
                "# Prompt markdown bruto",
                "{\"type\":\"object\"}",
                "{\"model\":\"gpt-test\"}",
                "openai-job-1");

        var response = controller.recebePrompt("job-ia-1", payload);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("Prompt para IA", payload.prompt());
        assertEquals("openai-job-1", payload.jobidopenai());
        assertEquals("# Prompt markdown bruto", payload.promptMarkdownContent());
        assertEquals("{\"type\":\"object\"}", payload.schemaJson());
        assertEquals("{\"model\":\"gpt-test\"}", payload.requestBodyJson());
        verify(executionService).markWaitingOpenAiDispatch(
                "job-ia-1",
                "Prompt para IA",
                "# Prompt markdown bruto",
                "{\"type\":\"object\"}",
                "{\"model\":\"gpt-test\"}",
                "openai-job-1");
        assertTrue(output.getOut().contains("requestBodyLength="));
    }

    /** Deve receber a resposta da IA e delegar a conclusão da execução image generation. */
    @Test
    void recebeRespostaShouldDelegatePayloadToImageGenerationService() {
        BackendImageGenerationService executionService = mock(BackendImageGenerationService.class);
        BackendImageGenerationController controller = new BackendImageGenerationController(executionService);
        RecebeRespostaRequest payload = new RecebeRespostaRequest(
                44L,
                "landing-page-image-generation",
                "{\"landingPageWireframe\":{}}",
                120,
                80,
                new BigDecimal("0.012300"),
                "openai-job-1",
                null,
                null);

        var response = controller.recebeResposta("job-ia-1", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markCompletedFromResponse(
                "job-ia-1",
                44L,
                "landing-page-image-generation",
                "{\"landingPageWireframe\":{}}",
                120,
                80,
                new BigDecimal("0.012300"),
                "openai-job-1",
                null,
                null);
    }


    /** Deve receber payload de erro e delegar a falha para o serviço da etapa image generation. */
    @Test
    void recebeRespostaShouldDelegateErrorPayloadToImageGenerationService() {
        BackendImageGenerationService executionService = mock(BackendImageGenerationService.class);
        BackendImageGenerationController controller = new BackendImageGenerationController(executionService);
        RecebeRespostaRequest payload = new RecebeRespostaRequest(
                44L,
                "landing-page-image-generation",
                null,
                null,
                null,
                null,
                null,
                "Falha OpenAI",
                "Stack trace resumido");

        var response = controller.recebeResposta("job-ia-erro", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markCompletedFromResponse(
                "job-ia-erro",
                44L,
                "landing-page-image-generation",
                null,
                null,
                null,
                null,
                null,
                "Falha OpenAI",
                "Stack trace resumido");
    }

    /** Deve serializar pending como lista com atributos experiment e jobid em cada item. */
    @Test
    void pendingShouldSerializeListItemsWithExperimentAndJobid() throws Exception {
        BackendImageGenerationService executionService = mock(BackendImageGenerationService.class);
        BackendImageGenerationController controller = new BackendImageGenerationController(executionService);
        when(executionService.listPending("landing-page-image-generation")).thenReturn(List.of(
                new RecordImageGenerationPending(
                        33L,
                        "bbed57d0-dcc7-40ab-b936-20a19e21c7fe",
                        "landing-page-image-generation",
                        pendingExperiment(33L, "Experimento 33", "Hipótese 33"),
                        pendingHypothesis())));

        JsonNode json = new ObjectMapper().valueToTree(controller.pending());

        assertTrue(json.isArray());
        assertEquals(1, json.size());
        assertTrue(json.get(0).has("experiment"));
        assertTrue(json.get(0).has("jobid"));
        assertEquals("bbed57d0-dcc7-40ab-b936-20a19e21c7fe", json.get(0).get("jobid").asText());
        assertEquals(33L, json.get(0).get("experiment").get("id").asLong());
        assertEquals("Prompt texto", json.get(0).get("experiment").get("creativeTextPrompt").asText());
        assertTrue(json.get(0).get("experiment").get("campaignAngle").isObject());
        assertEquals(
                "Promessa clara",
                json.get(0).get("experiment").get("campaignAngle").get("campaignAngle").get("singleMindedPromise").asText());
        assertEquals("<html>GeraLanding</html>", json.get(0).get("experiment").get("htmlGeraLanding").asText());
        assertTrue(json.get(0).has("hypothesis"));
        assertEquals("Hipótese Framework", json.get(0).get("hypothesis").get("title").asText());
        assertEquals("Dor superficial", json.get(0).get("hypothesis").get("framework").get("pain").get("surface").asText());
        assertTrue(json.get(0).get("hypothesis").get("framework").has("result"));
        assertTrue(json.get(0).get("hypothesis").get("framework").has("mechanism"));
        assertTrue(json.get(0).get("hypothesis").get("framework").has("proof"));
        assertTrue(json.get(0).get("hypothesis").get("framework").has("offer"));
        assertTrue(json.get(0).get("hypothesis").get("framework").has("checklist"));
    }

    /** Cria a hipótese usada pelos testes de contrato pending. */
    private RecordImageGenerationHypothesis pendingHypothesis() {
        return new RecordImageGenerationHypothesis(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Hipótese Framework",
                Map.of(
                        "pain", Map.of("surface", "Dor superficial"),
                        "result", Map.of("desiredResult", "Resultado desejado"),
                        "mechanism", Map.of("core", "Mecanismo central"),
                        "proof", Map.of("type", "Prova"),
                        "offer", Map.of("name", "Oferta"),
                        "checklist", Map.of("painReady", true)));
    }

    /** Cria o resumo de experimento usado pelos testes de contrato pending. */
    private RecordImageGenerationExperiment pendingExperiment(Long id, String name, String hypothesis) {
        return new RecordImageGenerationExperiment(
                id,
                name,
                hypothesis,
                "PLANNED",
                "LANDING",
                "Prompt texto",
                "Prompt imagem",
                Map.of("campaignAngle", Map.of("singleMindedPromise", "Promessa clara")),
                Map.of("adCopy", Map.of("headline", "Headline")),
                "Briefing imagem",
                "Copy landing",
                "ImageGeneration landing",
                "Planejamento imagem",
                "Preset design",
                "Entregáveis landing",
                "<html>GeraLanding</html>");
    }
}
