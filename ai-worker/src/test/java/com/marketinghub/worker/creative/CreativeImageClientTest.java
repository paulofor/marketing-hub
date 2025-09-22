package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class CreativeImageClientTest {
    @Mock
    BackendAssetClient backendAssetClient;

    CreativeImageClient client;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        String imagePayload = Base64.getEncoder().encodeToString("img".getBytes(StandardCharsets.UTF_8));
        String body = "{\"data\":[{\"b64_json\":\"" + imagePayload + "\"}]}";
        ExchangeFunction exchange = request -> {
            assertThat(request.url().toString()).endsWith("/images/generations");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        };
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        client = new CreativeImageClient(builder, backendAssetClient, "key", "http://openai", "image-model");
    }

    @Test
    void uploadsReturnedImageToBackend() {
        when(backendAssetClient.uploadImage(any(), any(), any(), any())).thenReturn("/uploads/img.png");

        String result = client.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/img.png");
        verify(backendAssetClient).uploadImage(any(byte[].class),
                argThat(name -> name.startsWith("creative-") && name.endsWith(".png")),
                argThat(model -> model.equals("image-model")),
                argThat(prompt -> prompt.equals("prompt")));
    }

    @Test
    void supportsLargeBase64Payloads() {
        byte[] imageBytes = new byte[512 * 1024];
        Arrays.fill(imageBytes, (byte) 1);
        String imagePayload = Base64.getEncoder().encodeToString(imageBytes);
        String body = "{\"data\":[{\"b64_json\":\"" + imagePayload + "\"}]}";
        ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageClient largeClient = new CreativeImageClient(builder, backendAssetClient, "key", "http://openai", "image-model");
        when(backendAssetClient.uploadImage(any(), any(), any(), any())).thenReturn("/uploads/large.png");

        String result = largeClient.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/large.png");
        verify(backendAssetClient).uploadImage(argThat(bytes -> bytes.length == imageBytes.length), any(), any(), any());
    }
}

