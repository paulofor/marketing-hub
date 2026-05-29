package com.marketinghub.geralanding.wireframe.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.wireframe.service.BackendWireframeService;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStartResponse;
import com.marketinghub.geralanding.wireframe.service.pending.RecordWireframeExperiment;
import com.marketinghub.geralanding.wireframe.service.pending.RecordWireframeHypothesis;
import com.marketinghub.geralanding.wireframe.service.pending.RecordWireframePending;
import com.marketinghub.geralanding.wireframe.service.recebeprompt.RecebePromptRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Valida o contrato do controller de backend da etapa wireframe. */
@ExtendWith(OutputCaptureExtension.class)
class BackendWireframeControllerTest {

    /** Deve delegar o início da etapa diretamente para o BackendWireframeService. */
    @Test
    void startShouldDelegateToBackendWireframeService() {
        BackendWireframeService executionService = mock(BackendWireframeService.class);
        BackendWireframeController controller = new BackendWireframeController(executionService);
        GeraLandingWireframeStartResponse startResponse =
                new GeraLandingWireframeStartResponse("job-start", "INICIADO");
        when(executionService.start(44L)).thenReturn(startResponse);

        var response = controller.start(44L);

        assertEquals(202, response.getStatusCode().value());
        assertEquals(startResponse, response.getBody());
        verify(executionService).start(44L);
    }

    /** Deve delegar a listagem pendente para a etapa canônica de wireframe. */
    @Test
    void pendingShouldReturnStartedWireframeJobs() {
        BackendWireframeService executionService = mock(BackendWireframeService.class);
        BackendWireframeController controller = new BackendWireframeController(executionService);
        List<RecordWireframePending> pending = List.of(
                new RecordWireframePending(
                        12L,
                        "job-12",
                        "landing-page-wireframe",
                        pendingExperiment(12L, "Experimento 12", "Hipótese"),
                        pendingHypothesis()));
        when(executionService.listPending("landing-page-wireframe")).thenReturn(pending);

        List<RecordWireframePending> response = controller.pending();

        assertEquals(pending, response);
        verify(executionService).listPending("landing-page-wireframe");
    }

    /** Deve receber o prompt enviado para IA e delegar marcação de espera pelo retorno OpenAI. */
    @Test
    void recebePromptShouldMarkExecutionWaitingOpenAiDispatch(CapturedOutput output) {
        BackendWireframeService executionService = mock(BackendWireframeService.class);
        BackendWireframeController controller = new BackendWireframeController(executionService);
        RecebePromptRequest payload = new RecebePromptRequest("Prompt para IA", "openai-job-1");

        var response = controller.recebePrompt("job-ia-1", payload);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("Prompt para IA", payload.prompt());
        assertEquals("openai-job-1", payload.jobidopenai());
        verify(executionService).markWaitingOpenAiDispatch("job-ia-1", "Prompt para IA", "openai-job-1");
        assertTrue(output.getOut().contains("prompt=Prompt para IA"));
    }

    /** Deve serializar pending como lista com atributos experiment e jobid em cada item. */
    @Test
    void pendingShouldSerializeListItemsWithExperimentAndJobid() throws Exception {
        BackendWireframeService executionService = mock(BackendWireframeService.class);
        BackendWireframeController controller = new BackendWireframeController(executionService);
        when(executionService.listPending("landing-page-wireframe")).thenReturn(List.of(
                new RecordWireframePending(
                        33L,
                        "bbed57d0-dcc7-40ab-b936-20a19e21c7fe",
                        "landing-page-wireframe",
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
    private RecordWireframeHypothesis pendingHypothesis() {
        return new RecordWireframeHypothesis(
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
    private RecordWireframeExperiment pendingExperiment(Long id, String name, String hypothesis) {
        return new RecordWireframeExperiment(
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
