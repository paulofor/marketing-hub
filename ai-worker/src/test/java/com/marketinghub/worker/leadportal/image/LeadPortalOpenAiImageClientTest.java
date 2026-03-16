package com.marketinghub.worker.leadportal.image;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.CreativeImageOptimizer;
import com.marketinghub.worker.imagegeneration.ImageGenerationPlan;
import com.marketinghub.worker.imagegeneration.ImageOrientation;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class LeadPortalOpenAiImageClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void omitsResponseFormatForGptImageModelsAndDownloadsUrl() throws Exception {
        AtomicReference<Map<String, Object>> capturedPayload = new AtomicReference<>();
        AtomicBoolean downloaded = new AtomicBoolean(false);
        String downloadUrl = "https://example.com/generated.png";
        String generationResponse = "{\"data\":[{\"url\":\"" + downloadUrl + "\"}]}";
        byte[] downloadBytes = samplePng();
        ExchangeFunction exchange = new StubExchangeFunction(
                capturedPayload, generationResponse, downloadUrl, downloadBytes, downloaded);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageOptimizer optimizer = new CreativeImageOptimizer(900_000, 1024);
        LeadPortalOpenAiImageClient client =
                new LeadPortalOpenAiImageClient(builder, optimizer, "key", "http://openai", "gpt-image-1");

        ImageGenerationPlan plan = new ImageGenerationPlan(
                1L, 1L, "gpt-image-1", "standard", ImageOrientation.SQUARE, 1024, 1024, "1024x1024", null);

        var result = client.generateFromPrompt("prompt", plan);

        assertThat(capturedPayload.get()).isNotNull();
        assertThat(capturedPayload.get()).containsEntry("model", "gpt-image-1");
        assertThat(capturedPayload.get()).doesNotContainKey("response_format");
        assertThat(downloaded.get()).isTrue();
        assertThat(result).isNotNull();
        assertThat(result.extension()).isEqualTo("jpg");
        assertThat(result.content()).isNotEmpty();
    }

    @Test
    void includesResponseFormatForNonGptModels() throws Exception {
        AtomicReference<Map<String, Object>> capturedPayload = new AtomicReference<>();
        String base64 = Base64.getEncoder().encodeToString(samplePng());
        String generationResponse = "{\"data\":[{\"b64_json\":\"" + base64 + "\"}]}";
        ExchangeFunction exchange = new StubExchangeFunction(
                capturedPayload, generationResponse, null, null, new AtomicBoolean(false));
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageOptimizer optimizer = new CreativeImageOptimizer(900_000, 1024);
        LeadPortalOpenAiImageClient client =
                new LeadPortalOpenAiImageClient(builder, optimizer, "key", "http://openai", "gpt-image-1");

        ImageGenerationPlan plan = new ImageGenerationPlan(
                1L, 1L, "dall-e-2", "standard", ImageOrientation.SQUARE, 1024, 1024, "1024x1024", null);

        var result = client.generateFromPrompt("prompt", plan);

        assertThat(capturedPayload.get()).containsEntry("response_format", "b64_json");
        assertThat(result).isNotNull();
        assertThat(result.extension()).isEqualTo("jpg");
        assertThat(result.content()).isNotEmpty();
    }

    private static byte[] samplePng() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4XgXBAQ0AAADBoPTp1X0CAAAAAElFTkSuQmCC");
    }


    private static class StubExchangeFunction implements ExchangeFunction {
        private final AtomicReference<Map<String, Object>> capturedPayload;
        private final String generationResponse;
        private final String downloadUrl;
        private final byte[] downloadBytes;
        private final AtomicBoolean downloaded;

        StubExchangeFunction(
                AtomicReference<Map<String, Object>> capturedPayload,
                String generationResponse,
                String downloadUrl,
                byte[] downloadBytes,
                AtomicBoolean downloaded) {
            this.capturedPayload = capturedPayload;
            this.generationResponse = generationResponse;
            this.downloadUrl = downloadUrl;
            this.downloadBytes = downloadBytes;
            this.downloaded = downloaded;
        }

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            String url = request.url().toString();
            if (url.endsWith("/images/generations")) {
                MockClientHttpRequest httpRequest = new MockClientHttpRequest(request.method(), request.url());
                request.body().insert(httpRequest, BODY_INSERTER_CONTEXT).block();
                String body = httpRequest.getBodyAsString().block();
                try {
                    Map<String, Object> payload = MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {});
                    if (capturedPayload != null) {
                        capturedPayload.set(payload);
                    }
                } catch (Exception ex) {
                    return Mono.error(ex);
                }
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(generationResponse)
                        .build());
            }
            if (downloadUrl != null && url.equals(downloadUrl)) {
                downloaded.set(true);
                String body = downloadBytes == null
                        ? ""
                        : new String(downloadBytes, StandardCharsets.UTF_8);
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .body(body)
                        .build());
            }
            return Mono.error(new IllegalStateException("Unexpected request: " + url));
        }
    }

    private static final BodyInserter.Context BODY_INSERTER_CONTEXT = new BodyInserter.Context() {
        private final List<HttpMessageWriter<?>> messageWriters = ExchangeStrategies.withDefaults().messageWriters();

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return messageWriters;
        }

        @Override
        public Map<String, Object> hints() {
            return Collections.emptyMap();
        }

        @Override
        public java.util.Optional<ServerHttpRequest> serverRequest() {
            return java.util.Optional.empty();
        }
    };
}
