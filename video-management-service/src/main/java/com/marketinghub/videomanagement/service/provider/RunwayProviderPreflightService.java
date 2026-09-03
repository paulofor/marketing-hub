package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.client.payload.ProviderPreflightResultPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Responsabilidade: consultar organização e dry run da Runway sem iniciar geração faturável. */
@Component
public class RunwayProviderPreflightService {
    private static final Logger log = LoggerFactory.getLogger(RunwayProviderPreflightService.class);
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final RunwayRouterRequestFactory requestFactory;
    private final WebClient webClient;

    /** Configura o cliente oficial, o gerador determinístico de payload e a auditoria JSON. */
    public RunwayProviderPreflightService(
            VideoManagementProperties properties,
            ObjectMapper objectMapper,
            RunwayRouterRequestFactory requestFactory,
            WebClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.requestFactory = requestFactory;
        this.webClient = builder.baseUrl(properties.getProviders().getRunway().getBaseUrl().toString()).build();
    }

    /** Executa snapshot e dry runs, devolvendo bloqueio estruturado em qualquer incerteza. */
    public ProviderPreflightResultPayload execute(ProviderPreflightJob job) {
        Instant observedAt = Instant.now();
        String sourceUrl = sourceUrl();
        if (!"Runway".equalsIgnoreCase(job.aggregatorName())) {
            return blocked(job, null, "PROVIDER_ACCOUNT_UNSUPPORTED",
                    "O preflight Runway recebeu uma conta de outro agregador.", sourceUrl, observedAt);
        }
        if (!properties.getProviders().getRunway().isEnabled()) {
            return blocked(job, null, "PROVIDER_NOT_CONFIGURED",
                    "O adapter Runway está desabilitado no executor de vídeo.", sourceUrl, observedAt);
        }
        String apiKey;
        try {
            apiKey = resolveApiKey();
        } catch (IOException ex) {
            log.error("Falha ao ler credencial Runway no preflight; cycleId={}", job.cycleId(), ex);
            return blocked(job, null, "PROVIDER_AUTH_ERROR",
                    "Não foi possível ler a credencial Runway montada no executor.", sourceUrl, observedAt);
        }
        if (!StringUtils.hasText(apiKey)) {
            return blocked(job, null, "PROVIDER_AUTH_ERROR",
                    "A credencial Runway não está configurada no executor.", sourceUrl, observedAt);
        }
        try {
            JsonNode organization = getOrganization(job, apiKey);
            requireOrganizationContract(organization);
            JsonNode sanitizedOrganization = sanitizeOrganization(organization);
            List<Map<String, Object>> requests = requestFactory.build(job);
            String executionRequestsJson = objectMapper.writeValueAsString(requests);
            String payloadSha256 = sha256(executionRequestsJson);
            List<JsonNode> routingResponses = new ArrayList<>();
            List<Map<String, Object>> selectedRoutes = new ArrayList<>();
            BigDecimal estimatedCredits = BigDecimal.ZERO;
            BigDecimal maximumAuthorizedCredits = BigDecimal.ZERO;
            for (int index = 0; index < requests.size(); index++) {
                Map<String, Object> request = requests.get(index);
                JsonNode routing = dryRun(job, apiKey, request, index + 1);
                routingResponses.add(routing);
                JsonNode route = routing.path("routing");
                requireRoutingContract(request, route);
                BigDecimal credits = route.path("estimatedCost").path("credits").decimalValue();
                BigDecimal priceCeiling = route.path("resolvedSettings").path("priceCeiling").decimalValue();
                estimatedCredits = estimatedCredits.add(credits);
                maximumAuthorizedCredits = maximumAuthorizedCredits.add(priceCeiling);
                selectedRoutes.add(selectedRoute(job, index + 1, request, route, credits));
            }
            QuotaValidation quota = validateQuota(sanitizedOrganization, selectedRoutes);
            if (!quota.ready()) {
                return reviewableBlocker(
                        job,
                        configId(requests),
                        quota.code(),
                        quota.detail(),
                        sourceUrl,
                        Instant.now(),
                        sanitizedOrganization,
                        objectMapper.writeValueAsString(routingResponses),
                        objectMapper.writeValueAsString(selectedRoutes),
                        executionRequestsJson,
                        payloadSha256,
                        estimatedCredits,
                        objectMapper.writeValueAsString(quota.details()));
            }
            BigDecimal balance = sanitizedOrganization.path("creditBalance").decimalValue();
            Long maxMonthly = sanitizedOrganization.path("tier").path("maxMonthlyCreditSpend").longValue();
            String quotaJson = objectMapper.writeValueAsString(quota.details());
            boolean unsafeCeiling = maximumAuthorizedCredits.compareTo(job.maxCredits()) > 0;
            log.info(
                    "Preflight Runway concluído; cycleId={} account={} configId={} requests={} credits={} balance={} source={}",
                    job.cycleId(), job.accountKey(), configId(requests), requests.size(), estimatedCredits, balance,
                    sourceUrl);
            return new ProviderPreflightResultPayload(
                    "READY",
                    job.accountKey(),
                    configId(requests),
                    payloadSha256,
                    executionRequestsJson,
                    sanitizedOrganization.toString(),
                    objectMapper.writeValueAsString(routingResponses),
                    objectMapper.writeValueAsString(selectedRoutes),
                    estimatedCredits,
                    balance,
                    maxMonthly,
                    quotaJson,
                    sanitizedOrganization.path("usage").toString(),
                    unsafeCeiling ? "PROVIDER_ROUTER_CEILING_UNSAFE" : null,
                    unsafeCeiling
                            ? "A soma dos tetos do Router ultrapassa o teto em créditos autorizado para o ciclo; Plutus deve rejeitar ou orientar outro perfil."
                            : null,
                    sourceUrl,
                    Instant.now());
        } catch (WebClientResponseException ex) {
            String body = sanitize(ex.getResponseBodyAsString());
            log.error(
                    "Runway rejeitou preflight; cycleId={} status={} url={} response={}",
                    job.cycleId(), ex.getStatusCode().value(), sourceUrl, body, ex);
            return blocked(job, null, httpCode(ex, body), httpDetail(ex, body), sourceUrl, Instant.now());
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.error("Resposta Runway inválida no preflight; cycleId={} url={}", job.cycleId(), sourceUrl, ex);
            return blocked(job, null, "PROVIDER_PREFLIGHT_INVALID_RESPONSE",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(),
                    sourceUrl, Instant.now());
        } catch (RuntimeException ex) {
            log.error("Falha de integração no preflight Runway; cycleId={} url={}", job.cycleId(), sourceUrl, ex);
            return blocked(job, null, "PROVIDER_PREFLIGHT_UNAVAILABLE",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(),
                    sourceUrl, Instant.now());
        }
    }

    /** Consulta saldo, tier e uso pela API oficial da organização. */
    private JsonNode getOrganization(ProviderPreflightJob job, String apiKey) {
        String path = properties.getProviders().getRunway().getOrganizationPath();
        log.info("Consultando organização Runway; cycleId={} url={}", job.cycleId(), baseUrl() + path);
        JsonNode response = authorized(webClient.get().uri(path), apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        log.info("Resposta da organização Runway recebida; cycleId={} status=SUCCESS", job.cycleId());
        return response;
    }

    /** Mantém somente saldo e limites necessários, descartando metadados alheios ao preflight. */
    private JsonNode sanitizeOrganization(JsonNode organization) {
        var sanitized = objectMapper.createObjectNode();
        sanitized.set("creditBalance", organization.path("creditBalance").deepCopy());
        var tier = sanitized.putObject("tier");
        tier.set("maxMonthlyCreditSpend", organization.path("tier").path("maxMonthlyCreditSpend").deepCopy());
        tier.set("models", organization.path("tier").path("models").deepCopy());
        var usage = sanitized.putObject("usage");
        usage.set("models", organization.path("usage").path("models").deepCopy());
        return sanitized;
    }

    /** Executa a mesma requisição futura com o único acréscimo transitório `dryRun=true`. */
    private JsonNode dryRun(
            ProviderPreflightJob job, String apiKey, Map<String, Object> executionRequest, int scene) {
        LinkedHashMap<String, Object> request = new LinkedHashMap<>(executionRequest);
        request.put("dryRun", true);
        String path = properties.getProviders().getRunway().getRouterGeneratePath();
        log.info("Chamando dry run Runway; cycleId={} scene={} url={} request={}",
                job.cycleId(), scene, baseUrl() + path, request);
        JsonNode response = authorized(webClient.post()
                        .uri(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request), apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        log.info("Resposta dry run Runway; cycleId={} scene={} response={}", job.cycleId(), scene, response);
        if (response == null
                || !response.path("dryRun").isBoolean()
                || !response.path("dryRun").asBoolean()
                || !response.path("routing").isObject()) {
            throw new IllegalArgumentException("Runway não confirmou uma resposta de dry run com routing.");
        }
        return response;
    }

    /** Exige os campos oficiais necessários para saldo e limite antes de qualquer estimativa local. */
    private void requireOrganizationContract(JsonNode organization) {
        if (organization == null
                || !organization.path("creditBalance").isNumber()
                || !organization.path("tier").path("maxMonthlyCreditSpend").isIntegralNumber()
                || !organization.path("tier").path("models").isObject()
                || !organization.path("usage").path("models").isObject()) {
            throw new IllegalArgumentException("Snapshot da organização Runway está incompleto.");
        }
    }

    /** Exige a decisão completa do Router e o teto duro que limitará a chamada faturável. */
    private void requireRoutingContract(Map<String, Object> request, JsonNode route) {
        String requestedConfigId = String.valueOf(request.get("configId"));
        JsonNode settings = route.path("resolvedSettings");
        BigDecimal estimated = route.path("estimatedCost").path("credits").decimalValue();
        BigDecimal ceiling = settings.path("priceCeiling").decimalValue();
        if (!route.path("model").isTextual()
                || route.path("model").asText().isBlank()
                || !route.path("provider").isTextual()
                || route.path("provider").asText().isBlank()
                || !route.path("configId").isTextual()
                || !requestedConfigId.equals(route.path("configId").asText())
                || !settings.path("optimizeFor").isTextual()
                || settings.path("optimizeFor").asText().isBlank()
                || !settings.path("priceCeiling").isNumber()
                || ceiling.signum() <= 0
                || !route.path("resolvedInput").isObject()
                || !route.path("estimatedCost").path("credits").isNumber()
                || estimated.signum() <= 0
                || estimated.compareTo(ceiling) > 0) {
            throw new IllegalArgumentException(
                    "Dry run não confirmou modelo, provider, config, input, custo e teto coerentes.");
        }
    }

    /** Verifica quota diária e concorrência configurada de cada modelo selecionado pelo router. */
    private QuotaValidation validateQuota(
            JsonNode organization, List<Map<String, Object>> selectedRoutes) {
        Map<String, Integer> requestedByModel = new LinkedHashMap<>();
        selectedRoutes.forEach(route -> requestedByModel.merge(String.valueOf(route.get("model")), 1, Integer::sum));
        List<Map<String, Object>> details = new ArrayList<>();
        Set<String> missing = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> requested : requestedByModel.entrySet()) {
            String model = requested.getKey();
            JsonNode limits = organization.path("tier").path("models").path(model);
            JsonNode usage = organization.path("usage").path("models").path(model);
            if (!limits.path("maxConcurrentGenerations").isIntegralNumber()
                    || !limits.path("maxDailyGenerations").isIntegralNumber()
                    || !usage.path("dailyGenerations").isIntegralNumber()) {
                missing.add(model);
                continue;
            }
            long concurrency = limits.path("maxConcurrentGenerations").longValue();
            long dailyLimit = limits.path("maxDailyGenerations").longValue();
            long dailyUsed = usage.path("dailyGenerations").longValue();
            details.add(Map.of(
                    "model", model,
                    "requestedGenerations", requested.getValue(),
                    "maxConcurrentGenerations", concurrency,
                    "maxDailyGenerations", dailyLimit,
                    "dailyGenerations", dailyUsed,
                    "remainingDailyGenerations", Math.max(0, dailyLimit - dailyUsed)));
            if (concurrency <= 0) {
                return new QuotaValidation(false, "PROVIDER_CONCURRENCY_UNAVAILABLE",
                        "A conta não possui concorrência disponível para " + model + ".", details);
            }
            if (dailyUsed + requested.getValue() > dailyLimit) {
                return new QuotaValidation(false, "PROVIDER_DAILY_QUOTA_EXCEEDED",
                        "A quota diária não cobre as gerações previstas para " + model + ".", details);
            }
        }
        if (!missing.isEmpty()) {
            return new QuotaValidation(false, "PROVIDER_QUOTA_UNKNOWN",
                    "A Runway não informou limites completos para: " + String.join(", ", missing) + ".", details);
        }
        return new QuotaValidation(true, null, null, details);
    }

    /** Resume fabricante, modelo, agregador e custo de cada rota retornada pelo dry run. */
    private Map<String, Object> selectedRoute(
            ProviderPreflightJob job,
            int scene,
            Map<String, Object> request,
            JsonNode route,
            BigDecimal credits) {
        LinkedHashMap<String, Object> selected = new LinkedHashMap<>();
        selected.put("scene", scene);
        selected.put("manufacturer", route.path("provider").asText());
        selected.put("model", route.path("model").asText());
        selected.put("aggregator", job.aggregatorName());
        selected.put("accountKey", job.accountKey());
        selected.put("routerConfigId", route.path("configId").asText());
        selected.put("batchRouteId", "RUNWAY_ROUTER:" + route.path("configId").asText());
        selected.put("optimizeFor", route.path("resolvedSettings").path("optimizeFor").asText());
        selected.put(
                "priceCeilingCredits",
                route.path("resolvedSettings").path("priceCeiling").decimalValue());
        selected.put("estimatedCredits", credits);
        selected.put("durationSeconds", ((Map<?, ?>) request.get("input")).get("duration"));
        selected.put("resolution", ((Map<?, ?>) request.get("input")).get("resolution"));
        return selected;
    }

    /** Produz um bloqueio mínimo quando não houve resposta utilizável do agregador. */
    private ProviderPreflightResultPayload blocked(
            ProviderPreflightJob job,
            String configId,
            String code,
            String detail,
            String sourceUrl,
            Instant observedAt) {
        return new ProviderPreflightResultPayload(
                "BLOCKED",
                job.accountKey(),
                configId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                code,
                detail,
                sourceUrl,
                observedAt);
    }

    /** Preserva saldo, uso e dry run quando apenas a quota reprova o lote validado. */
    private ProviderPreflightResultPayload reviewableBlocker(
            ProviderPreflightJob job,
            String configId,
            String code,
            String detail,
            String sourceUrl,
            Instant observedAt,
            JsonNode organization,
            String routing,
            String routes,
            String requests,
            String payloadHash,
            BigDecimal estimatedCredits,
            String quotaJson) {
        return new ProviderPreflightResultPayload(
                "READY",
                job.accountKey(),
                configId,
                payloadHash,
                requests,
                organization.toString(),
                routing,
                routes,
                estimatedCredits,
                organization.path("creditBalance").decimalValue(),
                organization.path("tier").path("maxMonthlyCreditSpend").longValue(),
                quotaJson,
                organization.path("usage").toString(),
                code,
                detail,
                sourceUrl,
                observedAt);
    }

    /** Resolve o identificador único da configuração usada por todas as requisições do lote. */
    private String configId(List<Map<String, Object>> requests) {
        return requests.isEmpty() ? null : String.valueOf(requests.getFirst().get("configId"));
    }

    /** Aplica autenticação sem registrar ou persistir o segredo do executor. */
    private WebClient.RequestHeadersSpec<?> authorized(
            WebClient.RequestHeadersSpec<?> request, String apiKey) {
        return request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-Runway-Version", properties.getProviders().getRunway().getApiVersion())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    /** Lê a chave direta ou o secret montado sem expor seu conteúdo em logs. */
    private String resolveApiKey() throws IOException {
        VideoManagementProperties.Runway runway = properties.getProviders().getRunway();
        if (StringUtils.hasText(runway.getApiKey())) return runway.getApiKey().trim();
        if (!StringUtils.hasText(runway.getApiKeyFile())) return null;
        String key = Files.readString(Path.of(runway.getApiKeyFile().trim())).trim();
        return StringUtils.hasText(key) ? key : null;
    }

    /** Classifica erros HTTP oficiais em causas operacionais estáveis. */
    private String httpCode(WebClientResponseException ex, String body) {
        if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
            return "PROVIDER_AUTH_ERROR";
        }
        if (ex.getStatusCode().value() == 404 || body.contains("router_config_not_found")) {
            return "PROVIDER_ROUTER_CONFIG_MISSING";
        }
        if (ex.getStatusCode().value() == 429) return "PROVIDER_RATE_LIMIT";
        if (body.contains("no_eligible_model")) return "PROVIDER_NO_ELIGIBLE_MODEL";
        return "PROVIDER_PREFLIGHT_REJECTED";
    }

    /** Explica a ação necessária sem devolver credencial ou resposta externa ilimitada. */
    private String httpDetail(WebClientResponseException ex, String body) {
        return "Runway respondeu HTTP %d no preflight: %s"
                .formatted(ex.getStatusCode().value(), body);
    }

    /** Limita corpo externo usado em log e bloqueio persistido. */
    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) return "sem corpo";
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() > 2000 ? normalized.substring(0, 2000) : normalized;
    }

    /** Calcula o hash que o provider verificará antes de repetir as requisições faturáveis. */
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 indisponível no preflight Runway", ex);
            throw new IllegalStateException("SHA-256 indisponível.", ex);
        }
    }

    /** Resolve a base oficial usada somente para evidência e logs. */
    private String baseUrl() {
        return properties.getProviders().getRunway().getBaseUrl().toString();
    }

    /** Resolve a URL oficial da organização persistida como fonte do saldo. */
    private String sourceUrl() {
        return baseUrl() + properties.getProviders().getRunway().getOrganizationPath();
    }

    /** Representa a validação consolidada de limites dos modelos selecionados. */
    private record QuotaValidation(
            boolean ready, String code, String detail, List<Map<String, Object>> details) {}
}
