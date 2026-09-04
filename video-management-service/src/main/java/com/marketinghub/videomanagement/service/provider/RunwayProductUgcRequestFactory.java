package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: montar e precificar o contrato versionado da receita Product UGC da Runway. */
@Component
public class RunwayProductUgcRequestFactory {
    private static final Logger log = LoggerFactory.getLogger(RunwayProductUgcRequestFactory.class);
    public static final String PROVIDER_NAME = "RUNWAY_PRODUCT_UGC";
    public static final String RECIPE_NAME = "product_ugc";
    public static final String RECIPE_VERSION = "2026-06";
    public static final String CONFIGURATION_ID = RECIPE_NAME + "@" + RECIPE_VERSION;
    private static final String PRODUCT_INFO_PATH =
            "prompts/apollo/product-ugc/v1/product-info.md";
    private static final String USER_CONCEPT_PATH =
            "prompts/apollo/product-ugc/v1/user-concept.md";
    private static final String TASK_SCHEMA_PATH =
            "prompts/apollo/product-ugc/v1/response-schema.json";
    private final String productInfoTemplate;
    private final String userConceptTemplate;
    private final String taskSchema;
    private final String contractSha256;
    private final boolean allowLocalHttp;

    /** Carrega integralmente prompt e schema versionados antes de aceitar qualquer preflight. */
    public RunwayProductUgcRequestFactory() {
        this(false);
    }

    /** Permite saída HTTP apenas no servidor local usado pelos testes do adapter. */
    RunwayProductUgcRequestFactory(boolean allowLocalHttp) {
        this.productInfoTemplate = resource(PRODUCT_INFO_PATH);
        this.userConceptTemplate = resource(USER_CONCEPT_PATH);
        this.taskSchema = resource(TASK_SCHEMA_PATH);
        this.allowLocalHttp = allowLocalHttp;
        validateContractResources();
        this.contractSha256 = sha256(
                productInfoTemplate + "\n" + userConceptTemplate + "\n" + taskSchema);
    }

    /** Identifica explicitamente a receita no plano persistido do Estúdio. */
    public boolean supports(ProviderPreflightJob job) {
        return job != null
                && StringUtils.hasText(job.providerPlan())
                && job.providerPlan().toUpperCase(Locale.ROOT).contains("(" + PROVIDER_NAME + ")");
    }

    /** Cria uma única requisição premium, silenciosa e reproduzível para pós-produção determinística. */
    public List<Map<String, Object>> build(ProviderPreflightJob job) {
        validate(job);
        LinkedHashMap<String, Object> request = new LinkedHashMap<>();
        request.put("version", RECIPE_VERSION);
        request.put("characterImage", Map.of("uri", job.characterPerformanceUri().trim()));
        request.put("productImage", Map.of("uri", job.referencePerformanceUri().trim()));
        request.put("productInfo", limit(productInfo(job), 2500));
        request.put("userConcept", limit(userConcept(job), 3500));
        request.put("duration", job.targetDurationSeconds());
        request.put("ratio", ratio(job));
        request.put("audio", false);
        return List.of(request);
    }

    /** Calcula a tarifa oficial fixa da versão pinada sem executar uma chamada faturável. */
    public BigDecimal estimatedCredits(ProviderPreflightJob job) {
        validateDuration(job.targetDurationSeconds());
        int secondsAfterMinimum = job.targetDurationSeconds() - 4;
        boolean fullHd = "1080:1920".equals(ratio(job));
        int baseCredits = fullHd ? 208 : 192;
        int additionalCredits = fullHd ? 40 : 36;
        return BigDecimal.valueOf(baseCredits + (long) additionalCredits * secondsAfterMinimum);
    }

    /** Expõe a resolução física usada pela receita para auditoria financeira. */
    public String resolution(ProviderPreflightJob job) {
        return "1080:1920".equals(ratio(job)) ? "1080p" : "720p";
    }

    /** Expõe versão, caminhos e hash do contrato que produziu a requisição paga. */
    public Map<String, Object> contractAudit() {
        return Map.of(
                "contractVersion", "APOLLO_RUNWAY_PRODUCT_UGC_V1",
                "productInfoPrompt", PRODUCT_INFO_PATH,
                "userConceptPrompt", USER_CONCEPT_PATH,
                "taskResponseSchema", TASK_SCHEMA_PATH,
                "sha256", contractSha256);
    }

    /** Valida pelo contrato versionado a resposta inicial antes de iniciar polling cobrável. */
    public String requireAcceptedTaskId(JsonNode response) {
        String id = response == null ? "" : response.path("id").asText("").trim();
        if (!StringUtils.hasText(id)) {
            throw new VideoProviderException(
                    "PROVIDER_RENDER_FAILED",
                    "Runway não retornou o id exigido por " + TASK_SCHEMA_PATH + ".");
        }
        return id;
    }

    /** Valida a saída final da receita e devolve somente a URL HTTPS prevista no schema. */
    public String requireCompletedVideoUrl(JsonNode response) {
        String status = response == null ? "" : response.path("status").asText("").trim();
        JsonNode output = response == null ? null : response.path("output");
        String url = output != null && output.isArray() && !output.isEmpty()
                ? output.get(0).asText("").trim()
                : "";
        if (!"SUCCEEDED".equalsIgnoreCase(status) || !isApprovedOutputUri(url)) {
            throw new VideoProviderException(
                    "PROVIDER_RENDER_FAILED",
                    "A saída Product UGC diverge do contrato " + TASK_SCHEMA_PATH + ".");
        }
        return url;
    }

    /** Valida referências, direitos, duração e texto antes de qualquer consulta externa. */
    private void validate(ProviderPreflightJob job) {
        if (!supports(job)) {
            throw new VideoProviderException(
                    "PROVIDER_INPUT_INVALID", "O plano não selecionou a receita Product UGC.");
        }
        validateDuration(job.targetDurationSeconds());
        if (!"image".equalsIgnoreCase(job.characterPerformanceType())) {
            throw new VideoProviderException(
                    "PROVIDER_INPUT_INVALID", "Product UGC exige uma imagem autorizada da apresentadora.");
        }
        if (!isHttps(job.characterPerformanceUri()) || !isHttps(job.referencePerformanceUri())) {
            throw new VideoProviderException(
                    "PROVIDER_INPUT_INVALID",
                    "Product UGC exige URLs HTTPS da apresentadora e da tela limpa do PDE.");
        }
        if (!StringUtils.hasText(job.performanceConsentEvidence())
                || !StringUtils.hasText(job.performanceRightsEvidence())) {
            throw new VideoProviderException(
                    "PROVIDER_INPUT_INVALID",
                    "Product UGC exige consentimento da apresentadora e direitos das duas referências.");
        }
        if (!StringUtils.hasText(job.captionPlan()) || !StringUtils.hasText(job.ctaText())) {
            throw new VideoProviderException(
                    "PROVIDER_INPUT_INVALID", "Product UGC exige texto final e CTA aprovados.");
        }
    }

    /** Exige o intervalo contratual da receita sem reduzir silenciosamente a duração solicitada. */
    private void validateDuration(Integer duration) {
        if (duration == null || duration < 4 || duration > 15) {
            throw new VideoProviderException(
                    "PROVIDER_INPUT_INVALID", "Product UGC aceita duração de 4 a 15 segundos.");
        }
    }

    /** Resolve o formato vertical do perfil mantendo rascunho e campanha financeiramente distintos. */
    private String ratio(ProviderPreflightJob job) {
        return "DRAFT_INSTAGRAM".equalsIgnoreCase(job.productionProfile())
                ? "720:1280"
                : "1080:1920";
    }

    /** Descreve o PDE como experiência digital de IA, sem convertê-lo em objeto físico. */
    private String productInfo(ProviderPreflightJob job) {
        return renderTemplate(
                productInfoTemplate,
                Map.of(
                        "{{TITLE}}", fallback(job.title(), "MUSA"),
                        "{{OBJECTIVE}}", fallback(job.objective(), job.learningObjective()),
                        "{{DIGITAL_EXPERIENCE}}",
                                fallback(job.objectBible(), "diagnóstico e plano digital personalizados"),
                        "{{CTA}}", job.ctaText().trim()));
    }

    /** Converte a reprovação anterior em direção visual verificável para a tomada premium. */
    private String userConcept(ProviderPreflightJob job) {
        return renderTemplate(
                userConceptTemplate,
                Map.of(
                        "{{APPROVED_MESSAGE}}", fallback(job.captionPlan(), job.scriptText()),
                        "{{CONTINUITY}}", fallback(job.continuityRules(), job.characterBible()),
                        "{{EDITING_NOTES}}", fallback(job.editingNotes(), "ritmo claro para Reels"),
                        "{{QUALITY_GATE}}", fallback(job.qualityGate(), job.successCriterion())));
    }

    /** Resolve placeholders conhecidos sem executar template arbitrário vindo do projeto. */
    private String renderTemplate(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> value : values.entrySet()) {
            result = result.replace(value.getKey(), value.getValue());
        }
        return result.trim();
    }

    /** Lê integralmente um recurso do classpath e bloqueia o worker se o pacote estiver incompleto. */
    private String resource(String path) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Recurso ausente: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar contrato Product UGC; path={}", path, ex);
            throw new IllegalStateException("Contrato Product UGC não foi empacotado: " + path, ex);
        }
    }

    /** Confirma placeholders e estrutura mínima do schema antes da primeira requisição. */
    private void validateContractResources() {
        List<String> productPlaceholders =
                List.of("{{TITLE}}", "{{OBJECTIVE}}", "{{DIGITAL_EXPERIENCE}}", "{{CTA}}");
        List<String> conceptPlaceholders =
                List.of("{{APPROVED_MESSAGE}}", "{{CONTINUITY}}", "{{EDITING_NOTES}}", "{{QUALITY_GATE}}");
        try {
            JsonNode schema = new ObjectMapper().readTree(taskSchema);
            boolean valid = productPlaceholders.stream().allMatch(productInfoTemplate::contains)
                    && conceptPlaceholders.stream().allMatch(userConceptTemplate::contains)
                    && schema.isObject()
                    && schema.path("$id").asText().endsWith("response-schema.json")
                    && schema.path("$defs").path("accepted").isObject()
                    && schema.path("$defs").path("completed").isObject();
            if (!valid) throw new IllegalArgumentException("Prompt ou schema sem os campos obrigatórios.");
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.error(
                    "Contrato Product UGC versionado é inválido; prompts={} {} schema={}",
                    PRODUCT_INFO_PATH,
                    USER_CONCEPT_PATH,
                    TASK_SCHEMA_PATH,
                    ex);
            throw new IllegalStateException("Contrato Product UGC versionado é inválido.", ex);
        }
    }

    /** Calcula a identidade do prompt e schema exatos carregados pelo executor. */
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 indisponível para o contrato Product UGC", ex);
            throw new IllegalStateException("SHA-256 indisponível para o contrato Product UGC.", ex);
        }
    }

    /** Confirma URL pública segura sem realizar download na fase de montagem do contrato. */
    private boolean isHttps(String value) {
        if (!StringUtils.hasText(value)) return false;
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && StringUtils.hasText(uri.getHost());
        } catch (IllegalArgumentException ex) {
            log.warn("Referência Product UGC não é uma URL válida; value={}", value, ex);
            return false;
        }
    }

    /** Mantém HTTPS em produção e libera HTTP somente para loopback nos testes do adapter. */
    private boolean isApprovedOutputUri(String value) {
        if (isHttps(value)) return true;
        if (!allowLocalHttp || !StringUtils.hasText(value)) return false;
        try {
            URI uri = URI.create(value.trim());
            return "http".equalsIgnoreCase(uri.getScheme())
                    && Set.of("localhost", "127.0.0.1", "::1").contains(uri.getHost());
        } catch (IllegalArgumentException ex) {
            log.warn("Runway retornou URL de saída inválida; value={}", value, ex);
            return false;
        }
    }

    /** Limita texto segundo o contrato da API sem cortar um par substituto UTF-16. */
    private String limit(String value, int maximumUtf16Units) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() <= maximumUtf16Units) return normalized;
        int end = maximumUtf16Units;
        if (Character.isHighSurrogate(normalized.charAt(end - 1))) end--;
        return normalized.substring(0, end).stripTrailing();
    }

    /** Substitui contexto ausente por uma descrição aprovada já presente no job. */
    private String fallback(String value, String replacement) {
        if (StringUtils.hasText(value)) return value.trim();
        return StringUtils.hasText(replacement) ? replacement.trim() : "Contexto aprovado no Estúdio";
    }
}
