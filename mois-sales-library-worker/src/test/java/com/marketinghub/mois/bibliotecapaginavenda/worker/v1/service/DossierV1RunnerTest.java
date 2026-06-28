package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierPendingJob;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierPendingRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierPendingResponse;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierRecebeRequestRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierRecebeResponseRequest;
import com.marketinghub.pipelines.dossie.v1.PipelineWorker;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import com.marketinghub.pipelines.dossie.v1.StageResult.OpenAiInteraction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Valida o runner operacional do pipeline dossieproduto.v1 no worker MOIS. */
class DossierV1RunnerTest {

    /** Garante que etapas com ChatGPT enviem ao backend o request e response exatos da OpenAI. */
    @Test
    void deveRegistrarPayloadsBrutosDaOpenAiNosCallbacksCanonicos() {
        DossierV1BackendClient backendClient = org.mockito.Mockito.mock(DossierV1BackendClient.class);
        PipelineWorker pipelineWorker = org.mockito.Mockito.mock(PipelineWorker.class);
        WorkerProperties properties = new WorkerProperties(null, "workspace-mois", null, null, null, null, 0L, 0L, 0L, null, null, false, 0L, null, null, null, null, 10);
        DossierV1Runner runner = new DossierV1Runner(backendClient, pipelineWorker, properties);
        DossierPendingJob job = new DossierPendingJob(
                "job-openai", 11L, 22L, "workspace-mois", "product-understanding", Map.of("productKey", "produto-1"));
        String rawRequest = "{\"model\":\"gpt-5.2\",\"input\":\"prompt exato\"}";
        String rawResponse = "{\"id\":\"resp_1\",\"output_text\":\"resposta exata\"}";
        StageResult result = StageResult.doneWithOpenAiInteractions(
                Map.of("product-understanding", Map.of("status", "DONE")),
                List.of(),
                List.of(new OpenAiInteraction(rawRequest, rawResponse, 10, 20, new BigDecimal("0.01"), "gpt-5.2", null)));

        stubSemPendencia(backendClient, "intake");
        when(backendClient.pending(eq("product-understanding"), any(DossierPendingRequest.class)))
                .thenReturn(new DossierPendingResponse(true, List.of(job)));
        stubSemPendencia(backendClient, "source-product-match");
        stubSemPendencia(backendClient, "investigation-anchor-builder");
        stubSemPendencia(backendClient, "warmup-resource-discovery");
        stubSemPendencia(backendClient, "warmup-signal-extraction");
        stubSemPendencia(backendClient, "warmup-map-builder");
        stubSemPendencia(backendClient, "dossier-synthesis");
        when(pipelineWorker.execute(any(StageContext.class))).thenReturn(result);

        runner.runDossierV1Cycle();

        InOrder order = inOrder(backendClient, pipelineWorker);
        order.verify(backendClient).recebeRequest(eq("product-understanding"), eq("produto-1"), eq("job-openai"), any(DossierRecebeRequestRequest.class));
        order.verify(pipelineWorker).execute(any(StageContext.class));
        ArgumentCaptor<DossierRecebeRequestRequest> openAiRequestCaptor = ArgumentCaptor.forClass(DossierRecebeRequestRequest.class);
        order.verify(backendClient).recebeRequest(eq("product-understanding"), eq("produto-1"), eq("job-openai"), openAiRequestCaptor.capture());
        ArgumentCaptor<DossierRecebeResponseRequest> openAiResponseCaptor = ArgumentCaptor.forClass(DossierRecebeResponseRequest.class);
        order.verify(backendClient).recebeResponse(eq("product-understanding"), eq("produto-1"), eq("job-openai"), openAiResponseCaptor.capture());

        org.assertj.core.api.Assertions.assertThat(openAiRequestCaptor.getValue().request()).isEqualTo(rawRequest);
        org.assertj.core.api.Assertions.assertThat(openAiRequestCaptor.getValue().plataforma()).isEqualTo("openai");
        org.assertj.core.api.Assertions.assertThat(openAiResponseCaptor.getValue().response()).isEqualTo(rawResponse);
        org.assertj.core.api.Assertions.assertThat(openAiResponseCaptor.getValue().quantidadeTokenEntrada()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(openAiResponseCaptor.getValue().quantidadeTokenSaida()).isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(openAiResponseCaptor.getValue().modelo()).isEqualTo("gpt-5.2");
    }

    /** Configura uma etapa do backend fake sem jobs pendentes. */
    private void stubSemPendencia(DossierV1BackendClient backendClient, String stageName) {
        when(backendClient.pending(eq(stageName), any(DossierPendingRequest.class)))
                .thenReturn(new DossierPendingResponse(false, List.of()));
    }
}
