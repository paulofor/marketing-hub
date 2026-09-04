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
        ApolloTechnicalVideoQualityGate gate = gate(false);

        ProviderArtifacts result = gate.validate(job(), artifacts());

        assertThat(result.metadata().get("apollo_technical_quality").toString())
                .contains("stability_status=APPROVED")
                .contains("measured_frames=12")
                .contains("method=FFMPEG_VIDSTAB_GLOBAL_MOTION_DELTA");
        assertThat(result.auditFiles()).hasSize(1);
        assertThat(result.auditFiles().getFirst().role()).isEqualTo(ProviderAssetRole.AUDIO_AUDIT);
    }

    /** Reprova um salto abrupto que reproduz a percepção de imagem tremida. */
    @Test
    void shouldRejectAbruptMotionSpike() throws Exception {
        ApolloTechnicalVideoQualityGate gate = gate(true);

        assertThatThrownBy(() -> gate.validate(job(), artifacts()))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "APOLLO_VIDEO_STABILITY_REJECTED")
                .hasMessageContaining("pico");
    }

    /** Configura um ffmpeg simulado que gera movimentos globais estáveis ou com salto. */
    private ApolloTechnicalVideoQualityGate gate(boolean shaky) throws Exception {
        Path script = Files.createTempFile("fake-ffmpeg-stability", ".sh");
        String motions = shaky
                ? "0 0 0 0 0 1\\n0 0.1 0 0 0 1\\n0 0.2 0 0 0 1\\n0 0.3 0 0 0 1\\n0 0.4 0 0 0 1\\n0 0.5 0 0 0 1\\n0 0.6 0 0 0 1\\n0 0.7 0 0 0 1\\n0 0.8 0 0 0 1\\n0 35 0 0 0 1\\n0 35.1 0 0 0 1\\n0 35.2 0 0 0 1"
                : "0 0 0 0 0 1\\n0 0.1 0 0 0 1\\n0 0.2 0 0 0 1\\n0 0.3 0 0 0 1\\n0 0.4 0 0 0 1\\n0 0.5 0 0 0 1\\n0 0.6 0 0 0 1\\n0 0.7 0 0 0 1\\n0 0.8 0 0 0 1\\n0 0.9 0 0 0 1\\n0 1.0 0 0 0 1\\n0 1.1 0 0 0 1";
        Files.writeString(script, """
                #!/bin/sh
                for argument in "$@"; do
                  case "$argument" in
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
                """.formatted(motions));
        script.toFile().setExecutable(true);
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getPostProduction().setFfmpegPath(script.toString());
        return new ApolloTechnicalVideoQualityGate(new ObjectMapper(), properties);
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
