package com.marketinghub.worker.pipeline.gerasalespagev1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.marketinghub.worker.pipeline.ArtifactStore;
import com.marketinghub.worker.pipeline.StageArtifact;
import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageExecution;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: comprovar mídia aprovada, reprodução opcional e auditoria da página do kit. */
class GeraSalesPageReviewedVideoTest {
    private final ObjectMapper json = new ObjectMapper();

    /** Produto sem recibo mantém o HTML legado exatamente como foi recebido. */
    @Test
    void preservesLegacyWithoutReviewedVideo() {
        assertThat(GeraSalesPageReviewedVideo.render("<body>Oferta</body>", Map.of(), json)).isEqualTo("<body>Oferta</body>");
    }

    /** Duas identidades conservam seus arquivos, texto escapado e CTA antes do vídeo. */
    @ParameterizedTest
    @ValueSource(longs = {75, 975})
    void rendersExactVideoWithoutDuplicateOrAutoplay(long assetId) {
        var data = source(assetId);
        String html = "<html><body><main><section><a href='https://checkout.invalid'>Comprar</a></section></main></body></html>";
        String rendered = GeraSalesPageReviewedVideo.render(html, data, json);
        assertThat(rendered).contains("data-video-asset-id=\"" + assetId + "\"", "https://media.invalid/" + assetId + ".mp4")
                .contains("controls", "playsinline", "preload=\"metadata\"", "&lt;script&gt;")
                .doesNotContain("autoplay", "<script>malicious");
        assertThat(rendered.indexOf("Comprar")).isLessThan(rendered.indexOf("data-mh-reviewed-video"));
        assertThat(GeraSalesPageReviewedVideo.render(rendered, data, json)).isEqualTo(rendered);
        var changed = source(assetId + 1);
        assertThat(GeraSalesPageReviewedVideo.render(rendered, changed, json)).doesNotContain("https://media.invalid/" + assetId + ".mp4");
    }

    /** Resposta sem demonstração é enriquecida antes da revisão e da publicação sem nova inferência. */
    @ParameterizedTest
    @ValueSource(strings = {"sales-page-html", "sales-page-publication-package"})
    void processorPreservesRawCostAndSelectedMedia(String stage) {
        var ai = mock(OpenAiClientPort.class);
        var backend = mock(GeraSalesPageBackendClient.class);
        var processor = new GeraSalesPageProcessor(json, ai, new GeraSalesPageResponseValidator(json), backend, "flex");
        String raw = "{\"html\":\"<html><body><main>Oferta útil</main></body></html>\"}";
        var dispatch = new OpenAiDispatch("simulated", "prompt", "{}", "{}", "prompt", Instant.now());
        when(ai.dispatch(any())).thenReturn(dispatch);
        when(ai.awaitResult(dispatch)).thenReturn(new OpenAiResult<>("simulated", "raw-preserved", raw, raw, 17, 23, new BigDecimal("0.01")));
        var context = context(stage, source(75));
        var result = processor.process(context);
        assertThat(result.output().payload().get("html").toString()).contains("data-video-asset-id=\"75\"", "https://media.invalid/75.mp4");
        var audited = (OpenAiResult<?>) result.metrics().get("openAiResult");
        assertThat(audited.rawResponse()).isEqualTo("raw-preserved");
        assertThat(audited.costUsd()).isEqualByComparingTo("0.01");
        verify(ai, times(1)).dispatch(any());
    }

    /** Fonte incompleta ou não aprovada falha antes do modelo, sem cobrar uma tentativa inútil. */
    @Test
    void rejectsInvalidVideoBeforeModel() {
        var ai = mock(OpenAiClientPort.class);
        var processor = new GeraSalesPageProcessor(json, ai, new GeraSalesPageResponseValidator(json), mock(GeraSalesPageBackendClient.class), "flex");
        assertThatThrownBy(() -> processor.process(context("sales-page-html", Map.of("experiment", Map.of("approvedLandingVideo", Map.of("assetId", 75))))))
                .hasMessageContaining("demonstração selecionada");
        verifyNoInteractions(ai);
    }

    /** Monta o contexto de execução real com armazenamento local simulado e sem rede. */
    private StageContext<GeraSalesPageInput> context(String stage, Map<String, Object> data) {
        var input = new GeraSalesPageInput(105L, stage, "local", "model-test", "schema-test", "prompt", "{}", data);
        var execution = new StageExecution<>("local", 105L, stage, "PENDING", Instant.now(), input, Map.of());
        ArtifactStore store = (type, name, contentType, content, metadata) -> new StageArtifact(type, name, contentType, "local/" + name, "test-hash", metadata);
        return new StageContext<>(execution, input, store, Map.of());
    }

    /** Fornece mídia aprovada com legenda maliciosa apenas para testar escape, sem credenciais. */
    private Map<String, Object> source(long id) {
        return Map.of("experiment", Map.of("approvedLandingVideo", Map.of("assetId", id, "assetUrl", "https://media.invalid/" + id + ".mp4",
                "role", "LANDING_HERO", "reviewStatus", "APPROVED", "sha256", "f".repeat(64), "captionsBurnedIn", true, "captions", "<script>malicious</script> Demonstração útil")));
    }
}
