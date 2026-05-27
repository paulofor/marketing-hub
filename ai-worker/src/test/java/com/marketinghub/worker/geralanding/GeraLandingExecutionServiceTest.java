package com.marketinghub.worker.geralanding;

import static org.mockito.ArgumentMatchers.any;

import com.marketinghub.worker.geralanding.deliverables.GeraLandingJobCompletionDeliverablesPayload;
import com.marketinghub.worker.geralanding.wireframe.callback.GeraLandingJobCompletionWireframePayload;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient;
import com.marketinghub.worker.geralanding.stage.GeraLandingStageSchemaResolver;
import com.marketinghub.worker.geralanding.wireframe.openai.MontaRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

class GeraLandingExecutionServiceTest {

    @Test
    void processPendingExecutionsShouldSendPromptToOpenAiAndRegisterResult() throws Exception {
        GeraLandingCopyBackendClient backendClient = Mockito.mock(GeraLandingCopyBackendClient.class);
        GeraLandingService geraLandingService = Mockito.mock(GeraLandingService.class);
        GeraLandingOpenAiFlexClient openAiClient = Mockito.mock(GeraLandingOpenAiFlexClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GeraLandingStageSchemaResolver stageSchemaResolver = Mockito.mock(GeraLandingStageSchemaResolver.class);
        MontaRequest wireframeMontaRequest = Mockito.mock(MontaRequest.class);
        com.marketinghub.worker.geralanding.copy.MontaRequest copyMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.MontaRequest.class);
        com.marketinghub.worker.geralanding.imageplanning.MontaRequest imagePlanningMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.MontaRequest.class);
        com.marketinghub.worker.geralanding.presetdesign.MontaRequest presetDesignMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.MontaRequest.class);
        com.marketinghub.worker.geralanding.deliverables.MontaRequest deliverablesMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.MontaRequest.class);
        com.marketinghub.worker.geralanding.wireframe.callback.RecebeResponse wireframeRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.wireframe.callback.RecebeResponse.class);
        com.marketinghub.worker.geralanding.copy.RecebeResponse copyRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.RecebeResponse.class);
        com.marketinghub.worker.geralanding.imageplanning.RecebeResponse imagePlanningRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.RecebeResponse.class);
        com.marketinghub.worker.geralanding.presetdesign.RecebeResponse presetDesignRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.RecebeResponse.class);
        com.marketinghub.worker.geralanding.deliverables.RecebeResponse deliverablesRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.RecebeResponse.class);

        com.marketinghub.worker.geralanding.wireframe.monitor.WireframePendingJobsService wireframePendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.wireframe.monitor.WireframePendingJobsService.class);
        com.marketinghub.worker.geralanding.copy.CopyPendingJobsService copyPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.CopyPendingJobsService.class);
        com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService imagePlanningPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService.class);
        com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService presetDesignPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService.class);
        com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService deliverablesPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService.class);

        when(openAiClient.isEnabled()).thenReturn(true);
        when(backendClient.listPendingExecutions(20)).thenReturn(List.of(
                new GeraLandingStageExecutionDto(19L, "dd8a7dac-ce15-4858-99b4-45a7a18591fa", "landing-page-wireframe")));
        when(backendClient.loadPromptData(19L)).thenReturn(Map.of());
        when(wireframeMontaRequest.montarPrompt(any())).thenReturn("prompt-content");
        when(wireframeMontaRequest.carregarPromptMarkdownCru()).thenReturn("prompt-markdown");
        when(wireframeMontaRequest.montar(any())).thenReturn("{\"model\":\"gpt-5.2\"}");
        when(openAiClient.generate(any())).thenReturn(new GeraLandingJobCompletionPayload(
                """
                {
                  "definicoes": {
                    "estrutura": {"desktop":[{"nome":"wFull","atributoCss":"width","valor":"100%"}],"mobile":[{"nome":"wFull","atributoCss":"width","valor":"100%"}]},
                    "posicao": {"desktop":[{"nome":"posRelative","atributoCss":"position","valor":"relative"}],"mobile":[{"nome":"posRelative","atributoCss":"position","valor":"relative"}]},
                    "layout": {"desktop":[{"nome":"displayFlex","atributoCss":"display","valor":"flex"}],"mobile":[{"nome":"displayFlex","atributoCss":"display","valor":"flex"}]},
                    "mistas": {"desktop":[{"nome":"noneTransform","atributoCss":"transform","valor":"none"}],"mobile":[{"nome":"noneTransform","atributoCss":"transform","valor":"none"}]}
                  },
                  "pagina": {
                    "corpo": {
                      "secoes": [
                        {"estilos":["wFull"],"estrutura":["wFull"],"posicao":["posRelative"],"layout":["displayFlex"],"mistas":["noneTransform"],"elementosSeccao":[]}
                      ]
                    }
                  }
                }
                """, "raw", "request", "openai-job", 10, 20, BigDecimal.ONE));

        GeraLandingExecutionService service =
                new GeraLandingExecutionService(
                        backendClient,
                        geraLandingService,
                        openAiClient,
                        objectMapper,
                        stageSchemaResolver,
                        wireframeMontaRequest,
                        copyMontaRequest,
                        imagePlanningMontaRequest,
                        presetDesignMontaRequest,
                        deliverablesMontaRequest,
                        wireframeRecebeResponse,
                        copyRecebeResponse,
                        imagePlanningRecebeResponse,
                        presetDesignRecebeResponse,
                        deliverablesRecebeResponse,
                        wireframePendingJobsService,
                        copyPendingJobsService,
                        imagePlanningPendingJobsService,
                        presetDesignPendingJobsService,
                        deliverablesPendingJobsService,
                        20,
                        new ClassPathResource("prompts/geralanding/landing-page-wireframe-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-copy-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-image-planning-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-deliverables-schema.json"));
        service.processPendingExecutions();

        verify(openAiClient).generate(any());
        verify(backendClient).receivePrompt(any(), any(), any(), any(), any(), any(), any(), any());
        verify(wireframeRecebeResponse).processar(any(), any(), any(), any());
        verify(backendClient, never()).receiveDispatch(any(), any(), any(), any());
        verify(backendClient, never())
                .receiveResult(any(), any(), any(), any(GeraLandingJobCompletionWireframePayload.class));
        verify(backendClient, never())
                .receiveResult(any(), any(), any(), any(GeraLandingJobCompletionDeliverablesPayload.class));
        verify(backendClient, never()).receiveFailure(any(), any(), any(), any(), any());
    }

    @Test
    void processPendingExecutionsShouldRegisterFailureWhenOpenAiFails() throws Exception {
        GeraLandingCopyBackendClient backendClient = Mockito.mock(GeraLandingCopyBackendClient.class);
        GeraLandingService geraLandingService = Mockito.mock(GeraLandingService.class);
        GeraLandingOpenAiFlexClient openAiClient = Mockito.mock(GeraLandingOpenAiFlexClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GeraLandingStageSchemaResolver stageSchemaResolver = Mockito.mock(GeraLandingStageSchemaResolver.class);
        MontaRequest wireframeMontaRequest = Mockito.mock(MontaRequest.class);
        com.marketinghub.worker.geralanding.copy.MontaRequest copyMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.MontaRequest.class);
        com.marketinghub.worker.geralanding.imageplanning.MontaRequest imagePlanningMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.MontaRequest.class);
        com.marketinghub.worker.geralanding.presetdesign.MontaRequest presetDesignMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.MontaRequest.class);
        com.marketinghub.worker.geralanding.deliverables.MontaRequest deliverablesMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.MontaRequest.class);
        com.marketinghub.worker.geralanding.wireframe.callback.RecebeResponse wireframeRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.wireframe.callback.RecebeResponse.class);
        com.marketinghub.worker.geralanding.copy.RecebeResponse copyRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.RecebeResponse.class);
        com.marketinghub.worker.geralanding.imageplanning.RecebeResponse imagePlanningRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.RecebeResponse.class);
        com.marketinghub.worker.geralanding.presetdesign.RecebeResponse presetDesignRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.RecebeResponse.class);
        com.marketinghub.worker.geralanding.deliverables.RecebeResponse deliverablesRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.RecebeResponse.class);

        com.marketinghub.worker.geralanding.wireframe.monitor.WireframePendingJobsService wireframePendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.wireframe.monitor.WireframePendingJobsService.class);
        com.marketinghub.worker.geralanding.copy.CopyPendingJobsService copyPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.CopyPendingJobsService.class);
        com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService imagePlanningPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService.class);
        com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService presetDesignPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService.class);
        com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService deliverablesPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService.class);

        when(openAiClient.isEnabled()).thenReturn(true);
        when(backendClient.listPendingExecutions(20)).thenReturn(List.of(
                new GeraLandingStageExecutionDto(19L, "dd8a7dac-ce15-4858-99b4-45a7a18591fa", "landing-page-wireframe")));
        when(backendClient.loadPromptData(19L)).thenReturn(Map.of());
        when(wireframeMontaRequest.montarPrompt(any())).thenReturn("prompt-content");
        when(wireframeMontaRequest.carregarPromptMarkdownCru()).thenReturn("prompt-markdown");
        when(wireframeMontaRequest.montar(any())).thenReturn("{\"model\":\"gpt-5.2\"}");
        when(openAiClient.generate(any())).thenThrow(new IllegalStateException("OpenAI erro de flex"));

        GeraLandingExecutionService service =
                new GeraLandingExecutionService(
                        backendClient,
                        geraLandingService,
                        openAiClient,
                        objectMapper,
                        stageSchemaResolver,
                        wireframeMontaRequest,
                        copyMontaRequest,
                        imagePlanningMontaRequest,
                        presetDesignMontaRequest,
                        deliverablesMontaRequest,
                        wireframeRecebeResponse,
                        copyRecebeResponse,
                        imagePlanningRecebeResponse,
                        presetDesignRecebeResponse,
                        deliverablesRecebeResponse,
                        wireframePendingJobsService,
                        copyPendingJobsService,
                        imagePlanningPendingJobsService,
                        presetDesignPendingJobsService,
                        deliverablesPendingJobsService,
                        20,
                        new ClassPathResource("prompts/geralanding/landing-page-wireframe-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-copy-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-image-planning-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-deliverables-schema.json"));
        service.processPendingExecutions();

        verify(backendClient).receiveFailure(any(), any(), any(), any(), any());
        verify(backendClient, never())
                .receiveResult(any(), any(), any(), any(GeraLandingJobCompletionWireframePayload.class));
        verify(backendClient, never())
                .receiveResult(any(), any(), any(), any(GeraLandingJobCompletionDeliverablesPayload.class));
    }

    @Test
    void processPendingExecutionsShouldRegisterFailureWhenWireframeUsesUndefinedStyle() throws Exception {
        GeraLandingCopyBackendClient backendClient = Mockito.mock(GeraLandingCopyBackendClient.class);
        GeraLandingService geraLandingService = Mockito.mock(GeraLandingService.class);
        GeraLandingOpenAiFlexClient openAiClient = Mockito.mock(GeraLandingOpenAiFlexClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GeraLandingStageSchemaResolver stageSchemaResolver = Mockito.mock(GeraLandingStageSchemaResolver.class);
        MontaRequest wireframeMontaRequest = Mockito.mock(MontaRequest.class);
        com.marketinghub.worker.geralanding.copy.MontaRequest copyMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.MontaRequest.class);
        com.marketinghub.worker.geralanding.imageplanning.MontaRequest imagePlanningMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.MontaRequest.class);
        com.marketinghub.worker.geralanding.presetdesign.MontaRequest presetDesignMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.MontaRequest.class);
        com.marketinghub.worker.geralanding.deliverables.MontaRequest deliverablesMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.MontaRequest.class);
        com.marketinghub.worker.geralanding.wireframe.callback.RecebeResponse wireframeRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.wireframe.callback.RecebeResponse.class);
        com.marketinghub.worker.geralanding.copy.RecebeResponse copyRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.RecebeResponse.class);
        com.marketinghub.worker.geralanding.imageplanning.RecebeResponse imagePlanningRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.RecebeResponse.class);
        com.marketinghub.worker.geralanding.presetdesign.RecebeResponse presetDesignRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.RecebeResponse.class);
        com.marketinghub.worker.geralanding.deliverables.RecebeResponse deliverablesRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.RecebeResponse.class);

        com.marketinghub.worker.geralanding.wireframe.monitor.WireframePendingJobsService wireframePendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.wireframe.monitor.WireframePendingJobsService.class);
        com.marketinghub.worker.geralanding.copy.CopyPendingJobsService copyPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.CopyPendingJobsService.class);
        com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService imagePlanningPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService.class);
        com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService presetDesignPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService.class);
        com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService deliverablesPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService.class);

        when(openAiClient.isEnabled()).thenReturn(true);
        when(backendClient.listPendingExecutions(20)).thenReturn(List.of(
                new GeraLandingStageExecutionDto(19L, "dd8a7dac-ce15-4858-99b4-45a7a18591fa", "landing-page-wireframe")));
        when(backendClient.loadPromptData(19L)).thenReturn(Map.of());
        when(wireframeMontaRequest.montarPrompt(any())).thenReturn("prompt-content");
        when(wireframeMontaRequest.carregarPromptMarkdownCru()).thenReturn("prompt-markdown");
        when(wireframeMontaRequest.montar(any())).thenReturn("{\"model\":\"gpt-5.2\"}");
        when(openAiClient.generate(any())).thenReturn(new GeraLandingJobCompletionPayload(
                """
                {
                  "definicoes": {
                    "estrutura": {"desktop":[{"nome":"wFull","atributoCss":"width","valor":"100%"}],"mobile":[{"nome":"wFull","atributoCss":"width","valor":"100%"}]},
                    "posicao": {"desktop":[{"nome":"posRelative","atributoCss":"position","valor":"relative"}],"mobile":[{"nome":"posRelative","atributoCss":"position","valor":"relative"}]},
                    "layout": {"desktop":[{"nome":"displayFlex","atributoCss":"display","valor":"flex"}],"mobile":[{"nome":"displayFlex","atributoCss":"display","valor":"flex"}]},
                    "mistas": {"desktop":[{"nome":"noneTransform","atributoCss":"transform","valor":"none"}],"mobile":[{"nome":"noneTransform","atributoCss":"transform","valor":"none"}]}
                  },
                  "pagina": {
                    "head":{"texto":""},
                    "body":{"classes":["bgBody","fontBase","textPrimary","marginReset"]},
                    "corpo":{
                      "secoes":[
                        {"nome":"hero","objetivo":"x","id":"s1","estrutura":["wFull"],"posicao":["posRelative"],"layout":["displayFlex"],"mistas":["noneTransform"],"estilos":["naoExiste"],"oQueQuerProvocarNoUsuario":"x","papelComercial":"x","fasePersuasao":"x","objeçãoQueRemove":"x","prioridadeConversao":1,"acaoEsperada":"x","fonteContexto":["x"],"elementosSeccao":[]},
                        {"nome":"proof","objetivo":"x","id":"s2","estrutura":["wFull"],"posicao":["posRelative"],"layout":["displayFlex"],"mistas":["noneTransform"],"estilos":["wFull"],"oQueQuerProvocarNoUsuario":"x","papelComercial":"x","fasePersuasao":"x","objeçãoQueRemove":"x","prioridadeConversao":2,"acaoEsperada":"x","fonteContexto":["x"],"elementosSeccao":[]},
                        {"nome":"faq","objetivo":"x","id":"s3","estrutura":["wFull"],"posicao":["posRelative"],"layout":["displayFlex"],"mistas":["noneTransform"],"estilos":["wFull"],"oQueQuerProvocarNoUsuario":"x","papelComercial":"x","fasePersuasao":"x","objeçãoQueRemove":"x","prioridadeConversao":3,"acaoEsperada":"x","fonteContexto":["x"],"elementosSeccao":[]},
                        {"nome":"cta","objetivo":"x","id":"s4","estrutura":["wFull"],"posicao":["posRelative"],"layout":["displayFlex"],"mistas":["noneTransform"],"estilos":["wFull"],"oQueQuerProvocarNoUsuario":"x","papelComercial":"x","fasePersuasao":"x","objeçãoQueRemove":"x","prioridadeConversao":4,"acaoEsperada":"x","fonteContexto":["x"],"elementosSeccao":[]}
                      ]
                    }
                  }
                }
                """, "raw", "request", "openai-job", 10, 20, BigDecimal.ONE));

        GeraLandingExecutionService service =
                new GeraLandingExecutionService(
                        backendClient,
                        geraLandingService,
                        openAiClient,
                        objectMapper,
                        stageSchemaResolver,
                        wireframeMontaRequest,
                        copyMontaRequest,
                        imagePlanningMontaRequest,
                        presetDesignMontaRequest,
                        deliverablesMontaRequest,
                        wireframeRecebeResponse,
                        copyRecebeResponse,
                        imagePlanningRecebeResponse,
                        presetDesignRecebeResponse,
                        deliverablesRecebeResponse,
                        wireframePendingJobsService,
                        copyPendingJobsService,
                        imagePlanningPendingJobsService,
                        presetDesignPendingJobsService,
                        deliverablesPendingJobsService,
                        20,
                        new ClassPathResource("prompts/geralanding/landing-page-wireframe-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-copy-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-image-planning-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json"),
                        new ClassPathResource("prompts/geralanding/landing-page-deliverables-schema.json"));
        service.processPendingExecutions();

        verify(backendClient).receiveFailure(any(), any(), any(), any(), any());
        verify(backendClient, never())
                .receiveResult(any(), any(), any(), any(GeraLandingJobCompletionWireframePayload.class));
        verify(backendClient, never())
                .receiveResult(any(), any(), any(), any(GeraLandingJobCompletionDeliverablesPayload.class));
    }

    @Test
    void processPendingExecutionsShouldRegisterFailureWhenDesignPresetUsesUndefinedStyle() throws Exception {
        GeraLandingCopyBackendClient backendClient = Mockito.mock(GeraLandingCopyBackendClient.class);
        GeraLandingService geraLandingService = Mockito.mock(GeraLandingService.class);
        GeraLandingOpenAiFlexClient openAiClient = Mockito.mock(GeraLandingOpenAiFlexClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GeraLandingStageSchemaResolver stageSchemaResolver = Mockito.mock(GeraLandingStageSchemaResolver.class);
        MontaRequest wireframeMontaRequest = Mockito.mock(MontaRequest.class);
        com.marketinghub.worker.geralanding.copy.MontaRequest copyMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.MontaRequest.class);
        com.marketinghub.worker.geralanding.imageplanning.MontaRequest imagePlanningMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.MontaRequest.class);
        com.marketinghub.worker.geralanding.presetdesign.MontaRequest presetDesignMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.MontaRequest.class);
        com.marketinghub.worker.geralanding.deliverables.MontaRequest deliverablesMontaRequest =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.MontaRequest.class);
        com.marketinghub.worker.geralanding.wireframe.callback.RecebeResponse wireframeRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.wireframe.callback.RecebeResponse.class);
        com.marketinghub.worker.geralanding.copy.RecebeResponse copyRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.RecebeResponse.class);
        com.marketinghub.worker.geralanding.imageplanning.RecebeResponse imagePlanningRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.RecebeResponse.class);
        com.marketinghub.worker.geralanding.presetdesign.RecebeResponse presetDesignRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.RecebeResponse.class);
        com.marketinghub.worker.geralanding.deliverables.RecebeResponse deliverablesRecebeResponse =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.RecebeResponse.class);

        com.marketinghub.worker.geralanding.wireframe.monitor.WireframePendingJobsService wireframePendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.wireframe.monitor.WireframePendingJobsService.class);
        com.marketinghub.worker.geralanding.copy.CopyPendingJobsService copyPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.copy.CopyPendingJobsService.class);
        com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService imagePlanningPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.imageplanning.ImagePlanningPendingJobsService.class);
        com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService presetDesignPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.presetdesign.PresetDesignPendingJobsService.class);
        com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService deliverablesPendingJobsService =
                Mockito.mock(com.marketinghub.worker.geralanding.deliverables.DeliverablesPendingJobsService.class);

        when(openAiClient.isEnabled()).thenReturn(true);
        when(backendClient.listPendingExecutions(20)).thenReturn(List.of(
                new GeraLandingStageExecutionDto(19L, "dd8a7dac-ce15-4858-99b4-45a7a18591fa", "landing-page-design-preset")));
        when(backendClient.loadPromptData(19L)).thenReturn(Map.of());
        when(presetDesignMontaRequest.montarPrompt(any())).thenReturn("prompt-content");
        when(presetDesignMontaRequest.carregarPromptMarkdownCru()).thenReturn("prompt-markdown");
        when(presetDesignMontaRequest.montar(any())).thenReturn("{\"model\":\"gpt-5.2\"}");
        when(openAiClient.generate(any())).thenReturn(new GeraLandingJobCompletionPayload(
                """
                {
                  "definicoes": {
                    "cores-fundo": {"desktop":[{"nome":"bgBody","atributoCss":"background-color","valor":"#fff"}],"mobile":[]},
                    "tipografia": {"desktop":[{"nome":"fontBase","atributoCss":"font-family","valor":"Inter"}],"mobile":[]},
                    "texto": {"desktop":[{"nome":"textPrimary","atributoCss":"color","valor":"#111"}],"mobile":[]},
                    "bordas": {"desktop":[],"mobile":[]},
                    "contorno": {"desktop":[],"mobile":[]},
                    "sombras-transparencia": {"desktop":[],"mobile":[]},
                    "filtro-efeitos": {"desktop":[],"mobile":[]},
                    "cursor": {"desktop":[],"mobile":[]},
                    "listas": {"desktop":[],"mobile":[]},
                    "imagens": {"desktop":[],"mobile":[]},
                    "transições": {"desktop":[],"mobile":[]},
                    "animações": {"desktop":[],"mobile":[]}
                  },
                  "pagina": {
                    "body": {"estilos": ["bgBody", "naoExistePreset"]},
                    "corpo": {"estilos": ["fontBase"], "secoes": []}
                  }
                }
                """, "raw", "request", "openai-job", 10, 20, BigDecimal.ONE));

        GeraLandingExecutionService service = new GeraLandingExecutionService(
                backendClient, geraLandingService, openAiClient, objectMapper, stageSchemaResolver, wireframeMontaRequest,
                copyMontaRequest, imagePlanningMontaRequest, presetDesignMontaRequest, deliverablesMontaRequest, wireframeRecebeResponse, copyRecebeResponse, imagePlanningRecebeResponse, presetDesignRecebeResponse, deliverablesRecebeResponse,
                wireframePendingJobsService, copyPendingJobsService, imagePlanningPendingJobsService, presetDesignPendingJobsService, deliverablesPendingJobsService, 20,
                new ClassPathResource("prompts/geralanding/landing-page-wireframe-schema.json"),
                new ClassPathResource("prompts/geralanding/landing-page-copy-schema.json"),
                new ClassPathResource("prompts/geralanding/landing-page-image-planning-schema.json"),
                new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json"),
                new ClassPathResource("prompts/geralanding/landing-page-deliverables-schema.json"));
        service.processPendingExecutions();

        verify(backendClient).receiveFailure(any(), any(), any(), any(), any());
        verify(backendClient, never())
                .receiveResult(any(), any(), any(), any(GeraLandingJobCompletionWireframePayload.class));
        verify(backendClient, never())
                .receiveResult(any(), any(), any(), any(GeraLandingJobCompletionDeliverablesPayload.class));
    }
}
