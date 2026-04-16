package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoScriptStatus;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubVideoProviderTest {

    private final StubVideoProvider provider = new StubVideoProvider();

    @Test
    void shouldGenerateArtifactsFromScript() {
        SalesVideoJob job = job();
        SalesVideoProfile profile = profile();
        AtomicInteger lastProgress = new AtomicInteger();

        ProviderArtifacts artifacts = provider.render(job, profile,
                (percent, status, message) -> lastProgress.set(percent != null ? percent : 0));

        assertThat(artifacts.videoFile()).isNotNull();
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.metadata()).containsKey("script_version");
        assertThat(lastProgress.get()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldFailWhenScriptMissing() {
        SalesVideoJob job = job();
        SalesVideoProfile profile = new SalesVideoProfile(
                2L,
                1L,
                null,
                "SHORT",
                "Título",
                null,
                null,
                null,
                "pt-BR",
                60,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                null,
                null);

        assertThatThrownBy(() -> provider.render(job, profile, (p, s, m) -> {}))
                .isInstanceOf(VideoProviderException.class);
    }

    private SalesVideoJob job() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "STUB",
                null,
                SalesVideoJobType.RENDER,
                SalesVideoStatus.VIDEO_REQUESTED,
                1,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());
    }

    private SalesVideoProfile profile() {
        SalesVideoScript script = new SalesVideoScript(
                10L,
                1,
                "script text",
                "hook",
                "cta",
                "caption",
                null,
                "MANUAL",
                "gpt",
                "prompt",
                SalesVideoScriptStatus.APPROVED,
                "user",
                Instant.now(),
                Instant.now());
        return new SalesVideoProfile(
                2L,
                1L,
                null,
                "SHORT",
                "Título",
                "Persona",
                "Estilo",
                "Voz",
                "pt-BR",
                60,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}
