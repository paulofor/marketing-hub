package com.marketinghub.worker.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Valida o comportamento do cliente de imagens contra os contratos da OpenAI e do upload no backend.
 */
class CreativeImageClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    BackendAssetClient backendAssetClient;

    CreativeImageClient client;
    CreativeImageOptimizer optimizer;
    AtomicReference<Map<String, Object>> lastRequestPayload;

    /**
     * Cria a fixture do cliente de imagem com resposta OpenAI simulada com sucesso.
     */
    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        optimizer = new CreativeImageOptimizer(900_000, 1024);
        lastRequestPayload = new AtomicReference<>();
        String imagePayload = Base64.getEncoder().encodeToString(createSolidPng(128, 128));
        String body = "{\"data\":[{\"b64_json\":\"" + imagePayload + "\"}]}";
        ExchangeFunction exchange = stubImageApi(lastRequestPayload, body, HttpStatus.OK);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        client = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai", "gpt-image-1",
                "gpt-5.5", "default", 900);
    }

    /**
     * Garante que a imagem base64 retornada pela OpenAI seja otimizada e enviada ao backend.
     */
    @Test
    void uploadsReturnedImageToBackend() {
        when(backendAssetClient.uploadImage(any(), any(), any(), any(), any())).thenReturn("/uploads/img.jpg");

        String result = client.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/img.jpg");
        Map<String, Object> request = lastRequestPayload.get();
        assertThat(request).containsEntry("model", "gpt-image-1");
        assertThat(request).containsEntry("prompt", "prompt");
        assertThat(request).doesNotContainKey("response_format");
        verify(backendAssetClient).uploadImage(any(byte[].class),
                argThat(name -> name.startsWith("creative-") && name.endsWith(".jpg")),
                argThat(model -> model.equals("gpt-image-1")),
                argThat(prompt -> prompt.equals("prompt")),
                isNull());
    }

    /**
     * Garante que payloads grandes em base64 sejam aceitos dentro do limite de memória configurado.
     */
    @Test
    void supportsLargeBase64Payloads() {
        byte[] imageBytes = createRandomPng(1024, 1024);
        String imagePayload = Base64.getEncoder().encodeToString(imageBytes);
        String body = "{\"data\":[{\"b64_json\":\"" + imagePayload + "\"}]}";
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        ExchangeFunction exchange = stubImageApi(requestPayload, body, HttpStatus.OK);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageClient largeClient = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai",
                "gpt-image-1", "gpt-5.5", "default", 900);
        when(backendAssetClient.uploadImage(any(), any(), any(), any(), any())).thenReturn("/uploads/large.jpg");

        String result = largeClient.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/large.jpg");
        verify(backendAssetClient).uploadImage(argThat(bytes -> bytes.length > 0),
                argThat(name -> name.endsWith(".jpg")), any(), any(), isNull());
    }

    /**
     * Garante que mensagens de erro da OpenAI sejam expostas quando a resposta não tem imagem.
     */
    @Test
    void surfacesErrorMessageWhenResponseLacksData() {
        String body = "{\"data\":null,\"error\":{\"message\":\"quota exceeded\"}}";
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        ExchangeFunction exchange = stubImageApi(requestPayload, body, HttpStatus.BAD_REQUEST);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageClient errorClient = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai",
                "gpt-image-1", "gpt-5.5", "default", 900);

        assertThatThrownBy(() -> errorClient.generateImage("prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("quota exceeded");
    }

    /**
     * Garante que modelos de imagem não GPT peçam resposta base64 explicitamente.
     */
    @Test
    void requestsBase64PayloadExplicitlyForNonGptModels() {
        String imagePayload;
        try {
            imagePayload = Base64.getEncoder().encodeToString(createSolidPng(64, 64));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String body = "{\"data\":[{\"b64_json\":\"" + imagePayload + "\"}]}";
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        ExchangeFunction exchange = stubImageApi(requestPayload, body, HttpStatus.OK);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageClient dalleClient = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai",
                "dall-e-3", "gpt-5.5", "default", 900);
        when(backendAssetClient.uploadImage(any(), any(), any(), any(), any())).thenReturn("/uploads/dalle.jpg");

        String result = dalleClient.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/dalle.jpg");
        Map<String, Object> payload = requestPayload.get();
        assertThat(payload).containsEntry("response_format", "b64_json");
    }

    /**
     * Garante que o tier Flex use a ferramenta de geração de imagem da Responses API.
     */
    @Test
    void usesResponsesApiImageToolForFlexTier() {
        String imagePayload;
        try {
            imagePayload = Base64.getEncoder().encodeToString(createSolidPng(64, 64));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String body = "{\"output\":[{\"type\":\"image_generation_call\",\"result\":\"" + imagePayload + "\"}]}";
        AtomicReference<Map<String, Object>> requestPayload = new AtomicReference<>();
        ExchangeFunction exchange = stubOpenAiApi(requestPayload, body, HttpStatus.OK, "/responses");
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchange);
        CreativeImageClient flexClient = new CreativeImageClient(builder, backendAssetClient, optimizer, "key", "http://openai",
                "gpt-image-2", "gpt-5.5", "flex", 900);
        when(backendAssetClient.uploadImage(any(), any(), any(), any(), any())).thenReturn("/uploads/flex.jpg");

        String result = flexClient.generateImage("prompt");

        assertThat(result).isEqualTo("/uploads/flex.jpg");
        Map<String, Object> payload = requestPayload.get();
        assertThat(payload).containsEntry("model", "gpt-5.5");
        assertThat(payload).containsEntry("input", "prompt");
        assertThat(payload).containsEntry("service_tier", "flex");
        List<Map<String, Object>> tools = (List<Map<String, Object>>) payload.get("tools");
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0))
                .containsEntry("type", "image_generation")
                .containsEntry("action", "generate")
                .containsEntry("model", "gpt-image-2");
        verify(backendAssetClient).uploadImage(any(byte[].class),
                argThat(name -> name.endsWith(".jpg")),
                argThat(model -> model.equals("gpt-image-2")),
                argThat(prompt -> prompt.equals("prompt")),
                isNull());
    }

    /**
     * Deve tentar Flex duas vezes e cair para Standard/default na terceira tentativa quando a OpenAI retorna 429.
     */
    @Test
    void retriesTransientFlexImageErrorWithStandardTierOnThirdAttempt() {
        String imagePayload;
        try {
            imagePayload = Base64.getEncoder().encodeToString(createSolidPng(64, 64));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String transientBody = "{\"error\":{\"message\":\"We're currently processing too many requests — please try again later.\",\"code\":\"rate_limit_exceeded\"}}";
        String successBody = "{\"data\":[{\"b64_json\":\"" + imagePayload + "\"}]}";
        AtomicInteger calls = new AtomicInteger();
        List<Map<String, Object>> payloads = new ArrayList<>();
        ExchangeFunction exchange = request -> {
            int attempt = calls.incrementAndGet();
            MockClientHttpRequest httpRequest = new MockClientHttpRequest(request.method(), request.url());
            request.body().insert(httpRequest, BODY_INSERTER_CONTEXT).block();
            String requestBody = httpRequest.getBodyAsString().block();
            try {
                payloads.add(MAPPER.readValue(requestBody, new TypeReference<Map<String, Object>>() { }));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            String expectedPath = attempt < 3 ? "/responses" : "/images/generations";
            assertThat(request.url().toString()).endsWith(expectedPath);
            return Mono.just(ClientResponse.create(attempt < 3 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(attempt < 3 ? transientBody : successBody)
                    .build());
        };
        CreativeImageClient retryClient = new CreativeImageClient(
                WebClient.builder().exchangeFunction(exchange),
                backendAssetClient,
                optimizer,
                "key",
                "http://openai",
                "gpt-image-2",
                "gpt-5.5",
                "flex",
                900);
        when(backendAssetClient.uploadImage(any(), any(), any(), any(), any())).thenReturn("/uploads/retry.jpg");

        String result = retryClient.generateImage("prompt", null, "creative-experiment-54");

        assertThat(result).isEqualTo("/uploads/retry.jpg");
        assertThat(calls).hasValue(3);
        assertThat(payloads.get(0)).containsEntry("service_tier", "flex");
        assertThat(payloads.get(1)).containsEntry("service_tier", "flex");
        assertThat(payloads.get(2)).doesNotContainKey("service_tier");
    }


    /**
     * Garante que ausência de credencial OpenAI falhe em vez de retornar URL nula.
     */
    @Test
    void failsWhenOpenAiKeyIsMissing() {
        CreativeImageClient disabledClient = new CreativeImageClient(
                WebClient.builder().exchangeFunction(stubImageApi(null, "{}", HttpStatus.OK)),
                backendAssetClient,
                optimizer,
                " ",
                "http://openai",
                "gpt-image-1",
                "gpt-5.5",
                "flex",
                900);

        assertThatThrownBy(() -> disabledClient.generateImage("prompt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OpenAI API key is required");
    }

    /**
     * Monta um ExchangeFunction que captura payloads da OpenAI e retorna resposta simulada.
     */
    private ExchangeFunction stubImageApi(AtomicReference<Map<String, Object>> capturedPayload,
                                          String responseBody,
                                          HttpStatus status) {
        return stubOpenAiApi(capturedPayload, responseBody, status, "/images/generations");
    }

    /**
     * Monta um ExchangeFunction que captura payloads da OpenAI para um endpoint específico.
     */
    private ExchangeFunction stubOpenAiApi(AtomicReference<Map<String, Object>> capturedPayload,
                                           String responseBody,
                                           HttpStatus status,
                                           String expectedPath) {
        return request -> {
            assertThat(request.url().toString()).endsWith(expectedPath);
            MockClientHttpRequest httpRequest = new MockClientHttpRequest(request.method(), request.url());
            request.body().insert(httpRequest, BODY_INSERTER_CONTEXT).block();
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

    private static final BodyInserter.Context BODY_INSERTER_CONTEXT = new BodyInserter.Context() {
        private final List<HttpMessageWriter<?>> messageWriters = ExchangeStrategies.withDefaults().messageWriters();

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return messageWriters;
        }

        @Override
        public Optional<ServerHttpRequest> serverRequest() {
            return Optional.empty();
        }

        @Override
        public Map<String, Object> hints() {
            return Collections.emptyMap();
        }
    };

    /**
     * Cria uma imagem PNG pequena e determinística para testes de upload.
     */
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

    /**
     * Cria uma imagem PNG pseudoaleatória determinística para testes de tamanho de payload.
     */
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

    /**
     * Codifica uma imagem em memória como bytes PNG.
     */
    private static byte[] writePng(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode PNG", e);
        }
    }
}
