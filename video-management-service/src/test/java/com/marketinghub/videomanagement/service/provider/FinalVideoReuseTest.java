package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.security.MessageDigest;
import java.util.HexFormat;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Garante que a recuperação usa exatamente os artefatos existentes sem TTS ou render. */
class FinalVideoReuseTest {
    private final ObjectMapper mapper = new ObjectMapper();

    /** Executa o provider real e verifica somente os dois downloads, preservando bytes e custo zero. */
    @Test void shouldReuseBytesWithoutVoiceGeneration() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("video-original"));
            server.enqueue(new MockResponse().setBody("WEBVTT\n\n00:00.000 --> 00:01.000\nCopy"));
            var metadata = mapper.createObjectNode().put("deliveryOnly",true);
            metadata.putObject("delivery_source").put("contractVersion","VIDEO_FINAL_REUSE_V1")
                    .put("jobId",91009).put("videoUrl",server.url("/source.mp4").toString())
                    .put("captionUrl",server.url("/source.vtt").toString())
                    .put("videoSha256",hash("video-original"))
                    .put("captionSha256",hash("WEBVTT\n\n00:00.000 --> 00:01.000\nCopy"));
            var job = mapper.treeToValue(mapper.createObjectNode().put("id",91010).put("profileId",91001)
                    .put("metadataJson",metadata.toString()),SalesVideoJob.class);
            var properties = new VideoManagementProperties();
            properties.getProviders().getPostProduction().setOpenAiApiKey("fixture-only");
            properties.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
            properties.getProviders().getPostProduction().setOpenAiBaseUrl(server.url("/").uri());
            var result = new PostProductionVideoProvider(properties,mapper,WebClient.builder())
                    .render(job,mapper.treeToValue(mapper.createObjectNode().put("id",91001),SalesVideoProfile.class),(p,s,m)->{});
            assertThat(result.videoFile().content()).isEqualTo("video-original".getBytes());
            assertThat(result.metadata().get("cost_usd").toString()).isEqualTo("0");
            assertThat(result.metadata().get("generation_repeated")).isEqualTo(false);
            assertThat(server.getRequestCount()).isEqualTo(2);
            assertThat(server.takeRequest().getPath()).isEqualTo("/source.mp4");
            assertThat(server.takeRequest().getPath()).isEqualTo("/source.vtt");
        }
    }

    /** Recusa bytes diferentes antes de produzir qualquer entrega. */
    @Test void shouldRejectChangedContent() throws Exception {
        try (var server = new MockWebServer()) {
            server.start(); server.enqueue(new MockResponse().setBody("changed"));
            var metadata = mapper.createObjectNode();
            metadata.putObject("delivery_source").put("contractVersion","VIDEO_FINAL_REUSE_V1")
                    .put("videoUrl",server.url("/source.mp4").toString()).put("videoSha256",hash("original"));
            var job = mapper.treeToValue(mapper.createObjectNode().put("id",91010),SalesVideoJob.class);
            assertThatThrownBy(() -> new FinalVideoReuse(WebClient.builder().build(),mapper).restore(job,metadata))
                    .isInstanceOf(VideoProviderException.class).hasMessageContaining("conferir");
            assertThat(server.getRequestCount()).isEqualTo(1);
        }
    }

    /** Calcula hashes das fixtures sem depender de credenciais ou provedores reais. */
    private String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes()));
    }
}
