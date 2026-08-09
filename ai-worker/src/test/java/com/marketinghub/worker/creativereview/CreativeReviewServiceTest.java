package com.marketinghub.worker.creativereview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar o gate multimodal e seu callback auditável sem acessar serviços reais. */
class CreativeReviewServiceTest {

    /** Confirma que uma decisão estruturada é reportada com scores e auditoria técnica. */
    @Test
    void reportsApprovedMultimodalReview() throws Exception {
        CreativeReviewBackendClient backend = mock(CreativeReviewBackendClient.class);
        CreativeReviewOpenAiClient openAi = mock(CreativeReviewOpenAiClient.class);
        CreativeReviewService service = new CreativeReviewService(backend, openAi);
        Map<String, Object> pending = Map.of("creativeId", 85L, "mediaUrl", "https://cdn.test/ad.png");
        var result = new ObjectMapper().readTree("""
                {"decision":"APPROVED","attentionScore":84,"clarityScore":90,"desireScore":78,
                "credibilityScore":76,"actionScore":88,"copyAssessment":"Copy forte",
                "commercialAestheticAssessment":"Design premium","destinationIntegrationAssessment":"Integração coerente",
                "summary":"Pronto","issues":[],"recommendations":[],
                "mandatoryVisualRequirements":[],"forbiddenVisualElements":[],"visualAcceptanceCriteria":[]}
                """);
        when(backend.listPending(3)).thenReturn(List.of(pending));
        when(openAi.review(pending)).thenReturn(
                new CreativeReviewOpenAiClient.ReviewExecution(
                        "gpt-test", "{request}", "{response}", result, 20, 10, java.math.BigDecimal.valueOf(0.01)));

        CreativeReviewService.Summary summary = service.processPending(3);

        assertThat(summary.success()).isEqualTo(1);
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(backend).report(eq(85L), payload.capture());
        assertThat(payload.getValue()).containsEntry("decision", "APPROVED");
        assertThat(payload.getValue()).containsEntry("attentionScore", 84);
        assertThat(payload.getValue()).containsEntry("destinationIntegrationAssessment", "Integração coerente");
        assertThat(payload.getValue()).containsEntry("requestJson", "{request}");
        assertThat(payload.getValue()).containsEntry("responseJson", "{response}");
        assertThat(payload.getValue()).containsKey("mandatoryVisualRequirements");
    }

    /** Confirma que falha técnica nunca abre o gate e é reportada como FAILED. */
    @Test
    void reportsFailedReviewAndKeepsGateClosed() {
        CreativeReviewBackendClient backend = mock(CreativeReviewBackendClient.class);
        CreativeReviewOpenAiClient openAi = mock(CreativeReviewOpenAiClient.class);
        CreativeReviewService service = new CreativeReviewService(backend, openAi);
        Map<String, Object> pending = Map.of("creativeId", 86L, "mediaUrl", "https://cdn.test/ad.png");
        when(backend.listPending(3)).thenReturn(List.of(pending));
        when(openAi.review(pending)).thenThrow(new IllegalStateException("modelo indisponível"));

        CreativeReviewService.Summary summary = service.processPending(3);

        assertThat(summary.failed()).isEqualTo(1);
        verify(backend).report(86L, Map.of("decision", "FAILED", "error", "modelo indisponível"));
    }
}
