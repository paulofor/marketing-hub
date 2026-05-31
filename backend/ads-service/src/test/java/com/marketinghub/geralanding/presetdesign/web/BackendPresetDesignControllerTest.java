package com.marketinghub.geralanding.presetdesign.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.presetdesign.service.BackendPresetDesignService;
import com.marketinghub.geralanding.presetdesign.service.GeraLandingPresetDesignStartResponse;
import com.marketinghub.geralanding.presetdesign.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.presetdesign.service.recebeResposta.RecebeRespostaRequest;
import com.marketinghub.geralanding.presetdesign.service.pending.RecordPresetDesignExperiment;
import com.marketinghub.geralanding.presetdesign.service.pending.RecordPresetDesignHypothesis;
import com.marketinghub.geralanding.presetdesign.service.pending.RecordPresetDesignPending;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Valida o contrato HTTP da etapa design preset do GeraLanding. */
class BackendPresetDesignControllerTest {

    /** Deve delegar início da etapa design preset para o serviço específico. */
    @Test
    void startShouldDelegateToDesignPresetService() {
        BackendPresetDesignService executionService = mock(BackendPresetDesignService.class);
        BackendPresetDesignController controller = new BackendPresetDesignController(executionService);
        when(executionService.start(44L)).thenReturn(new GeraLandingPresetDesignStartResponse("job-design-preset", "INICIADO"));

        var response = controller.start(44L);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("job-design-preset", response.getBody().idJob());
        verify(executionService).start(44L);
    }

    /** Deve receber prompt e delegar o payload bruto para o serviço da etapa design preset. */
    @Test
    void receivePromptShouldDelegateToDesignPresetService() {
        BackendPresetDesignService executionService = mock(BackendPresetDesignService.class);
        BackendPresetDesignController controller = new BackendPresetDesignController(executionService);
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
        BackendPresetDesignService executionService = mock(BackendPresetDesignService.class);
        BackendPresetDesignController controller = new BackendPresetDesignController(executionService);
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
    void pendingShouldSerializeListItemsWithExperimentAndJobid() throws Exception {
        BackendPresetDesignService executionService = mock(BackendPresetDesignService.class);
        BackendPresetDesignController controller = new BackendPresetDesignController(executionService);
        when(executionService.listPending("landing-page-design-preset")).thenReturn(List.of(
                new RecordPresetDesignPending(
                        33L,
                        "job-design-preset",
                        "landing-page-design-preset",
                        pendingExperiment(33L, "Experimento 33", "Hipótese 33"),
                        pendingHypothesis())));

        JsonNode json = new ObjectMapper().valueToTree(controller.pending());

        assertTrue(json.isArray());
        assertEquals(1, json.size());
        assertEquals("job-design-preset", json.get(0).get("jobid").asText());
        assertEquals(33L, json.get(0).get("experiment").get("id").asLong());
        assertTrue(json.get(0).has("hypothesis"));
        assertEquals("Dor superficial", json.get(0).get("hypothesis").get("framework").get("pain").get("surface").asText());
    }

    /** Cria a hipótese usada pelos testes de contrato pending. */
    private RecordPresetDesignHypothesis pendingHypothesis() {
        return new RecordPresetDesignHypothesis(
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
    private RecordPresetDesignExperiment pendingExperiment(Long id, String name, String hypothesis) {
        return new RecordPresetDesignExperiment(
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
