package com.marketinghub.worker.targetingrequest;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cliente responsável por gerar candidatos iniciais de targeting via OpenAI para posterior validação oficial na Meta.
 */
@Component
public class TargetingRequestChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestChatGptClient.class);
    private static final String DOMAIN = "TARGETING_REQUEST";
    private static final String RESPONSES_ENDPOINT = "/v1/responses";
    private static final String SERVICE_TIER_FLEX = "flex";
    private static final String PROMPT_PATH = "prompts/targetingrequest/targeting-request-v1.md";
    private static final String COMPLETION_WINDOW = "24h";
    private static final int MAX_SEED_WORDS = 4;
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern LOCATION_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[\\p{L}\\s]{2,}$");
    private static final Pattern STATE_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[A-Z]{2}$");
    private static final Pattern PARENTHESIS_SUFFIX_PATTERN = Pattern.compile("\\s*\\([^)]*\\)$");
    private static final Set<String> TERMINAL_BATCH_STATUSES = Set.of("completed", "failed", "cancelled", "expired");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiGenerationRecorder generationRecorder;
    private final String targetingModel;
    private final Duration batchTimeout;
    private final Duration batchPollInterval;

    public TargetingRequestChatGptClient(WebClient.Builder builder,
                                         ObjectMapper objectMapper,
                                         AiGenerationRecorder generationRecorder,
                                         @Value("${openai.api-key:}") String apiKey,
                                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                         @Value("${openai.targeting-request-model:gpt-5.5}") String targetingModel,
                                         @Value("${openai.batch-timeout:PT5M}") Duration batchTimeout,
                                         @Value("${openai.batch-poll-interval:PT0.5S}") Duration batchPollInterval) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(targetingModel)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.generationRecorder = generationRecorder;
        this.targetingModel = targetingModel;
        this.batchTimeout = batchTimeout != null ? batchTimeout : Duration.ofMinutes(5);
        this.batchPollInterval = batchPollInterval != null ? batchPollInterval : Duration.ofMillis(500);
    }

    /**
     * Gera candidatos de targeting a partir da solicitação recebida do backend.
     */
    public List<TargetingCandidateSuggestion> generateCandidates(TargetingRequestDto request) {
        if (request == null) {
            return List.of();
        }
        String prompt = buildPrompt(request);
        RequestContext context = buildContext(request, prompt);
        Map<String, RequestContext> contexts = Map.of(context.customId(), context);
        Map<String, OpenAiResponse> responses = executeBatchRequests(contexts);
        OpenAiResponse response = responses.get(context.customId());
        if (response == null) {
            log.warn("OpenAI batch returned no response for targeting request {}", request.id());
            return List.of();
        }
        if (response.hasError()) {
            throw new IllegalStateException("OpenAI error: " + response.errorMessage());
        }
        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                request.id() != null ? request.id().toString() : null,
                prompt,
                content,
                targetingModel,
                response.usage());
        try {
            return parseContent(content);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response for request {}: {}", request.id(), content, e);
            throw new IllegalStateException("Unable to parse OpenAI response", e);
        }
    }

    /**
     * Monta o contexto de batch com payload Responses API em modo flex.
     */
    private RequestContext buildContext(TargetingRequestDto request, String prompt) {
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um planejador de mídia especialista em Meta Ads."),
                OpenAiRequestUtils.message("user", prompt)
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", targetingModel);
        payload.put("input", input);
        payload.put("service_tier", SERVICE_TIER_FLEX);
        if (OpenAiRequestUtils.supportsTemperature(targetingModel)) {
            payload.put("temperature", 0.4);
        }
        OpenAiRequestUtils.maybeAddReasoning(payload, targetingModel);
        String customId = request.id() != null ? "targeting-request-" + request.id() : "targeting-request-" + UUID.randomUUID();
        log.info("Queued targeting request {} with model {}", customId, targetingModel);
        return new RequestContext(customId, request, prompt, payload, targetingModel);
    }

    /**
     * Executa o lote OpenAI e converte o arquivo de saída em respostas por customId.
     */
    private Map<String, OpenAiResponse> executeBatchRequests(Map<String, RequestContext> contexts) {
        if (contexts.isEmpty()) {
            return Map.of();
        }
        String inputFileId = uploadBatchFile(contexts);
        OpenAiBatch batch = createBatch(inputFileId);
        OpenAiBatch completed = awaitCompletion(batch);
        if (!StringUtils.hasText(completed.outputFileId())) {
            throw new IllegalStateException("OpenAI batch did not return output_file_id");
        }
        String content = downloadFile(completed.outputFileId());
        return parseBatchOutput(content);
    }

    /**
     * Envia o arquivo JSONL com as solicitações de targeting para a OpenAI.
     */
    private String uploadBatchFile(Map<String, RequestContext> contexts) {
        StringBuilder sb = new StringBuilder();
        contexts.forEach((customId, ctx) -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("custom_id", customId);
            line.put("method", "POST");
            line.put("url", RESPONSES_ENDPOINT);
            line.put("body", ctx.payload());
            try {
                sb.append(objectMapper.writeValueAsString(line)).append('\n');
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize batch line for " + customId, e);
            }
        });
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "targeting-request-batch.jsonl";
            }
        };
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("purpose", "batch");
        multipart.add("file", resource);
        log.info("Uploading {} targeting requests to OpenAI batch file", contexts.size());
        OpenAiFile file = webClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart))
                .retrieve()
                .bodyToMono(OpenAiFile.class)
                .block();
        if (file == null || !StringUtils.hasText(file.id())) {
            throw new IllegalStateException("OpenAI file upload failed for targeting request batch");
        }
        return file.id();
    }

    /**
     * Cria o batch da OpenAI para processar as solicitações de targeting.
     */
    private OpenAiBatch createBatch(String inputFileId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("input_file_id", inputFileId);
        payload.put("endpoint", RESPONSES_ENDPOINT);
        payload.put("completion_window", COMPLETION_WINDOW);
        OpenAiBatch batch = webClient.post()
                .uri("/batches")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiBatch.class)
                .block();
        if (batch == null || !StringUtils.hasText(batch.id())) {
            throw new IllegalStateException("OpenAI batch creation failed for targeting requests");
        }
        return batch;
    }

    /**
     * Aguarda a conclusão do batch da OpenAI dentro do timeout configurado.
     */
    private OpenAiBatch awaitCompletion(OpenAiBatch initial) {
        OpenAiBatch current = initial;
        Instant deadline = Instant.now().plus(batchTimeout);
        while (current != null && !isTerminal(current)) {
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("Timed out waiting for OpenAI batch " + current.id() + " in status " + current.status());
            }
            try {
                Thread.sleep(Math.max(50L, batchPollInterval.toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for OpenAI batch", e);
            }
            current = webClient.get()
                    .uri("/batches/{id}", current.id())
                    .retrieve()
                    .bodyToMono(OpenAiBatch.class)
                    .block();
        }
        if (current == null) {
            throw new IllegalStateException("OpenAI returned null batch while polling targeting requests");
        }
        if (!"completed".equalsIgnoreCase(current.status())) {
            throw new IllegalStateException("OpenAI batch " + current.id() + " finished with status " + current.status());
        }
        return current;
    }

    /**
     * Verifica se o batch chegou a um estado terminal conhecido.
     */
    private boolean isTerminal(OpenAiBatch batch) {
        if (batch == null || !StringUtils.hasText(batch.status())) {
            return false;
        }
        return TERMINAL_BATCH_STATUSES.contains(batch.status().toLowerCase());
    }

    /**
     * Baixa o arquivo de saída produzido pelo batch da OpenAI.
     */
    private String downloadFile(String fileId) {
        return webClient.get()
                .uri("/files/{id}/content", fileId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Converte cada linha JSONL retornada pelo batch em resposta estruturada.
     */
    private Map<String, OpenAiResponse> parseBatchOutput(String content) {
        Map<String, OpenAiResponse> responses = new LinkedHashMap<>();
        if (!StringUtils.hasText(content)) {
            return responses;
        }
        for (String line : content.split("\n")) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            try {
                BatchOutput output = objectMapper.readValue(line, BatchOutput.class);
                if (output.response() != null && output.response().isSuccessful()) {
                    Map<String, Object> body = output.response().body();
                    if (body == null) {
                        log.warn("OpenAI batch line {} has null body", output.customId());
                        continue;
                    }
                    responses.put(output.customId(), objectMapper.convertValue(body, OpenAiResponse.class));
                } else if (output.response() != null) {
                    log.error("OpenAI batch request {} failed with status {}", output.customId(), output.response().statusCode());
                } else if (output.error() != null) {
                    log.error("OpenAI batch request {} failed: {} - {}", output.customId(), output.error().code(), output.error().message());
                }
            } catch (Exception ex) {
                log.error("Failed to parse batch output line: {}", line, ex);
            }
        }
        return responses;
    }

    /**
     * Carrega o prompt versionado e injeta os dados operacionais da solicitação.
     */
    private String buildPrompt(TargetingRequestDto request) {
        String template = loadPromptTemplate();
        return template
                .replace("{{locale}}", request.localeOrDefault())
                .replace("{{country}}", request.countryOrDefault())
                .replace("{{descricao}}", request.descricao() != null ? request.descricao() : "");
    }

    /**
     * Lê o prompt de targeting do classpath para evitar instruções longas hardcoded.
     */
    private String loadPromptTemplate() {
        ClassPathResource resource = new ClassPathResource(PROMPT_PATH);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar prompt de targeting no caminho {}", PROMPT_PATH, ex);
            throw new IllegalStateException("Não foi possível carregar o prompt de targeting", ex);
        }
    }

    /**
     * Interpreta o JSON retornado pela OpenAI e monta sugestões normalizadas.
     */
    private List<TargetingCandidateSuggestion> parseContent(String content) throws Exception {
        String sanitizedContent = sanitizeContent(content);
        ObjectMapper lenientMapper = objectMapper.copy()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature());
        JsonNode root = lenientMapper.readTree(sanitizedContent);
        JsonNode candidatesNode = root.get("candidates");
        if (candidatesNode == null) {
            candidatesNode = root;
        }
        List<TargetingCandidateSuggestion> result = new ArrayList<>();
        if (candidatesNode == null) {
            return result;
        }
        Iterator<JsonNode> iterator = candidatesNode.isArray() ? candidatesNode.iterator() : List.of(candidatesNode).iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String seed = sanitizeSeed(readText(node, "seed"));
            if (!StringUtils.hasText(seed)) {
                seed = sanitizeSeed(readText(node, "texto_sugerido"));
            }
            if (!StringUtils.hasText(seed)) {
                continue;
            }
            TargetingCandidateType tipo = TargetingCandidateType.from(readText(node, "tipo"));
            BigDecimal score = readDecimal(node, "score");
            if (score != null && score.compareTo(BigDecimal.valueOf(0.75)) < 0) {
                log.info("Descartando seed {} por baixa aderência comercial de IA: {}", seed, score);
                continue;
            }
            String rationale = readText(node, "rationale");
            String idiomaHint = firstNonBlank(readText(node, "idioma_hint"), readText(node, "idioma"));
            String intent = readText(node, "intent_tag");
            String country = null;
            JsonNode constraints = node.get("constraints");
            if (constraints != null) {
                country = readText(constraints, "country");
            }
            result.add(new TargetingCandidateSuggestion(
                    seed,
                    List.of(seed),
                    tipo != null ? tipo : TargetingCandidateType.INTEREST,
                    "AI",
                    score,
                    rationale,
                    idiomaHint,
                    intent,
                    country
            ));
        }
        return result;
    }

    /**
     * Remove cercas de markdown para permitir leitura do JSON retornado.
     */
    private String sanitizeContent(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.contains("\n")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    /**
     * Lê campo textual de um nó JSON quando ele existe.
     */
    private String readText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    /**
     * Lê campo decimal de um nó JSON aceitando número ou texto numérico.
     */
    private BigDecimal readDecimal(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode value = node.get(field);
        if (value != null && value.isNumber()) {
            return value.decimalValue();
        }
        if (value != null && value.isTextual()) {
            try {
                return new BigDecimal(value.asText());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * Normaliza o seed removendo localidade, sufixos e excesso de palavras.
     */
    private String sanitizeSeed(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String collapsed = MULTIPLE_SPACES.matcher(raw).replaceAll(" ").trim();
        collapsed = PARENTHESIS_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = LOCATION_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = STATE_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = collapsed.replaceAll("[\\p{Punct}]+$", "");
        collapsed = collapsed.trim();
        if (!StringUtils.hasText(collapsed)) {
            return null;
        }
        return limitWords(collapsed, MAX_SEED_WORDS);
    }

    /**
     * Limita um texto à quantidade máxima de palavras permitida para seed.
     */
    private String limitWords(String value, int maxWords) {
        String[] tokens = MULTIPLE_SPACES.matcher(value).replaceAll(" ").trim().split(" ");
        if (tokens.length <= maxWords) {
            return String.join(" ", tokens);
        }
        return String.join(" ", Arrays.copyOf(tokens, maxWords));
    }

    /**
     * Retorna o primeiro texto preenchido entre os valores recebidos.
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record RequestContext(String customId,
                                  TargetingRequestDto request,
                                  String prompt,
                                  Map<String, Object> payload,
                                  String model) {
    }

    private record OpenAiFile(String id) {
    }

    private record OpenAiBatch(String id, String status,
                               @com.fasterxml.jackson.annotation.JsonProperty("output_file_id") String outputFileId,
                               BatchError error) {
    }

    private record BatchError(String message, String code) {
    }

    private record BatchOutput(@com.fasterxml.jackson.annotation.JsonProperty("custom_id") String customId,
                               BatchOutputResponse response,
                               BatchOutputError error) {
    }

    private record BatchOutputResponse(@com.fasterxml.jackson.annotation.JsonProperty("status_code") Integer statusCode,
                                       Map<String, Object> body) {
        boolean isSuccessful() {
            return statusCode != null && statusCode >= 200 && statusCode < 300;
        }
    }

    private record BatchOutputError(String message, String code) {
    }
}
