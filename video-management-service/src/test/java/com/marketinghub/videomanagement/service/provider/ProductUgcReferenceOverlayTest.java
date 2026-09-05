package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Responsabilidade: comprovar a remoção determinística de texto inventado no Product UGC. */
class ProductUgcReferenceOverlayTest {

    /** Substitui somente os planos alternados de produto e preserva evidência da referência. */
    @Test
    void shouldReplaceAlternatingProductScenesWithApprovedReference() throws Exception {
        Path arguments = Files.createTempFile("product-reference-overlay-arguments", ".txt");
        VideoManagementProperties properties = properties(arguments);
        AtomicInteger requests = new AtomicInteger();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    requests.incrementAndGet();
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "image/png")
                            .body("approved-product-reference")
                            .build());
                })
                .build();
        ProductUgcReferenceOverlay overlay = new ProductUgcReferenceOverlay(
                properties, new ObjectMapper(), webClient);
        Path source = Files.createTempFile("product-ugc-source", ".mp4");
        Files.writeString(source, "source-video");
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put(
                "generation_strategy",
                "RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION");
        metadata.putObject("post_production")
                .put("text_rendering", "DETERMINISTIC_OVERLAY")
                .put("provider_embedded_text_allowed", false);
        metadata.put(
                "runwayRouterRequestsJson",
                "[{\"productImage\":{\"uri\":\"https://assets.example/musa.png\"}}]");

        ProductUgcReferenceOverlay.OverlayResult result = overlay.apply(
                source,
                metadata,
                21234L);

        try {
            assertThat(result.videoFile()).isNotEqualTo(source).isRegularFile();
            assertThat(result.audit())
                    .containsEntry("status", "APPROVED_REFERENCE_APPLIED")
                    .containsEntry("provider_embedded_text_removed", true);
            assertThat(result.audit().get("scene_cut_times_seconds").toString())
                    .contains("5.5", "7.625", "10.75");
            assertThat(result.audit().get("replaced_scene_numbers").toString())
                    .isEqualTo("[2, 4]");
            assertThat(Files.readString(arguments))
                    .contains(
                            "scdet=threshold=10",
                            "between(t,5.500,7.625)+gte(t,10.750)",
                            "-loop",
                            "-t",
                            "15.042",
                            "libx264");
            assertThat(requests).hasValue(1);
        } finally {
            Files.deleteIfExists(result.videoFile());
            Files.deleteIfExists(source);
        }
    }

    /** Não baixa nem altera referência quando o job não solicita o contrato Product UGC. */
    @Test
    void shouldKeepSourceUntouchedForOtherRecipes() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    requests.incrementAndGet();
                    return Mono.error(new IllegalStateException("download não esperado"));
                })
                .build();
        ProductUgcReferenceOverlay overlay = new ProductUgcReferenceOverlay(
                new VideoManagementProperties(), new ObjectMapper(), webClient);
        Path source = Files.createTempFile("ordinary-source", ".mp4");

        ProductUgcReferenceOverlay.OverlayResult result = overlay.apply(
                source, new ObjectMapper().readTree("{}"), 55L);

        assertThat(result.videoFile()).isEqualTo(source);
        assertThat(result.audit()).isEmpty();
        assertThat(requests).hasValue(0);
        Files.deleteIfExists(source);
    }

    /** Configura um ffmpeg simulado que emite cortes e materializa o vídeo composto. */
    private VideoManagementProperties properties(Path arguments) throws Exception {
        Path ffmpeg = Files.createTempFile("fake-product-reference-ffmpeg", ".sh");
        Files.writeString(ffmpeg, """
                #!/bin/sh
                printf '%%s\n' "$@" >> '%s'
                output=""
                for argument in "$@"; do
                  case "$argument" in
                    *scdet=*file=*)
                      cuts="${argument##*file=}"
                      printf 'frame:132 pts:1\nlavfi.scd.time=5.5\nframe:183 pts:2\nlavfi.scd.time=7.625\nframe:258 pts:3\nlavfi.scd.time=10.75\n' > "$cuts"
                      ;;
                  esac
                  output="$argument"
                done
                if [ "$output" != "-" ]; then printf 'video-with-approved-reference' > "$output"; fi
                exit 0
                """.formatted(arguments));
        ffmpeg.toFile().setExecutable(true);
        Path ffprobe = Files.createTempFile("fake-product-reference-ffprobe", ".sh");
        Files.writeString(ffprobe, "#!/bin/sh\nprintf '15.042\\n'\n");
        ffprobe.toFile().setExecutable(true);
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getPostProduction().setFfmpegPath(ffmpeg.toString());
        properties.getProviders().getPostProduction().setFfprobePath(ffprobe.toString());
        return properties;
    }
}
