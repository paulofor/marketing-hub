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

/** Valida o runner operacional do pipeline salespagepatterns.v1 no worker MOIS. */
class SalesPagePatternsV1RunnerTest {

    /** Garante que o runner consome a fila de padrões e audita payloads OpenAI no backend correto. */
    @Test
    void deveProcessarPagePatternExtractionComCallbacksCanonicos() {
        SalesPagePatternsV1BackendClient backendClient = org.mockito.Mockito.mock(SalesPagePatternsV1BackendClient.class);
        PipelineWorker pipelineWorker = org.mockito.Mockito.mock(PipelineWorker.class);
        WorkerProperties properties = new WorkerProperties(null, "workspace-mois", null, null, null, null, 0L, 0L, 0L, null, null, false, 0L, null, null, null, null, 10);
        SalesPagePatternsV1Runner runner = new SalesPagePatternsV1Runner(backendClient, pipelineWorker, properties);
        StageContext.PromptSchemaTemplate template = template();
        DossierPendingJob job = new DossierPendingJob(
                "job-patterns", 31L, 401L, "workspace-mois", "page-pattern-extraction", Map.of("productKey", "401"),
                template);
        String rawRequest = "{\"model\":\"gpt-5.2\",\"service_tier\":\"flex\"}";
        String rawResponse = "{\"id\":\"resp_1\",\"output_text\":\"{\\\"status\\\":\\\"ok\\\"}\"}";
        StageResult result = StageResult.doneWithOpenAiInteractions(
                Map.of("page-pattern-extraction", Map.of("status", "DONE")),
                List.of(),
                List.of(new OpenAiInteraction(rawRequest, rawResponse, 30, 40, new BigDecimal("0.02"), "gpt-5.2", null)));
        when(backendClient.pending(eq("page-pattern-extraction"), any(DossierPendingRequest.class)))
                .thenReturn(new DossierPendingResponse(true, List.of(job)));
        when(pipelineWorker.execute(any(StageContext.class))).thenReturn(result);

        runner.runSalesPagePatternsV1Cycle();

        InOrder order = inOrder(backendClient, pipelineWorker);
        order.verify(backendClient).recebeRequest(eq("page-pattern-extraction"), eq("401"), eq("job-patterns"), any(DossierRecebeRequestRequest.class));
        order.verify(pipelineWorker).execute(any(StageContext.class));
        ArgumentCaptor<DossierRecebeRequestRequest> requestCaptor = ArgumentCaptor.forClass(DossierRecebeRequestRequest.class);
        order.verify(backendClient).recebeRequest(eq("page-pattern-extraction"), eq("401"), eq("job-patterns"), requestCaptor.capture());
        ArgumentCaptor<DossierRecebeResponseRequest> responseCaptor = ArgumentCaptor.forClass(DossierRecebeResponseRequest.class);
        order.verify(backendClient).recebeResponse(eq("page-pattern-extraction"), eq("401"), eq("job-patterns"), responseCaptor.capture());

        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().plataforma()).isEqualTo("openai");
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().request()).isEqualTo(rawRequest);
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().promptTemplateKey()).isEqualTo(template.templateKey());
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().schema()).isEqualTo(template.schemaJson());
        org.assertj.core.api.Assertions.assertThat(responseCaptor.getValue().response()).isEqualTo(rawResponse);
        org.assertj.core.api.Assertions.assertThat(responseCaptor.getValue().promptTemplateVersion()).isEqualTo(template.version());
        org.assertj.core.api.Assertions.assertThat(responseCaptor.getValue().schemaName()).isEqualTo(template.schemaName());
        org.assertj.core.api.Assertions.assertThat(responseCaptor.getValue().custo()).isEqualByComparingTo("0.02");
    }

    /** Monta um template canônico para validar propagação de auditoria. */
    private StageContext.PromptSchemaTemplate template() {
        return new StageContext.PromptSchemaTemplate(
                "mois-sales-library:salespagepatterns.v1:page-pattern-extraction:v1",
                "salespagepatterns.v1",
                "page-pattern-extraction",
                "v1",
                "gpt-5.2",
                "sales_page_patterns_v1",
                "prompt banco",
                "{\"type\":\"object\"}");
    }
}
