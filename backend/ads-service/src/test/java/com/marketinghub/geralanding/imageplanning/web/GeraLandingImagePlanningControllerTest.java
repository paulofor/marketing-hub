package com.marketinghub.geralanding.imageplanning.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.geralanding.imageplanning.service.BackendImagePlanningService;
import com.marketinghub.geralanding.imageplanning.service.GeraLandingImagePlanningStartResponse;
import com.marketinghub.geralanding.imageplanning.service.recebePrompt.RecebeDispatchRequest;
import com.marketinghub.geralanding.imageplanning.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.imageplanning.service.recebeResposta.RecebeRespostaRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Valida o controller backend da etapa image planning. */
class GeraLandingImagePlanningControllerTest {

    /** Deve delegar o início manual para o serviço da etapa image planning. */
    @Test
    void startShouldDelegateToImagePlanningService() {
        BackendImagePlanningService executionService = mock(BackendImagePlanningService.class);
        GeraLandingImagePlanningController controller = new GeraLandingImagePlanningController(executionService);
        when(executionService.start(44L)).thenReturn(new GeraLandingImagePlanningStartResponse("job-1", "INICIADO"));

        var response = controller.start(44L);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("job-1", response.getBody().idJob());
        verify(executionService).start(44L);
    }

    /** Deve receber prompt e delegar persistência do request cru para o serviço da etapa image planning. */
    @Test
    void receivePromptShouldDelegateToImagePlanningService() {
        BackendImagePlanningService executionService = mock(BackendImagePlanningService.class);
        GeraLandingImagePlanningController controller = new GeraLandingImagePlanningController(executionService);
        RecebePromptRequest payload = new RecebePromptRequest(
                44L,
                "landing-page-image-planning",
                "prompt",
                "markdown",
                "schema",
                "request",
                "gpt-5.2",
                "openai-job-1");

        var response = controller.receivePrompt("job-ia-1", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markPromptReceived(
                "job-ia-1", "prompt", "markdown", "schema", "request", "gpt-5.2", "openai-job-1");
    }

    /** Deve receber dispatch e delegar mudança de status para aguardando retorno. */
    @Test
    void receiveDispatchShouldDelegateToImagePlanningService() {
        BackendImagePlanningService executionService = mock(BackendImagePlanningService.class);
        GeraLandingImagePlanningController controller = new GeraLandingImagePlanningController(executionService);
        RecebeDispatchRequest payload = new RecebeDispatchRequest(44L, "landing-page-image-planning", "openai-job-1");

        var response = controller.receiveDispatch("job-ia-1", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markWaitingOpenAiDispatch("job-ia-1", "openai-job-1");
    }

    /** Deve receber resultado e delegar conclusão da resposta para o serviço da etapa image planning. */
    @Test
    void receiveResultShouldDelegateToImagePlanningService() {
        BackendImagePlanningService executionService = mock(BackendImagePlanningService.class);
        GeraLandingImagePlanningController controller = new GeraLandingImagePlanningController(executionService);
        RecebeRespostaRequest payload = new RecebeRespostaRequest(
                44L,
                "landing-page-image-planning",
                "{\"landingPageImagePlanning\":{}}",
                "<html>preview</html>",
                120,
                80,
                new BigDecimal("0.012300"),
                "openai-job-1",
                null,
                null);

        var response = controller.receiveResult("job-ia-1", payload);

        assertEquals(202, response.getStatusCode().value());
        verify(executionService).markCompletedFromResponse(
                "job-ia-1",
                44L,
                "landing-page-image-planning",
                "{\"landingPageImagePlanning\":{}}",
                "<html>preview</html>",
                120,
                80,
                new BigDecimal("0.012300"),
                "openai-job-1",
                null,
                null);
    }
}
