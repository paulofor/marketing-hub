package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoScriptStatus;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar a pós-produção local de vídeos de venda. */
class PostProductionVideoProviderTest {
    private MockWebServer server;
    private Path ffmpegArguments;
    private double narrationSegmentDurationSeconds = 4.0;

    /** Inicializa o servidor HTTP usado para entregar o MP4 fonte. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o servidor HTTP após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve baixar o vídeo bruto e devolver MP4 final com legenda VTT e metadados comerciais. */
    @Test
    void shouldPostProduceVideoWithVoiceCaptionsAndMusicMetadata() throws Exception {
        server.enqueue(mp4Response());
        VideoManagementProperties properties = properties();
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(provider.supports(job())).isTrue();
        assertThat(artifacts.providerJobId()).isEqualTo("post-production-55");
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.videoFile().fileName()).isEqualTo("sales-video-55-musa-final.mp4");
        assertThat(artifacts.captionFile().assetType()).isEqualTo(AssetType.CAPTION);
        assertThat(new String(artifacts.captionFile().content()))
                .contains("WEBVTT", "Pare de se sentir comum", "00:00:12.000 --> 00:00:24.000")
                .doesNotContain("|");
        assertThat(artifacts.metadata())
                .containsEntry("provider", "MUSA_POST_PRODUCTION")
                .containsEntry("duration_seconds", 24.0)
                .containsEntry("has_audio", true)
                .containsEntry("audio_streams", 1)
                .containsEntry("cta_text", "Ver meu plano MUSA")
                .containsKey("audio")
                .containsKey("captions");
        assertThat(artifacts.metadata().get("captions").toString())
                .contains("cue_count=2", "timed=true");
        assertThat(artifacts.metadata().get("audio").toString())
                .contains("BLOCKED_FOR_CAMPAIGN", "synthetic_local");
        assertThat(Files.readString(ffmpegArguments))
                .contains(
                        "-t\n24\n",
                        "-preset\nveryfast\n",
                        "-movflags\n+faststart\n",
                        "loudnorm=I=-17:TP=-2:LRA=7")
                .doesNotContain("apad");
        assertThat(server.takeRequest().getPath()).isEqualTo("/source/musa.mp4");
    }

    /** Deve priorizar OpenAI TTS para evitar voz robótica quando a chave estiver configurada. */
    @Test
    void shouldUseOpenAiTtsWhenConfigured() throws Exception {
        server.enqueue(mp4Response());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "audio/mpeg")
                .setBody(new Buffer().write(new byte[] {1, 2, 3, 4})));
        VideoManagementProperties properties = properties();
        properties.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
        properties.getProviders().getPostProduction().setOpenAiApiKey("test-key");
        properties.getProviders().getPostProduction().setOpenAiBaseUrl(URI.create(server.url("/v1").toString()));
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(artifacts.metadata().get("audio").toString())
                .contains(
                        "OPENAI_TTS",
                        "natural_tts_candidate",
                        "ai_generated_disclosure=true",
                        "Apresentadora e voz geradas por IA")
                .doesNotContain("synthetic_local");
        assertThat(artifacts.metadata().get("synthetic_media_disclosure").toString())
                .contains(
                        "required=true",
                        "presenter_synthetic=true",
                        "voice_synthetic=true",
                        "Apresentadora e voz geradas por IA");
        assertThat(artifacts.auditFiles()).singleElement().satisfies(file -> {
            assertThat(file.assetType()).isEqualTo(AssetType.AUDIO);
            assertThat(file.role()).isEqualTo(ProviderAssetRole.AUDIO_AUDIT);
            assertThat(file.fileName()).isEqualTo("sales-video-55-tts-segment-01.mp3");
            assertThat(file.content()).containsExactly(1, 2, 3, 4);
        });
        assertThat(artifacts.metadata().get("tts_interactions").toString())
                .contains(
                        "raw_request",
                        "BINARY_AUDIT_ASSET",
                        "9f64a747e1b97f131fabb6b447296c9b6f0201e79fb3c5356e6c77e89b6a806a",
                        "PENDING_PROVIDER_RECONCILIATION",
                        "NOT_SUPPORTED_BY_AUDIO_SPEECH_ENDPOINT")
                .doesNotContain("test-key");
        assertThat(artifacts.metadata())
                .containsEntry("tts_cost_reconciliation_status", "PENDING_PROVIDER_RECONCILIATION");
        assertThat(server.takeRequest().getPath()).isEqualTo("/source/musa.mp4");
        var openAiRequest = server.takeRequest();
        assertThat(openAiRequest.getPath()).isEqualTo("/v1/audio/speech");
        assertThat(openAiRequest.getHeader("Authorization")).isEqualTo("Bearer test-key");
        assertThat(openAiRequest.getBody().readUtf8())
                .contains(
                        "gpt-4o-mini-tts-2025-12-15",
                        "marin",
                        "Você se arruma",
                        "ritmo de anúncio mobile");
    }

    /** Preserva request e resposta textual quando o provedor de voz recusa a chamada. */
    @Test
    void shouldAuditOpenAiTtsFailureWithoutRetrying() throws Exception {
        server.enqueue(mp4Response());
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"rate limited\"}}"));
        VideoManagementProperties properties = properties();
        properties.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
        properties.getProviders().getPostProduction().setOpenAiApiKey("test-key");
        properties.getProviders().getPostProduction().setOpenAiBaseUrl(URI.create(server.url("/v1").toString()));
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(job(), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "OPENAI_TTS_FAILED")
                .hasMessageContaining("status=429")
                .hasMessageContaining("rawRequest=")
                .hasMessageContaining("rate limited")
                .hasMessageNotContaining("test-key");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    /** Deve aplicar legenda sem exigir TTS quando a pós-produção não pedir voz off. */
    @Test
    void shouldPostProduceCaptionOnlyVideoWithoutVoiceOver() throws Exception {
        server.enqueue(mp4Response());
        VideoManagementProperties properties = properties();
        properties.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(captionOnlyJob(), profile(), (percent, status, message) -> { });

        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(new String(artifacts.captionFile().content())).contains("WEBVTT", "Legenda grande");
        assertThat(artifacts.metadata())
                .containsEntry("post_production_mode", "CAPTION_ONLY")
                .containsEntry("has_audio", false)
                .containsEntry("audio_streams", 0)
                .containsKey("audio");
        assertThat(artifacts.metadata().get("audio").toString())
                .contains("voice_over=false", "mode=CAPTION_ONLY", "NOT_REQUESTED")
                .doesNotContain("OPENAI_TTS", "ESPEAK_NG");
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getPath()).isEqualTo("/source/musa.mp4");
    }

    /** Reprova texto divergente antes de baixar mídia ou consumir um provedor de voz. */
    @Test
    void shouldRejectCaptionThatDoesNotMatchNarration() throws Exception {
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(
                        governedTextJob(false), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "APOLLO_CAPTION_NARRATION_MISMATCH");
        assertThat(server.getRequestCount()).isZero();
    }

    /** Aprova pontuação e pausas diferentes quando a sequência de palavras é idêntica. */
    @Test
    void shouldApproveTheSameNormalizedCaptionAndNarration() throws Exception {
        server.enqueue(mp4Response());
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(
                governedTextJob(true), profile(), (percent, status, message) -> { });

        assertThat(artifacts.metadata().get("caption_narration_sync").toString())
                .contains(
                        "APPROVED",
                        "NORMALIZED_EXACT_SEQUENCE",
                        "word_count=10",
                        "timing_status=APPROVED",
                        "timing_method=SEGMENT_AUDIO_DURATION",
                        "segment_count=2",
                        "synthetic_music_bed=false");
        assertThat(new String(artifacts.captionFile().content()))
                .contains(
                        "00:00:00.000 --> 00:00:04.000",
                        "00:00:04.000 --> 00:00:24.000");
        assertThat(Files.readString(ffmpegArguments))
                .contains("concat=n=2:v=0:a=1[aout]")
                .doesNotContain("sine=frequency=220");
    }

    /** Gera cada frase premium em uma chamada de voz para medir seus limites reais. */
    @Test
    void shouldGenerateOneNaturalVoiceSegmentForEachCaptionCue() throws Exception {
        server.enqueue(mp4Response());
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "audio/mpeg")
                .setBody(new Buffer().write(new byte[] {1, 2, 3, 4})));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "audio/mpeg")
                .setBody(new Buffer().write(new byte[] {5, 6, 7, 8})));
        VideoManagementProperties properties = properties();
        properties.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
        properties.getProviders().getPostProduction().setOpenAiApiKey("test-key");
        properties.getProviders().getPostProduction().setOpenAiBaseUrl(URI.create(server.url("/").toString()));
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties, new ObjectMapper(), WebClient.builder());

        ProviderArtifacts artifacts = provider.render(
                governedTextJob(true), profile(), (percent, status, message) -> { });

        assertThat(server.takeRequest().getPath()).isEqualTo("/source/musa.mp4");
        var firstSegment = server.takeRequest();
        var secondSegment = server.takeRequest();
        assertThat(firstSegment.getPath()).isEqualTo("/audio/speech");
        assertThat(firstSegment.getBody().readUtf8()).contains("Você se arruma, mas falta presença");
        assertThat(secondSegment.getPath()).isEqualTo("/audio/speech");
        assertThat(secondSegment.getBody().readUtf8()).contains("Faça o diagnóstico gratuito");
        assertThat(artifacts.metadata().get("audio").toString())
                .contains("OPENAI_TTS", "music=none");
        assertThat(artifacts.auditFiles())
                .extracting(ProviderFile::fileName)
                .containsExactly(
                        "sales-video-55-tts-segment-01.mp3",
                        "sales-video-55-tts-segment-02.mp3");
        assertThat(artifacts.metadata().get("tts_interactions").toString())
                .contains("segment_index=1", "segment_index=2", "output_duration_seconds=4.0");
    }

    /** Reprova copy cuja voz medida seria truncada pelo fim do vídeo. */
    @Test
    void shouldRejectNarrationLongerThanTheVideo() throws Exception {
        narrationSegmentDurationSeconds = 13.0;
        server.enqueue(mp4Response());
        PostProductionVideoProvider provider =
                new PostProductionVideoProvider(properties(), new ObjectMapper(), WebClient.builder());

        assertThatThrownBy(() -> provider.render(
                        governedTextJob(true), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "APOLLO_NARRATION_DURATION_EXCEEDED")
                .hasMessageContaining("26.000s")
                .hasMessageContaining("24.000s");
    }

    /** Cria uma resposta MP4 mínima para o download fonte. */
    private MockResponse mp4Response() {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "video/mp4")
                .setBody(new Buffer().write(new byte[] {
                        0, 0, 0, 32, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 2, 0
                }));
    }

    /** Configura o provider com binários fake para manter o teste determinístico. */
    private VideoManagementProperties properties() throws Exception {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setBackendBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getPostProduction().setEnabled(true);
        properties.getProviders().getPostProduction().setEspeakPath(fakeEspeak().toString());
        properties.getProviders().getPostProduction().setFfmpegPath(fakeFfmpeg().toString());
        properties.getProviders().getPostProduction().setFfprobePath(fakeFfprobe().toString());
        properties.getProviders().getPostProduction().setFontFile("/tmp/fake-font.ttf");
        return properties;
    }

    /** Cria um script executável que simula voz off em WAV. */
    private Path fakeEspeak() throws Exception {
        Path script = Files.createTempFile("fake-espeak", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                output=""
                previous=""
                for arg in "$@"; do
                  if [ "$previous" = "-w" ]; then output="$arg"; fi
                  previous="$arg"
                done
                printf 'RIFF....WAVEfmt ' > "$output"
                exit 0
                """);
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um script executável que simula ffmpeg escrevendo MP4 final. */
    private Path fakeFfmpeg() throws Exception {
        ffmpegArguments = Files.createTempFile("fake-ffmpeg-post-arguments", ".txt");
        Path script = Files.createTempFile("fake-ffmpeg-post", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                printf '%%s\\n' "$@" >> '%s'
                output=""
                for arg in "$@"; do
                  output="$arg"
                done
                if [ "$output" = "-" ]; then
                  echo '    I:         -26.3 LUFS'
                  echo '    Peak:       -7.6 dBFS'
                  exit 0
                fi
                printf '\\000\\000\\000\\040ftypisom\\000\\000\\002\\000' > "$output"
                exit 0
                """.formatted(ffmpegArguments));
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um ffprobe fake que informa duração vertical de vinte e quatro segundos. */
    private Path fakeFfprobe() throws Exception {
        Path script = Files.createTempFile("fake-ffprobe-post", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                source=""
                for argument in "$@"; do source="$argument"; done
                case "$source" in
                  *voiceover*) printf '%f\n' ;;
                  *) printf '24.000000\n' ;;
                esac
                """.formatted(narrationSegmentDurationSeconds));
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um job de pós-produção com vídeo fonte e textos comerciais. */
    private SalesVideoJob job() {
        return new SalesVideoJob(
                55L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "MUSA_POST_PRODUCTION",
                null,
                SalesVideoJobType.POST_PRODUCTION,
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
                """
                        {
                          "artifactType": "experiment.videoPostProductionRequest.v1",
                          "experimentVideoAssetId": 5,
                          "sourceVideoUrl": "/source/musa.mp4",
                          "voiceOverScript": "Você se arruma e sente que falta presença. Veja seu plano MUSA.",
                          "captionText": "Pare de se sentir comum no espelho. | Veja seu plano MUSA de 7 dias.",
                          "referenceGovernance": {
                            "presenterIsSynthetic": true,
                            "presenterConsentEvidence": "Referência sintética aprovada; nenhuma pessoa real é representada."
                          },
                          "post_production": {"cta_text": "Ver meu plano MUSA"}
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um job de pós-produção apenas com legenda. */
    private SalesVideoJob captionOnlyJob() {
        return new SalesVideoJob(
                56L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "MUSA_POST_PRODUCTION",
                null,
                SalesVideoJobType.POST_PRODUCTION,
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
                """
                        {
                          "sourceVideoUrl": "/source/musa.mp4",
                          "captionText": "Legenda grande para mobile sem depender de voz off."
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria pós-produção governada com copy única ou propositalmente divergente. */
    private SalesVideoJob governedTextJob(boolean matching) {
        SalesVideoJob base = job();
        String narration = matching
                ? "Você se arruma, mas falta presença. Faça o diagnóstico gratuito!"
                : "Você se arruma e compre um produto diferente.";
        String metadata = """
                {
                  "sourceVideoUrl":"/source/musa.mp4",
                  "captionText":"Você se arruma, mas falta presença | Faça o diagnóstico gratuito",
                  "voiceOverScript":"%s",
                  "technicalQualityGate":{"captionMustMatchNarration":true},
                  "post_production":{"cta_text":"Faça o diagnóstico gratuito"}
                }
                """.formatted(narration);
        return new SalesVideoJob(
                base.id(), base.profileId(), base.scriptId(), base.tenantId(), base.providerFamily(),
                base.providerName(), base.providerJobId(), base.jobType(), base.status(), base.retryAttempt(),
                base.retryReason(), base.retryOfJobId(), base.retryNotes(), base.progressPercent(),
                base.failureCode(), base.failureDetail(), base.requestedBy(), base.requestedAt(), base.startedAt(),
                base.finishedAt(), base.expiresAt(), base.assetId(), base.posterAssetId(), base.vttAssetId(),
                metadata, base.createdAt(), base.updatedAt());
    }

    /** Cria um perfil mínimo para a execução do provider. */
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
                "HERO",
                "Título",
                "Persona",
                "Estilo",
                "Voz",
                "pt-BR",
                30,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}
