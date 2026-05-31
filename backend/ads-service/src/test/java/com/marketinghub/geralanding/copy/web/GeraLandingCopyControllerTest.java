package com.marketinghub.geralanding.copy.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStageExecutionService;
import com.marketinghub.geralanding.copy.service.GeraLandingCopyStartResponse;
import com.marketinghub.geralanding.copy.service.pending.RecordCopyExperiment;
import com.marketinghub.geralanding.copy.service.pending.RecordCopyHypothesis;
import com.marketinghub.geralanding.copy.service.pending.RecordCopyPending;
import com.marketinghub.geralanding.copy.service.recebePrompt.RecebeDispatchRequest;
import com.marketinghub.geralanding.copy.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.copy.service.recebeResposta.RecebeRespostaRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida o contrato HTTP da etapa copy do GeraLanding. */
class GeraLandingCopyControllerTest {

    /** Deve delegar início da etapa copy para o serviço específico. */
    @Test
    void startShouldDelegateToCopyService() {
        GeraLandingCopyStageExecutionService executionService = mock(GeraLandingCopyStageExecutionService.class);
        GeraLandingCopyController controller = new GeraLandingCopyController(executionService);
        when(executionService.start(44L)).thenReturn(new GeraLandingCopyStartResponse("job-copy", "INICIADO"));

        var response = controller.start(44L);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("job-copy", response.getBody().idJob());
        verify(executionService).start(44L);
    }

    /** Deve receber prompt e delegar o payload bruto para o serviço da etapa copy. */
    @Test
    void receivePromptShouldDelegateToCopyService() {
        GeraLandingCopyStageExecutionService executionService = mock(GeraLandingCopyStageExecutionService.class);
        GeraLandingCopyController controller = new GeraLandingCopyController(executionService);
        RecebePromptRequest payload = new RecebePromptRequest(
                44L,
                "landing-page-copy",
                "prompt",
                "markdown",
                "{}",
                "{\"model\":\"gpt\"}",
                "gpt-5.2",
                "openai-job-1");

        var response = controller.receivePrompt("job-copy", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markPromptReceived(
                "job-copy", "prompt", "markdown", "{}", "{\"model\":\"gpt\"}", "gpt-5.2", "openai-job-1");
    }

    /** Deve receber dispatch e delegar o identificador OpenAI para o serviço da etapa copy. */
    @Test
    void receiveDispatchShouldDelegateToCopyService() {
        GeraLandingCopyStageExecutionService executionService = mock(GeraLandingCopyStageExecutionService.class);
        GeraLandingCopyController controller = new GeraLandingCopyController(executionService);

        var response = controller.receiveDispatch(
                "job-copy", new RecebeDispatchRequest(44L, "landing-page-copy", "openai-job-1"));

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markWaitingOpenAiDispatch("job-copy", "openai-job-1");
    }

    /** Deve receber resposta de sucesso e delegar conclusão para o serviço da etapa copy. */
    @Test
    void receiveResultShouldDelegateSuccessPayloadToCopyService() {
        GeraLandingCopyStageExecutionService executionService = mock(GeraLandingCopyStageExecutionService.class);
        GeraLandingCopyController controller = new GeraLandingCopyController(executionService);
        RecebeRespostaRequest payload = new RecebeRespostaRequest(
                44L,
                "landing-page-copy",
                "{\"landingPageCopy\":{}}",
                120,
                80,
                new BigDecimal("0.012300"),
                "openai-job-1",
                null,
                null);

        var response = controller.receiveResult("job-copy", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markCompletedFromResponse(
                "job-copy", 44L, "landing-page-copy", "{\"landingPageCopy\":{}}", 120, 80,
                new BigDecimal("0.012300"), "openai-job-1", null, null);
    }

    /** Deve serializar pending como lista com atributos compatíveis com o worker atual. */
    @Test
    void pendingShouldSerializeListItemsWithExperimentJobidAndIdJob() throws Exception {
        GeraLandingCopyStageExecutionService executionService = mock(GeraLandingCopyStageExecutionService.class);
        GeraLandingCopyController controller = new GeraLandingCopyController(executionService);
        when(executionService.listPending("landing-page-copy")).thenReturn(List.of(
                new RecordCopyPending(
                        33L,
                        "job-copy",
                        "job-copy",
                        "landing-page-copy",
                        "INICIADO",
                        pendingExperiment(33L, "Experimento 33", "Hipótese 33"),
                        pendingHypothesis())));

        JsonNode json = new ObjectMapper().valueToTree(controller.pending());

        assertTrue(json.isArray());
        assertEquals(1, json.size());
        assertEquals("job-copy", json.get(0).get("jobid").asText());
        assertEquals("job-copy", json.get(0).get("idJob").asText());
        assertEquals("INICIADO", json.get(0).get("status").asText());
        assertEquals(33L, json.get(0).get("experiment").get("id").asLong());
        assertTrue(json.get(0).has("hypothesis"));
        assertEquals("Dor superficial", json.get(0).get("hypothesis").get("framework").get("pain").get("surface").asText());
    }

    /** Cria a hipótese usada pelos testes de contrato pending. */
    private RecordCopyHypothesis pendingHypothesis() {
        return new RecordCopyHypothesis(
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
    private RecordCopyExperiment pendingExperiment(Long id, String name, String hypothesis) {
        return new RecordCopyExperiment(
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
                "Wireframe landing",
                "Planejamento imagem",
                "Preset design",
                "Entregáveis landing",
                "<html>GeraLanding</html>");
    }
}
