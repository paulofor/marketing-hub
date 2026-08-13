package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.VideoAssetClient;
import com.marketinghub.videomanagement.client.dto.AssetResponse;
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
import java.time.Duration;
import java.time.Instant;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o adapter direto da Luma Ray 3.2 no módulo de vídeo. */
class LumaRayVideoProviderTest {
    private MockWebServer server;

    /** Inicializa a Luma Agents API simulada para capturar geração, polling e download. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra a Luma Agents API simulada após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve gerar três cenas Luma, baixar MP4s e devolver um vídeo final montado. */
    @Test
    void shouldRenderThreeLumaScenesAndAssembleFinalVideo() throws Exception {
        for (int i = 1; i <= 3; i++) {
            server.enqueue(json("{\"id\":\"generation-" + i + "\",\"state\":\"queued\"}"));
            server.enqueue(json("""
                    {
                      "id": "generation-%d",
                      "state": "completed",
                      "output": [{"url": "%s/download/scene-%d.mp4?X-Amz-Expires=3600&X-Amz-Signature=assinatura-%d"}],
                      "model": "ray-3.2"
                    }
                    """.formatted(i, server.url("/").toString().replaceAll("/$", ""), i, i)));
            server.enqueue(mp4Response());
        }
        LumaRayVideoProvider provider = new LumaRayVideoProvider(
                properties(),
                new ObjectMapper(),
                mock(VideoAssetClient.class),
                WebClient.builder());

        ProviderArtifacts artifacts = provider.render(job(), profile(), (percent, status, message) -> { });

        assertThat(provider.supports(job())).isTrue();
        assertThat(artifacts.providerJobId()).isEqualTo("generation-1,generation-2,generation-3");
        assertThat(artifacts.videoFile().assetType()).isEqualTo(AssetType.VIDEO);
        assertThat(artifacts.videoFile().fileName()).isEqualTo("sales-video-1-luma.mp4");
        assertThat(artifacts.metadata())
                .containsEntry("provider", "LUMA_RAY_3_2")
                .containsEntry("duration_seconds", 30)
                .containsEntry("scene_count", 3);
        RecordedRequest firstCreate = server.takeRequest();
        assertThat(firstCreate.getPath()).isEqualTo("/v1/generations");
        assertThat(firstCreate.getHeader("Authorization")).isEqualTo("Bearer luma-test-key");
        assertThat(firstCreate.getBody().readUtf8())
                .contains("\"model\":\"ray-3.2\"")
                .contains("\"type\":\"video\"")
                .contains("\"aspect_ratio\":\"9:16\"")
                .contains("Método MUSA")
                .contains("Very sharp image, crisp focus and constant soft natural daylight");
        RecordedRequest firstPoll = server.takeRequest();
        assertThat(firstPoll.getPath()).isEqualTo("/v1/generations/generation-1");
        assertThat(firstPoll.getHeader("Authorization")).isEqualTo("Bearer luma-test-key");
        RecordedRequest firstDownload = server.takeRequest();
        assertThat(firstDownload.getPath())
                .isEqualTo("/download/scene-1.mp4?X-Amz-Expires=3600&X-Amz-Signature=assinatura-1");
        assertThat(firstDownload.getHeader("Authorization")).isNull();
    }

    /** Deve ampliar a montagem para seis cenas quando o vídeo final tiver sessenta segundos. */
    @Test
    void shouldPlanSixScenesForSixtySecondVideo() throws Exception {
        LumaRayVideoProvider provider = new LumaRayVideoProvider(
                properties(),
                new ObjectMapper(),
                mock(VideoAssetClient.class),
                WebClient.builder());
        SalesVideoProfile base = profile();
        SalesVideoProfile longProfile = new SalesVideoProfile(
                base.id(), base.productId(), base.landingPageId(), base.videoKind(), base.title(),
                base.personaName(), base.personaStyle(), base.voiceStyle(), base.language(), 60,
                base.status(), base.createdAt(), base.updatedAt(), base.latestScript(), base.lastJob());

        assertThat(provider.resolveSceneCount(job(), longProfile)).isEqualTo(6);
    }

    /** Deve manter um job do Estúdio como clipe isolado de dez segundos. */
    @Test
    void shouldRenderStudioSceneAsSingleLumaClip() throws Exception {
        server.enqueue(json("{\"id\":\"generation-mechanism\",\"state\":\"queued\"}"));
        server.enqueue(json("""
                {
                  "id": "generation-mechanism",
                  "state": "completed",
                  "output": [{"url": "%s/download/mechanism.mp4"}],
                  "model": "ray-3.2"
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        LumaRayVideoProvider provider = new LumaRayVideoProvider(
                properties(),
                new ObjectMapper(),
                mock(VideoAssetClient.class),
                WebClient.builder());

        ProviderArtifacts artifacts = provider.render(studioSceneJob(), profile(), (percent, status, message) -> { });

        assertThat(artifacts.providerJobId()).isEqualTo("generation-mechanism");
        assertThat(artifacts.metadata())
                .containsEntry("duration_seconds", 10)
                .containsEntry("scene_count", 1);
        RecordedRequest create = server.takeRequest();
        assertThat(create.getBody().readUtf8())
                .contains("MECANISMO")
                .contains("celular")
                .doesNotContain("Dor, result, mechanism and CTA");
    }

    /** Deve falhar cedo quando a chave Luma não estiver configurada. */
    @Test
    void shouldRejectMissingLumaApiKey() throws Exception {
        VideoManagementProperties properties = properties();
        properties.getProviders().getLuma().setApiKey("");
        LumaRayVideoProvider provider = new LumaRayVideoProvider(
                properties,
                new ObjectMapper(),
                mock(VideoAssetClient.class),
                WebClient.builder());

        assertThatThrownBy(() -> provider.render(job(), profile(), (percent, status, message) -> { }))
                .isInstanceOf(VideoProviderException.class)
                .hasMessageContaining("LUMA_AGENTS_API_KEY");
    }

    /** Deve gerar imagem OpenAI, publicar asset e enviar frame inicial para Luma. */
    @Test
    void shouldUseOpenAiReferenceImageAsLumaStartFrame() throws Exception {
        server.enqueue(json("""
                {
                  "output": [
                    {"type": "image_generation_call", "result": "iVBORw0KGgo="}
                  ]
                }
                """));
        server.enqueue(json("{\"id\":\"generation-1\",\"state\":\"queued\"}"));
        server.enqueue(json("""
                {
                  "id": "generation-1",
                  "state": "completed",
                  "output": [{"url": "%s/download/scene-1.mp4"}],
                  "model": "ray-3.2"
                }
                """.formatted(server.url("/").toString().replaceAll("/$", ""))));
        server.enqueue(mp4Response());
        VideoManagementProperties properties = properties();
        properties.getProviders().getLuma().setOpenAiReferenceImageEnabled(true);
        properties.getProviders().getLuma().setOpenAiApiKey("openai-test-key");
        properties.getProviders().getLuma().setOpenAiBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getLuma().setSceneCount(1);
        VideoAssetClient assetClient = mock(VideoAssetClient.class);
        when(assetClient.uploadAsset(any(ProviderFile.class), any(String.class)))
                .thenReturn(new AssetResponse(
                        99L,
                        AssetType.IMAGE,
                        "https://cdn.example.com/reference.png",
                        null,
                        null,
                        Instant.now(),
                        Instant.now()));
        LumaRayVideoProvider provider = new LumaRayVideoProvider(
                properties,
                new ObjectMapper(),
                assetClient,
                WebClient.builder());

        ProviderArtifacts artifacts = provider.render(imageToVideoJob(), profile(), (percent, status, message) -> { });

        assertThat(artifacts.metadata())
                .containsEntry("image_to_video_enabled", true);
        RecordedRequest openAiRequest = server.takeRequest();
        assertThat(openAiRequest.getPath()).isEqualTo("/responses");
        assertThat(openAiRequest.getHeader("Authorization")).isEqualTo("Bearer openai-test-key");
        assertThat(openAiRequest.getBody().readUtf8())
                .contains("\"service_tier\":\"flex\"")
                .contains("\"model\":\"gpt-image-2\"")
                .contains("anti-sensualizacao");
        RecordedRequest lumaCreate = server.takeRequest();
        assertThat(lumaCreate.getPath()).isEqualTo("/v1/generations");
        assertThat(lumaCreate.getBody().readUtf8())
                .contains("\"keyframes\"")
                .contains("\"frame0\"")
                .contains("https://cdn.example.com/reference.png");
    }

    /** Mantem o host oficial da Luma Agents como padrao para evitar chamadas ao contrato legado. */
    @Test
    void shouldUseOfficialLumaAgentsBaseUrlByDefault() {
        VideoManagementProperties properties = new VideoManagementProperties();

        assertThat(properties.getProviders().getLuma().getBaseUrl())
                .isEqualTo(URI.create("https://agents.lumalabs.ai"));
    }

    /** Cria uma resposta JSON para a Luma Agents API simulada. */
    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    /** Cria uma resposta MP4 mínima com assinatura ftyp. */
    private MockResponse mp4Response() {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "video/mp4")
                .setBody(new Buffer().write(new byte[] {
                        0, 0, 0, 32, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 2, 0
                }));
    }

    /** Configura o provider Luma apontando para a API simulada e para montador fake. */
    private VideoManagementProperties properties() throws Exception {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getLuma().setEnabled(true);
        properties.getProviders().getLuma().setBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getLuma().setApiKey("luma-test-key");
        properties.getProviders().getLuma().setPollInterval(Duration.ofMillis(1));
        properties.getProviders().getLuma().setFfmpegPath(fakeFfmpeg().toString());
        return properties;
    }

    /** Cria um script executável que simula a montagem copiando um MP4 válido para a saída final. */
    private Path fakeFfmpeg() throws Exception {
        Path script = Files.createTempFile("fake-ffmpeg", ".sh");
        Files.writeString(script, """
                #!/bin/sh
                output=""
                for arg in "$@"; do
                  output="$arg"
                done
                printf '\\000\\000\\000\\040ftypisom\\000\\000\\002\\000' > "$output"
                exit 0
                """);
        script.toFile().setExecutable(true);
        return script;
    }

    /** Cria um job de render Luma com plano de cenas. */
    private SalesVideoJob job() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "LUMA_RAY_3_2",
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
                """
                        {
                          "videoProductionCycleId": 5,
                          "assembly_plan": {
                            "scenes": [
                              {"role":"DOR","title":"Dor do espelho","message":"Você se arruma, mas sente que falta presença."},
                              {"role":"RESULTADO","title":"Presença desejada","message":"Imagem mais intencional em 7 dias."},
                              {"role":"MECANISMO","title":"Mecanismo MUSA","message":"Ruído visual, peça-sinal, cor e acabamento."},
                              {"role":"CTA","title":"Diagnóstico gratuito","message":"Faça o diagnóstico e veja seu plano."}
                            ]
                          },
                          "visual_provider_directives": "Very sharp image, crisp focus and constant soft natural daylight"
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um job Luma que pede imagem OpenAI antes da animação. */
    private SalesVideoJob imageToVideoJob() {
        return new SalesVideoJob(
                1L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "LUMA_RAY_3_2",
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
                """
                        {
                          "generation_strategy": "OPENAI_IMAGE_TO_LUMA_VIDEO",
                          "image_to_video": {
                            "enabled": true,
                            "reference_image_count": 1,
                            "image_prompt": "Quadro MUSA funcional anti-sensualizacao."
                          },
                          "assembly_plan": {
                            "scenes": [
                              {"role":"DOR","title":"Dor funcional","message":"Ela organiza as roupas sem pose sedutora."}
                            ]
                          },
                          "visual_provider_directives": "Imagem clara, postura natural e luz constante."
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um job do Estúdio que representa apenas a cena MECANISMO. */
    private SalesVideoJob studioSceneJob() {
        return new SalesVideoJob(
                11L,
                2L,
                3L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "LUMA_RAY_3_2",
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
                """
                        {
                          "generation_strategy": "SCENE_BY_SCENE_MONTAGE",
                          "scene": {
                            "order": 3,
                            "role": "MECANISMO",
                            "prompt": "A mesma mulher usa o celular e executa um microajuste visível na manga."
                          }
                        }
                        """,
                Instant.now(),
                Instant.now());
    }

    /** Cria um perfil MUSA com roteiro aprovado. */
    private SalesVideoProfile profile() {
        SalesVideoScript script = new SalesVideoScript(
                10L,
                1,
                "Você se arruma, mas sente que falta presença. O Método MUSA cria microações de 7 dias.",
                "Você se arruma e ainda sente que falta presença?",
                "Ver meu plano MUSA de 7 dias",
                "Faça o diagnóstico gratuito.",
                null,
                "OPENAI",
                "gpt-5.5",
                "prompt",
                SalesVideoScriptStatus.APPROVED,
                "user",
                Instant.now(),
                Instant.now());
        return new SalesVideoProfile(
                2L,
                4L,
                null,
                "HERO",
                "Vídeo hero PDE v4 - Método MUSA",
                "MUSA",
                "elegante, íntima e prática",
                "calorosa e direta",
                "pt-BR",
                30,
                SalesVideoStatus.SCRIPT_READY,
                Instant.now(),
                Instant.now(),
                script,
                null);
    }
}
