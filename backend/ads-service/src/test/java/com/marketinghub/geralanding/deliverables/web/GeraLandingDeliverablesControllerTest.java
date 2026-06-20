package com.marketinghub.geralanding.deliverables.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStageExecutionService;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStageService;
import com.marketinghub.geralanding.deliverables.service.GeraLandingDeliverablesStartResponse;
import com.marketinghub.geralanding.deliverables.service.pending.RecordDeliverablesExperiment;
import com.marketinghub.geralanding.deliverables.service.pending.RecordDeliverablesHypothesis;
import com.marketinghub.geralanding.deliverables.service.pending.RecordDeliverablesPending;
import com.marketinghub.geralanding.deliverables.service.recebePrompt.RecebeDispatchRequest;
import com.marketinghub.geralanding.deliverables.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.deliverables.service.recebeResposta.RecebeRespostaRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida o contrato HTTP da etapa deliverables do GeraLanding. */
class GeraLandingDeliverablesControllerTest {

    /** Deve delegar início da etapa deliverables para o serviço específico. */
    @Test
    void startShouldDelegateToDeliverablesService() {
        GeraLandingDeliverablesStageService stageService = mock(GeraLandingDeliverablesStageService.class);
        GeraLandingDeliverablesStageExecutionService executionService = mock(GeraLandingDeliverablesStageExecutionService.class);
        GeraLandingDeliverablesController controller = new GeraLandingDeliverablesController(stageService, executionService);
        when(stageService.start(44L)).thenReturn(new GeraLandingDeliverablesStartResponse("job-deliverables", "INICIADO"));

        var response = controller.start(44L);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("job-deliverables", response.getBody().idJob());
        verify(stageService).start(44L);
    }

    /** Deve marcar a execução como em processamento antes da chamada OpenAI. */
    @Test
    void runningShouldDelegateToDeliverablesService() {
        GeraLandingDeliverablesStageService stageService = mock(GeraLandingDeliverablesStageService.class);
        GeraLandingDeliverablesStageExecutionService executionService = mock(GeraLandingDeliverablesStageExecutionService.class);
        GeraLandingDeliverablesController controller = new GeraLandingDeliverablesController(stageService, executionService);

        var response = controller.running("job-deliverables", new RecebeDispatchRequest(44L, "landing-page-deliverables", null));

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markRunning("job-deliverables");
    }

    /** Deve receber prompt e delegar o payload bruto para o serviço da etapa deliverables. */
    @Test
    void receivePromptShouldDelegateToDeliverablesService() {
        GeraLandingDeliverablesStageService stageService = mock(GeraLandingDeliverablesStageService.class);
        GeraLandingDeliverablesStageExecutionService executionService = mock(GeraLandingDeliverablesStageExecutionService.class);
        GeraLandingDeliverablesController controller = new GeraLandingDeliverablesController(stageService, executionService);
        RecebePromptRequest payload = new RecebePromptRequest(44L, "landing-page-deliverables", "prompt", "markdown", "{}", "{\"model\":\"gpt\"}", "gpt-5.5", null);

        var response = controller.receivePrompt("job-deliverables", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markPromptReceived("job-deliverables", "prompt", "markdown", "{}", "{\"model\":\"gpt\"}", "gpt-5.5", null);
    }

    /** Deve receber resposta de sucesso e delegar conclusão para o serviço da etapa deliverables. */
    @Test
    void receiveResultShouldDelegateSuccessPayloadToDeliverablesService() {
        GeraLandingDeliverablesStageService stageService = mock(GeraLandingDeliverablesStageService.class);
        GeraLandingDeliverablesStageExecutionService executionService = mock(GeraLandingDeliverablesStageExecutionService.class);
        GeraLandingDeliverablesController controller = new GeraLandingDeliverablesController(stageService, executionService);
        RecebeRespostaRequest payload = new RecebeRespostaRequest(44L, "landing-page-deliverables", "{\"sampleDeliverables\":[],\"finalProductDeliverables\":[]}", 120, 80, new BigDecimal("0.012300"), "openai-job-1", null, null);

        var response = controller.receiveResult("job-deliverables", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markCompletedFromResponse(
                "job-deliverables", 44L, "landing-page-deliverables", "{\"sampleDeliverables\":[],\"finalProductDeliverables\":[]}", 120, 80,
                new BigDecimal("0.012300"), "openai-job-1", null, null);
    }

    /** Deve serializar pending com os campos exigidos pelo Worker AI de deliverables. */
    @Test
    void pendingShouldSerializeListItemsWithExperimentJobidAndIdJob() throws Exception {
        GeraLandingDeliverablesStageService stageService = mock(GeraLandingDeliverablesStageService.class);
        GeraLandingDeliverablesStageExecutionService executionService = mock(GeraLandingDeliverablesStageExecutionService.class);
        GeraLandingDeliverablesController controller = new GeraLandingDeliverablesController(stageService, executionService);
        when(executionService.listPending("landing-page-deliverables")).thenReturn(List.of(new RecordDeliverablesPending(
                33L,
                "job-deliverables",
                "job-deliverables",
                "landing-page-deliverables",
                "INICIADO",
                Instant.parse("2026-06-08T00:00:00Z"),
                pendingExperiment(33L, "Experimento 33", "Hipótese 33"),
                pendingHypothesis())));

        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(controller.pending());

        assertTrue(json.isArray());
        assertEquals(1, json.size());
        assertEquals("job-deliverables", json.get(0).get("jobid").asText());
        assertEquals("job-deliverables", json.get(0).get("idJob").asText());
        assertEquals("INICIADO", json.get(0).get("status").asText());
        assertEquals("<html>GeraLanding</html>", json.get(0).get("experiment").get("htmlGeraLanding").asText());
        assertEquals("Dor superficial", json.get(0).get("hypothesis").get("framework").get("pain").get("surface").asText());
    }

    /** Cria a hipótese usada pelos testes de contrato pending. */
    private RecordDeliverablesHypothesis pendingHypothesis() {
        return new RecordDeliverablesHypothesis(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Hipótese Framework",
                Map.of("pain", Map.of("surface", "Dor superficial"), "result", Map.of("desiredResult", "Resultado desejado")));
    }

    /** Cria o resumo de experimento usado pelos testes de contrato pending. */
    private RecordDeliverablesExperiment pendingExperiment(Long id, String name, String hypothesis) {
        return new RecordDeliverablesExperiment(
                id,
                name,
                hypothesis,
                "clientes desmarcam horário",
                "3 mensagens prontas",
                "Receber as 3 mensagens",
                "Receber as 3 mensagens",
                "LEADS",
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
                null,
                "Quality review",
                "<html>GeraLanding</html>");
    }
}
