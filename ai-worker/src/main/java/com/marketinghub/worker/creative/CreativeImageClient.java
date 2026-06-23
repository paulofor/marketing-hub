package com.marketinghub.worker.creative;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Simple wrapper around OpenAI image-capable APIs for creative image generation.
 */
@Component
public class CreativeImageClient {
    private final WebClient webClient;
    private final BackendAssetClient assetClient;
    private final String model;
    private final String responsesModel;
    private final String serviceTier;
    private final Duration requestTimeout;
    private final CreativeImageOptimizer imageOptimizer;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private static final Logger log = LoggerFactory.getLogger(CreativeImageClient.class);
    private static final int DEFAULT_MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(15);

    /**
     * Builds the OpenAI image client with the default Flex-compatible timeout and service tier.
     */
    public CreativeImageClient(WebClient.Builder builder,
                               BackendAssetClient assetClient,
                               CreativeImageOptimizer imageOptimizer,
                               @Value("${openai.api-key:}") String apiKey,
                               @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                               @Value("${openai.image-model:gpt-image-2}") String model,
                               @Value("${openai.responses-model:gpt-5.5}") String responsesModel,
                               @Value("${openai.image-service-tier:flex}") String serviceTier,
                               @Value("${openai.image-timeout-seconds:900}") long requestTimeoutSeconds) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.requestTimeout = resolveRequestTimeout(requestTimeoutSeconds);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(this.requestTimeout)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler((int) this.requestTimeout.getSeconds()))
                        .addHandlerLast(new WriteTimeoutHandler((int) this.requestTimeout.getSeconds())));
        WebClient.Builder clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_MAX_IN_MEMORY_SIZE));
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = clientBuilder.build();
        this.assetClient = assetClient;
        this.imageOptimizer = imageOptimizer;
        this.model = model;
        this.responsesModel = normalizeConfig(responsesModel, "gpt-5.5");
        this.serviceTier = normalizeConfig(serviceTier, "flex");
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (!enabled) {
            log.warn("OpenAI API key not configured; creative image generation will fail until the key is configured");
        }
    }

    /**
     * Generates an image for a prompt without an intermediate prompt.
     */
    public String generateImage(String prompt) {
        return generateImage(prompt, null, "creative-default");
    }

    /**
     * Generates an image and returns a usable URL or fails when generation cannot run.
     */
    public String generateImage(String prompt, String intermediatePrompt) {
        return generateImage(prompt, intermediatePrompt, "creative");
    }

    /**
     * Generates an image with operational context so logs can isolate OpenAI, upload or configuration failures.
     */
    public String generateImage(String prompt, String intermediatePrompt, String operationContext) {
        String context = normalizeContext(operationContext);
        if (!enabled) {
            log.error("Cannot generate creative image because OpenAI API key is missing. context={}", context);
            throw new IllegalStateException("OpenAI API key is required to generate creative images");
        }
        OpenAiImageResult imageResult = callOpenAiForImage(prompt, context);
        ImageData data = imageResult.data();
        if (data.base64() != null && !data.base64().isBlank()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(data.base64());
                CreativeImageOptimizer.OptimizedImage optimized = imageOptimizer.optimize(imageBytes);
                if (log.isInfoEnabled()) {
                    log.info("Optimized OpenAI image from {} bytes to {} bytes", imageBytes.length,
                            optimized.content().length);
                }
                String filename = "creative-" + UUID.randomUUID() + "." + optimized.extension();
                String uploadedUrl = assetClient.uploadImage(optimized.content(), filename, imageResult.sourceModel(), prompt,
                        intermediatePrompt);
                log.info("Creative image generated by OpenAI and uploaded to backend. context={} filename={} url={}",
                        context, filename, uploadedUrl);
                return uploadedUrl;
            } catch (BackendAssetClient.BackendAssetUploadException e) {
                log.error("OpenAI returned image bytes but backend asset upload failed. context={}", context, e);
                throw e;
            } catch (IllegalArgumentException e) {
                log.error("Failed to decode OpenAI image payload. context={}", context, e);
                throw new RuntimeException("Failed to decode image payload", e);
            }
        }
        if (data.url() != null && !data.url().isBlank()) {
            log.warn("OpenAI image response missing base64 payload, using remote URL. context={} url={}",
                    context, data.url());
            return data.url();
        }
        throw new RuntimeException("No image returned from OpenAI");
    }

    /**
     * Calls the OpenAI image-capable endpoint and extracts the generated image payload.
     */
    private OpenAiImageResult callOpenAiForImage(String prompt, String context) {
        return isFlexTier(serviceTier)
                ? callResponsesImageTool(prompt, context)
                : callImageApi(prompt, context);
    }

    /**
     * Calls the Responses API image generation tool using Flex processing.
     */
    private OpenAiImageResult callResponsesImageTool(String prompt, String context) {
        Map<String, Object> imageTool = new LinkedHashMap<>();
        imageTool.put("type", "image_generation");
        imageTool.put("action", "generate");
        imageTool.put("model", model);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", responsesModel);
        payload.put("input", prompt);
        payload.put("tools", List.of(imageTool));
        payload.put("service_tier", serviceTier);

        log.info("Sending creative image request to OpenAI Responses API. context={} timeoutSeconds={} payload={}",
                context, requestTimeout.getSeconds(), payload);
        ResponsesImageResponse response;
        try {
            response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .exchangeToMono(clientResponse -> readResponsesImageResponse(clientResponse, context))
                    .block(requestTimeout);
        } catch (Exception ex) {
            log.error("OpenAI Responses image generation transport failed after {} seconds. context={}",
                    requestTimeout.getSeconds(), context, ex);
            throw new RuntimeException("Failed to call OpenAI Responses API", ex);
        }

        log.info("Parsed OpenAI Responses image response. context={} parsedResponse={}", context, response);
        return extractResponsesImage(response);
    }

    /**
     * Calls the direct Image API for non-Flex image generation modes.
     */
    private OpenAiImageResult callImageApi(String prompt, String context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        if (supportsResponseFormat(model)) {
            payload.put("response_format", "b64_json");
        }

        log.info("Sending creative image request to OpenAI Image API. context={} timeoutSeconds={} payload={}",
                context, requestTimeout.getSeconds(), payload);
        ImageResponse response;
        try {
            response = webClient.post()
                    .uri("/images/generations")
                    .bodyValue(payload)
                    .exchangeToMono(clientResponse -> readImageResponse(clientResponse, context))
                    .block(requestTimeout);
        } catch (Exception ex) {
            log.error("OpenAI image generation transport failed after {} seconds. context={}",
                    requestTimeout.getSeconds(), context, ex);
            throw new RuntimeException("Failed to call OpenAI image API", ex);
        }

        log.info("Parsed OpenAI image response. context={} parsedResponse={}", context, response);
        return extractImageApiImage(response);
    }

    /**
     * Extracts the first generated image from an Image API response.
     */
    private OpenAiImageResult extractImageApiImage(ImageResponse response) {
        if (response == null) {
            throw new RuntimeException("No image returned from OpenAI");
        }
        List<ImageData> dataList = response.data();
        if (dataList == null || dataList.isEmpty()) {
            ApiError error = response.error();
            if (error != null && error.message() != null && !error.message().isBlank()) {
                log.error("OpenAI image API returned error. message={} type={} param={} code={}",
                        error.message(), error.type(), error.param(), error.code());
                throw new RuntimeException("OpenAI image API error: " + error.message());
            }
            throw new RuntimeException("No image returned from OpenAI");
        }
        return new OpenAiImageResult(dataList.get(0), model);
    }

    /**
     * Extracts the first generated image from a Responses API image generation tool response.
     */
    private OpenAiImageResult extractResponsesImage(ResponsesImageResponse response) {
        if (response == null) {
            throw new RuntimeException("No image returned from OpenAI Responses API");
        }
        ApiError error = response.error();
        if (error != null && error.message() != null && !error.message().isBlank()) {
            log.error("OpenAI Responses API returned error. message={} type={} param={} code={}",
                    error.message(), error.type(), error.param(), error.code());
            throw new RuntimeException("OpenAI Responses API error: " + error.message());
        }
        if (response.output() != null) {
            for (ResponsesOutput output : response.output()) {
                if (output != null && "image_generation_call".equals(output.type())
                        && output.result() != null && !output.result().isBlank()) {
                    return new OpenAiImageResult(new ImageData(null, output.result()), model);
                }
            }
        }
        throw new RuntimeException("No image returned from OpenAI Responses API");
    }

    /**
     * Reads and logs the raw OpenAI response before parsing the image contract.
     */
    private Mono<ImageResponse> readImageResponse(ClientResponse response, String context) {
        HttpStatusCode status = response.statusCode();
        return response.body(BodyExtractors.toDataBuffers())
                .map(this::toByteArray)
                .reduceWith(ByteArrayOutputStream::new, this::appendChunk)
                .map(ByteArrayOutputStream::toByteArray)
                .map(bytes -> parseResponse(bytes, status, context));
    }

    /**
     * Copies a reactive data buffer to a byte array and releases it.
     */
    private byte[] toByteArray(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    /**
     * Appends a response chunk while enforcing the configured memory limit.
     */
    private ByteArrayOutputStream appendChunk(ByteArrayOutputStream output, byte[] chunk) {
        if (output.size() + chunk.length > DEFAULT_MAX_IN_MEMORY_SIZE) {
            throw new IllegalStateException("OpenAI image payload exceeds allowed size");
        }
        output.write(chunk, 0, chunk.length);
        return output;
    }

    /**
     * Reads and logs the raw OpenAI Responses API response before parsing the image tool contract.
     */
    private Mono<ResponsesImageResponse> readResponsesImageResponse(ClientResponse response, String context) {
        HttpStatusCode status = response.statusCode();
        return response.body(BodyExtractors.toDataBuffers())
                .map(this::toByteArray)
                .reduceWith(ByteArrayOutputStream::new, this::appendChunk)
                .map(ByteArrayOutputStream::toByteArray)
                .map(bytes -> parseResponsesImageResponse(bytes, status, context));
    }

    /**
     * Parses the raw OpenAI image response after recording status and body for root-cause analysis.
     */
    private ImageResponse parseResponse(byte[] bytes, HttpStatusCode status, String context) {
        String rawResponse = new String(bytes, StandardCharsets.UTF_8);
        log.info("Received raw OpenAI image response. context={} status={} rawResponse={}",
                context, status.value(), rawResponse);
        try {
            return objectMapper.readValue(bytes, ImageResponse.class);
        } catch (IOException e) {
            log.error("Failed to decode OpenAI image response as JSON. context={} status={} rawResponse={}",
                    context, status.value(), rawResponse, e);
            throw new RuntimeException("Failed to decode image payload", e);
        }
    }

    /**
     * Parses the raw OpenAI Responses API response after recording status and body for root-cause analysis.
     */
    private ResponsesImageResponse parseResponsesImageResponse(byte[] bytes, HttpStatusCode status, String context) {
        String rawResponse = new String(bytes, StandardCharsets.UTF_8);
        log.info("Received raw OpenAI Responses image response. context={} status={} rawResponse={}",
                context, status.value(), rawResponse);
        try {
            return objectMapper.readValue(bytes, ResponsesImageResponse.class);
        } catch (IOException e) {
            log.error("Failed to decode OpenAI Responses image response as JSON. context={} status={} rawResponse={}",
                    context, status.value(), rawResponse, e);
            throw new RuntimeException("Failed to decode image payload", e);
        }
    }

    /**
     * Indicates whether the selected model supports an explicit response_format request field.
     */
    private boolean supportsResponseFormat(String selectedModel) {
        if (selectedModel == null || selectedModel.isBlank()) {
            return true;
        }
        return !selectedModel.toLowerCase(Locale.ROOT).startsWith("gpt-image-");
    }

    /**
     * Determines whether the configured service tier should use the Responses API Flex path.
     */
    private boolean isFlexTier(String selectedServiceTier) {
        return "flex".equalsIgnoreCase(selectedServiceTier);
    }

    /**
     * Resolves the request timeout, keeping Flex-compatible defaults for invalid configuration.
     */
    private Duration resolveRequestTimeout(long requestTimeoutSeconds) {
        if (requestTimeoutSeconds <= 0) {
            return DEFAULT_REQUEST_TIMEOUT;
        }
        return Duration.ofSeconds(requestTimeoutSeconds);
    }

    /**
     * Normalizes blank configuration values to explicit operational defaults.
     */
    private String normalizeConfig(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * Normalizes absent context values to keep logs queryable.
     */
    private String normalizeContext(String operationContext) {
        if (operationContext == null || operationContext.isBlank()) {
            return "creative";
        }
        return operationContext.trim();
    }

    private record OpenAiImageResult(ImageData data, String sourceModel) {}
    private record ImageResponse(List<ImageData> data, ApiError error) {}
    private record ResponsesImageResponse(List<ResponsesOutput> output, ApiError error) {}
    private record ResponsesOutput(String type, String result) {}
    private record ImageData(String url, @JsonProperty("b64_json") String base64) {}
    private record ApiError(String message, String type, String param, String code) {}
}
