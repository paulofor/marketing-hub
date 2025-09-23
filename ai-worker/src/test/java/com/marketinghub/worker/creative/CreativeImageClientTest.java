package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserterContext;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class CreativeImageClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    BackendAssetClient backendAssetClient;

    CreativeImageClient client;
    CreativeImageOptimizer optimizer;
    AtomicReference<Map<String, Object>> lastRequestPayload;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        optimizer = new CreativeImageOptimizer(900_000, 1024);
        lastRequestPayload = new AtomicReference<>();
        String imagePayload = Base64.getEncoder().encodeToString(createSolidPng(128, 128));
        String body = "{\"data\":[{\"b64_json\":\"" + imagePayload + "\"}]}";
        ExchangeFunction exchange = stubImageApi(lastRequestPayload, body, HttpStatus.OK);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        client = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai", "gpt-image-1");
    }

    @Test
    void uploadsReturnedImageToBackend() {
        when(backendAssetClient.uploadImage(any(), any(), any(), any())).thenReturn("/uploads/img.jpg");

        String result = client.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/img.jpg");
        Map<String, Object> request = lastRequestPayload.get();
        assertThat(request).containsEntry("model", "gpt-image-1");
        assertThat(request).containsEntry("prompt", "prompt");
        assertThat(request).doesNotContainKey("response_format");
        verify(backendAssetClient).uploadImage(any(byte[].class),
                argThat(name -> name.startsWith("creative-") && name.endsWith(".jpg")),
                argThat(model -> model.equals("gpt-image-1")),
                argThat(prompt -> prompt.equals("prompt")));
    }

    @Test
    void supportsLargeBase64Payloads() {
        byte[] imageBytes = createRandomPng(1024, 1024);
        String imagePayload = Base64.getEncoder().encodeToString(imageBytes);
        String body = "{\"data\":[{\"b64_json\":\"" + imagePayload + "\"}]}";
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        ExchangeFunction exchange = stubImageApi(requestPayload, body, HttpStatus.OK);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageClient largeClient = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai", "gpt-image-1");
        when(backendAssetClient.uploadImage(any(), any(), any(), any())).thenReturn("/uploads/large.jpg");

        String result = largeClient.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/large.jpg");
        verify(backendAssetClient).uploadImage(argThat(bytes -> bytes.length > 0),
                argThat(name -> name.endsWith(".jpg")), any(), any());
    }

    @Test
    void surfacesErrorMessageWhenResponseLacksData() {
        String body = "{\"data\":null,\"error\":{\"message\":\"quota exceeded\"}}";
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        ExchangeFunction exchange = stubImageApi(requestPayload, body, HttpStatus.BAD_REQUEST);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageClient errorClient = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai", "gpt-image-1");

        assertThatThrownBy(() -> errorClient.generateImage("prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("quota exceeded");
    }

    @Test
    void requestsBase64PayloadExplicitlyForNonGptModels() {
        String body = "{\"data\":[{\"b64_json\":\"" + Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}) + "\"}]}";
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        ExchangeFunction exchange = stubImageApi(requestPayload, body, HttpStatus.OK);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageClient dalleClient = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai", "dall-e-3");
        when(backendAssetClient.uploadImage(any(), any(), any(), any())).thenReturn("/uploads/dalle.jpg");

        String result = dalleClient.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/dalle.jpg");
        Map<String, Object> payload = requestPayload.get();
        assertThat(payload).containsEntry("response_format", "b64_json");
    }

    private ExchangeFunction stubImageApi(AtomicReference<Map<String, Object>> capturedPayload,
                                          String responseBody,
                                          HttpStatus status) {
        return request -> {
            assertThat(request.url().toString()).endsWith("/images/generations");
            MockClientHttpRequest httpRequest = new MockClientHttpRequest(request.method(), request.url());
            request.body().insert(httpRequest, new BodyInserterContext());
            String requestBody = httpRequest.getBodyAsString().block();
            try {
                Map<String, Object> payload = MAPPER.readValue(requestBody, new TypeReference<Map<String, Object>>() { });
                if (capturedPayload != null) {
                    capturedPayload.set(payload);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(responseBody)
                    .build());
        };
    }

    private static byte[] createSolidPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(new Color(30, 144, 255, 200));
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return writePng(image);
    }

    private static byte[] createRandomPng(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(42);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = random.nextInt(256);
                int g = random.nextInt(256);
                int b = random.nextInt(256);
                int a = 255;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
            }
        }
        return writePng(image);
    }

    private static byte[] writePng(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode PNG", e);
        }
    }
}

