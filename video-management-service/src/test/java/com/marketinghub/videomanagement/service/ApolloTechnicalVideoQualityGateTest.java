package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.ProviderArtifacts;
import com.marketinghub.videomanagement.service.provider.ProviderAssetRole;
import com.marketinghub.videomanagement.service.provider.ProviderFile;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Responsabilidade: comprovar o bloqueio técnico de tremor exigido por Apolo. */
class ApolloTechnicalVideoQualityGateTest {

    /** Aprova uma tomada cuja mudança de movimento permanece abaixo dos dois limites. */
    @Test
    void shouldApproveStableContinuousTake() throws Exception {
        ApolloTechnicalVideoQualityGate gate = gate(false, false);

        ProviderArtifacts result = gate.validate(job(), artifacts());

        assertThat(result.metadata().get("apollo_technical_quality").toString())
                .contains("stability_status=APPROVED")
                .contains("measured_frames=12")
                .contains("method=FFMPEG_SCENE_AWARE_VIDSTAB_GLOBAL_MOTION_DELTA");
        assertThat(result.auditFiles()).hasSize(1);
        assertThat(result.auditFiles().getFirst().role()).isEqualTo(ProviderAssetRole.AUDIO_AUDIT);
    }

    /** Reprova um salto abrupto que reproduz a percepção de imagem tremida. */
    @Test
    void shouldRejectAbruptMotionSpike() throws Exception {
        ApolloTechnicalVideoQualityGate gate = gate(true, false);

        assertThatThrownBy(() -> gate.validate(job(), artifacts()))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "APOLLO_VIDEO_STABILITY_REJECTED")
                .hasMessageContaining("pico");
    }

    /** Aprova cortes editoriais limitados sem contar a transição como tremor de câmera. */
    @Test
    void shouldMeasureStabilityInsideIntentionalShots() throws Exception {
        ApolloTechnicalVideoQualityGate gate = gate(true, true);

        ProviderArtifacts result = gate.validate(editorialJob(4), artifacts());

        assertThat(result.metadata().get("apollo_technical_quality").toString())
                .contains("stability_status=APPROVED")
                .contains("continuous_take=false")
                .contains("detected_scene_cuts=1")
                .contains("excluded_transition_deltas=2");
    }

    /** Mantém tomada única como contrato estrito quando essa política for solicitada. */
    @Test
    void shouldRejectSceneCutWhenContinuousTakeIsRequired() throws Exception {
        ApolloTechnicalVideoQualityGate gate = gate(false, true);

        assertThatThrownBy(() -> gate.validate(job(), artifacts()))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "APOLLO_VIDEO_CONTINUITY_REJECTED")
                .hasMessageContaining("1 corte");
    }

    /** Reprova montagem que excede a quantidade de transições explicitamente autorizada. */
    @Test
    void shouldRejectSceneCutsBeyondPolicyLimit() throws Exception {
        ApolloTechnicalVideoQualityGate gate = gate(false, true);

        assertThatThrownBy(() -> gate.validate(editorialJob(0), artifacts()))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "APOLLO_VIDEO_SCENE_CUTS_REJECTED")
                .hasMessageContaining("máximo 0");
    }

    /** Interpreta somente os cabeçalhos de quadro emitidos pelo scdet. */
    @Test
    void shouldParseSceneCutFrames() throws Exception {
        ApolloTechnicalVideoQualityGate gate = gate(false, false);

        assertThat(gate.parseSceneCutFrames(List.of(
                        "frame:132  pts:67584 pts_time:5.5",
                        "lavfi.scd.time=5.5",
                        "frame:258  pts:132096 pts_time:10.75")))
                .containsExactly(132, 258);
    }

    /** Configura um ffmpeg simulado que gera movimentos globais e um corte opcional. */
    private ApolloTechnicalVideoQualityGate gate(boolean shaky, boolean sceneCut) throws Exception {
        Path script = Files.createTempFile("fake-ffmpeg-stability", ".sh");
        String motions = shaky
                ? "0 0 0 0 0 1\\n0 0.1 0 0 0 1\\n0 0.2 0 0 0 1\\n0 0.3 0 0 0 1\\n0 0.4 0 0 0 1\\n0 0.5 0 0 0 1\\n0 0.6 0 0 0 1\\n0 0.7 0 0 0 1\\n0 0.8 0 0 0 1\\n0 35 0 0 0 1\\n0 35.1 0 0 0 1\\n0 35.2 0 0 0 1"
                : "0 0 0 0 0 1\\n0 0.1 0 0 0 1\\n0 0.2 0 0 0 1\\n0 0.3 0 0 0 1\\n0 0.4 0 0 0 1\\n0 0.5 0 0 0 1\\n0 0.6 0 0 0 1\\n0 0.7 0 0 0 1\\n0 0.8 0 0 0 1\\n0 0.9 0 0 0 1\\n0 1.0 0 0 0 1\\n0 1.1 0 0 0 1";
        Files.writeString(script, """
                #!/bin/sh
                for argument in "$@"; do
                  case "$argument" in
                    *scdet=*file=*)
                      result="${argument##*file=}"
                      %s
                      ;;
                    vidstabdetect=result=*)
                      result="${argument#vidstabdetect=result=}"
                      result="${result%%%%:*}"
                      : > "$result"
                      ;;
                    vidstabtransform=*)
                      printf '%%b\n' '%s' > global_motions.trf
                      ;;
                  esac
                done
                exit 0
                """.formatted(
                        sceneCut
                                ? "printf 'frame:9  pts:4608 pts_time:0.375\\nlavfi.scd.time=0.375\\n' > \"$result\""
                                : ": > \"$result\"",
                        motions));
        script.toFile().setExecutable(true);
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getPostProduction().setFfmpegPath(script.toString());
        return new ApolloTechnicalVideoQualityGate(new ObjectMapper(), properties);
    }

    /** Cria o contrato Product UGC que aceita poucos cortes editoriais entre planos estáveis. */
    private SalesVideoJob editorialJob(int maximumSceneCuts) {
        SalesVideoJob source = job();
        return new SalesVideoJob(
                source.id(), source.profileId(), source.scriptId(), source.tenantId(),
                source.providerFamily(), source.providerName(), source.providerJobId(),
                source.jobType(), source.status(), source.retryAttempt(), source.retryReason(),
                source.retryOfJobId(), source.retryNotes(), source.progressPercent(), source.failureCode(),
                source.failureDetail(), source.requestedBy(), source.requestedAt(), source.startedAt(),
                source.finishedAt(), source.expiresAt(), source.assetId(), source.posterAssetId(),
                source.vttAssetId(),
                """
                        {"technicalQualityGate":{
                          "continuousTakeRequired":false,
                          "intentionalSceneCutsAllowed":true,
                          "maximumSceneCuts":%d,
                          "maximumMeanMotionDelta":1.25,
                          "maximumPeakMotionDelta":12.0
                        }}
                        """.formatted(maximumSceneCuts),
                source.createdAt(), source.updatedAt());
    }

    /** Cria um job com os limites homologados para a tomada Product UGC. */
    private SalesVideoJob job() {
        return new SalesVideoJob(
                91L,
                57L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "RUNWAY_PRODUCT_UGC",
                null,
                SalesVideoJobType.RENDER,
                SalesVideoStatus.VIDEO_PROCESSING,
                1,
                null,
                null,
                null,
                50,
                null,
                null,
                "Apolo",
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                """
                        {"technicalQualityGate":{
                          "continuousTakeRequired":true,
                          "maximumMeanMotionDelta":1.25,
                          "maximumPeakMotionDelta":12.0
                        }}
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um MP4 mínimo, pois o ffmpeg do teste apenas simula a análise. */
    private ProviderArtifacts artifacts() {
        ProviderFile video = new ProviderFile(
                "ugc.mp4",
                MediaType.valueOf("video/mp4"),
                AssetType.VIDEO,
                ProviderAssetRole.VIDEO,
                new byte[] {0, 0, 0, 32, 'f', 't', 'y', 'p'});
        ProviderFile auditAudio = new ProviderFile(
                "voice.mp3",
                MediaType.valueOf("audio/mpeg"),
                AssetType.AUDIO,
                ProviderAssetRole.AUDIO_AUDIT,
                new byte[] {1, 2, 3});
        return new ProviderArtifacts(
                "ugc-91", video, null, null, Map.of("provider", "RUNWAY"), List.of(auditAudio));
    }
}
