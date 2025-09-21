package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
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
        when(backendAssetClient.uploadImage(any(), any())).thenReturn("/uploads/img.png");

        String result = client.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/img.png");
        verify(backendAssetClient).uploadImage(any(byte[].class), argThat(name -> name.startsWith("creative-") && name.endsWith(".png")));
    }
}

