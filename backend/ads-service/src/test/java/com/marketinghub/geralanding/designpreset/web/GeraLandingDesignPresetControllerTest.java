package com.marketinghub.geralanding.designpreset.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStageExecutionService;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStageService;
import com.marketinghub.geralanding.designpreset.service.GeraLandingDesignPresetStartResponse;
import com.marketinghub.geralanding.designpreset.service.RecebePromptRequest;
import com.marketinghub.geralanding.designpreset.service.RecebeRespostaRequest;
import com.marketinghub.geralanding.designpreset.service.RecordDesignPresetExperiment;
import com.marketinghub.geralanding.designpreset.service.RecordDesignPresetHypothesis;
import com.marketinghub.geralanding.designpreset.service.RecordDesignPresetPending;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida o contrato HTTP da etapa design preset do GeraLanding. */
class GeraLandingDesignPresetControllerTest {

    /** Deve delegar início da etapa design preset para o serviço específico. */
    @Test
    void startShouldDelegateToDesignPresetService() {
        GeraLandingDesignPresetStageService stageService = mock(GeraLandingDesignPresetStageService.class);
        GeraLandingDesignPresetStageExecutionService executionService = mock(GeraLandingDesignPresetStageExecutionService.class);
        GeraLandingDesignPresetController controller = new GeraLandingDesignPresetController(stageService, executionService);
        when(stageService.start(44L)).thenReturn(new GeraLandingDesignPresetStartResponse("job-design-preset", "INICIADO"));

        var response = controller.start(44L);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("job-design-preset", response.getBody().idJob());
        verify(stageService).start(44L);
    }

    /** Deve receber prompt e delegar o payload bruto para o serviço da etapa design preset. */
    @Test
    void receivePromptShouldDelegateToDesignPresetService() {
        GeraLandingDesignPresetStageExecutionService executionService = mock(GeraLandingDesignPresetStageExecutionService.class);
        GeraLandingDesignPresetController controller = new GeraLandingDesignPresetController(mock(GeraLandingDesignPresetStageService.class), executionService);
        RecebePromptRequest payload = new RecebePromptRequest(
                44L,
                "landing-page-design-preset",
                "prompt",
                "markdown",
                "{}",
                "{\"model\":\"gpt\"}",
                "gpt-5.2",
                "openai-job-1");

        var response = controller.recebePrompt("job-design-preset", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markPromptReceived(
                "job-design-preset", "prompt", "markdown", "{}", "{\"model\":\"gpt\"}", "gpt-5.2", "openai-job-1");
    }

    /** Deve receber resposta de sucesso e delegar conclusão para o serviço da etapa design preset. */
    @Test
    void receiveResultShouldDelegateSuccessPayloadToDesignPresetService() {
        GeraLandingDesignPresetStageExecutionService executionService = mock(GeraLandingDesignPresetStageExecutionService.class);
        GeraLandingDesignPresetController controller = new GeraLandingDesignPresetController(mock(GeraLandingDesignPresetStageService.class), executionService);
        RecebeRespostaRequest payload = new RecebeRespostaRequest(
                44L,
                "landing-page-design-preset",
                "{\"landingPageDesignPreset\":{}}",
                120,
                80,
                new BigDecimal("0.012300"),
                "openai-job-1",
                null,
                null);

        var response = controller.recebeResposta("job-design-preset", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markCompletedFromResponse(
                "job-design-preset", 44L, "landing-page-design-preset", "{\"landingPageDesignPreset\":{}}", 120, 80,
                new BigDecimal("0.012300"), "openai-job-1", null, null);
    }

    /** Deve serializar pending como lista com atributos compatíveis com o worker atual. */
    @Test
    void pendingShouldSerializeListItemsWithExperimentJobidAndIdJob() throws Exception {
        GeraLandingDesignPresetStageExecutionService executionService = mock(GeraLandingDesignPresetStageExecutionService.class);
        GeraLandingDesignPresetController controller = new GeraLandingDesignPresetController(mock(GeraLandingDesignPresetStageService.class), executionService);
        when(executionService.listPending("landing-page-design-preset")).thenReturn(List.of(
                new RecordDesignPresetPending(
                        33L,
                        "job-design-preset",
                        "job-design-preset",
                        "landing-page-design-preset",
                        "INICIADO",
                        pendingExperiment(33L, "Experimento 33", "Hipótese 33"),
                        pendingHypothesis())));

        JsonNode json = new ObjectMapper().valueToTree(controller.pending());

        assertTrue(json.isArray());
        assertEquals(1, json.size());
        assertEquals("job-design-preset", json.get(0).get("jobid").asText());
        assertEquals("job-design-preset", json.get(0).get("idJob").asText());
        assertEquals("INICIADO", json.get(0).get("status").asText());
        assertEquals(33L, json.get(0).get("experiment").get("id").asLong());
        assertTrue(json.get(0).has("hypothesis"));
        assertEquals("Dor superficial", json.get(0).get("hypothesis").get("framework").get("pain").get("surface").asText());
    }

    /** Cria a hipótese usada pelos testes de contrato pending. */
    private RecordDesignPresetHypothesis pendingHypothesis() {
        return new RecordDesignPresetHypothesis(
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
    private RecordDesignPresetExperiment pendingExperiment(Long id, String name, String hypothesis) {
        return new RecordDesignPresetExperiment(
                id,
                name,
                hypothesis,
                "PLANNED",
                "LANDING",
                "Prompt texto",
                "Prompt imagem",
                Map.of("campaignAngle", Map.of("singleMindedPromise", "Promessa clara")),
                Map.of("adDesignPreset", Map.of("headline", "Headline")),
                "Briefing imagem",
                "DesignPreset landing",
                "Wireframe landing",
                "Planejamento imagem",
                "Preset design",
                "Entregáveis landing",
                "<html>GeraLanding</html>");
    }
}
