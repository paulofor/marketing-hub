package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a restauração segura do contrato Product UGC na pós-produção. */
@ExtendWith(MockitoExtension.class)
class ProductUgcPostProductionContractResolverTest {
    @Mock private BackendVideoClient backendClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ProductUgcPostProductionContractResolver resolver;

    /** Inicializa o resolvedor com acesso simulado ao backend. */
    @BeforeEach
    void setUp() {
        resolver = new ProductUgcPostProductionContractResolver(backendClient, objectMapper);
    }

    /** Recupera duração e cortes auditados quando o job filho herdou o contrato antigo. */
    @Test
    void shouldRestoreCanonicalSourceContractForPostProduction() throws Exception {
        SalesVideoJob downstream = job(
                21233L,
                SalesVideoJobType.POST_PRODUCTION,
                "MUSA_POST_PRODUCTION",
                """
                {
                  "videoProductionCycleId":11,
                  "videoProjectId":3,
                  "productId":4,
                  "experimentId":91,
                  "sourceJobId":21232,
                  "generation_strategy":"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION",
                  "technicalQualityGate":{"continuousTakeRequired":true}
                }
                """);
        SalesVideoJob source = job(
                21232L,
                SalesVideoJobType.RENDER,
                "RUNWAY_PRODUCT_UGC",
                """
                {
                  "videoProductionCycleId":11,
                  "videoProjectId":3,
                  "productId":4,
                  "experimentId":91,
                  "generation_strategy":"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION",
                  "targetDurationSeconds":15,
                  "sceneCount":1,
                  "assemblyRequired":false,
                  "technicalQualityGate":{
                    "continuousTakeRequired":true,
                    "maximumMeanMotionDelta":1.25,
                    "maximumPeakMotionDelta":12.0
                  },
                  "apollo_technical_quality":{
                    "stability_status":"APPROVED",
                    "continuous_take":false,
                    "intentional_scene_cuts_allowed":true,
                    "maximum_scene_cuts":4,
                    "method":"FFMPEG_SCENE_AWARE_VIDSTAB_GLOBAL_MOTION_DELTA"
                  }
                }
                """);
        when(backendClient.fetchJob(21232L)).thenReturn(source);

        SalesVideoJob result = resolver.resolve(downstream);
        JsonNode metadata = objectMapper.readTree(result.metadataJson());

        assertThat(metadata.path("targetDurationSeconds").asInt()).isEqualTo(15);
        assertThat(metadata.path("technicalQualityGate").path("continuousTakeRequired").asBoolean())
                .isFalse();
        assertThat(metadata.path("technicalQualityGate").path("intentionalSceneCutsAllowed").asBoolean())
                .isTrue();
        assertThat(metadata.path("technicalQualityGate").path("maximumSceneCuts").asInt())
                .isEqualTo(4);
        assertThat(metadata.path("source_contract_hydration").path("source_job_id").asLong())
                .isEqualTo(21232L);
    }

    /** Bloqueia job filho que aponta para experimento diferente do contrato fonte. */
    @Test
    void shouldRejectDivergentSourceLineage() {
        SalesVideoJob downstream = job(
                21233L,
                SalesVideoJobType.POST_PRODUCTION,
                "MUSA_POST_PRODUCTION",
                """
                {"experimentId":91,"sourceJobId":21232,
                 "generation_strategy":"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION"}
                """);
        SalesVideoJob source = job(
                21232L,
                SalesVideoJobType.RENDER,
                "RUNWAY_PRODUCT_UGC",
                """
                {"experimentId":90,"targetDurationSeconds":15,
                 "generation_strategy":"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION",
                 "apollo_technical_quality":{"stability_status":"APPROVED",
                  "method":"FFMPEG_SCENE_AWARE_VIDSTAB_GLOBAL_MOTION_DELTA"}}
                """);
        when(backendClient.fetchJob(21232L)).thenReturn(source);

        assertThatThrownBy(() -> resolver.resolve(downstream))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue(
                        "code", "APOLLO_POST_PRODUCTION_CONTRACT_INVALID")
                .hasMessageContaining("experimentId");
    }

    /** Mantém jobs que não pertencem à pós-produção Product UGC inalterados. */
    @Test
    void shouldIgnoreOtherStrategies() {
        SalesVideoJob job = job(
                10L,
                SalesVideoJobType.RENDER,
                "LUMA_RAY_3_2",
                "{\"generation_strategy\":\"SCENE_BY_SCENE_MONTAGE\"}");

        assertThat(resolver.resolve(job)).isSameAs(job);
    }

    /** Mantém render comum sem metadados fora da hidratação exclusiva da pós-produção. */
    @Test
    void shouldIgnoreRenderWithoutMetadata() {
        SalesVideoJob render = job(11L, SalesVideoJobType.RENDER, "RUNWAY", null);

        assertThat(resolver.resolve(render)).isSameAs(render);
    }

    /** Cria um job imutável mínimo para os cenários de contrato. */
    private SalesVideoJob job(Long id, SalesVideoJobType type, String provider, String metadata) {
        Instant now = Instant.parse("2026-09-05T06:00:00Z");
        return new SalesVideoJob(
                id,
                57L,
                558L,
                "default",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                provider,
                null,
                type,
                SalesVideoStatus.VIDEO_REQUESTED,
                1,
                null,
                null,
                null,
                0,
                null,
                null,
                "operator@marketinghub.io",
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                metadata,
                now,
                now);
    }
}
