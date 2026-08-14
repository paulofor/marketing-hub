package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.client.ApolloPlanningAiClient;
import com.marketinghub.videomanagement.service.provider.ProgressCallback;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: planejar por IA e aprovar deterministicamente storyboards antes do render pago de Apolo. */
@Service
public class ApolloStoryboardPlanner {
    private static final String PROMPT_PATH = "prompts/apollo/v2/storyboard-planner.md";
    private static final String SCHEMA_PATH = "prompts/apollo/v2/storyboard-planner-schema.json";
    private static final Set<String> REQUIRED_ROLES = Set.of("HOOK_DOR", "RESULTADO", "MECANISMO", "CTA");
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final ApolloPlanningAiClient aiClient;

    /** Configura o cliente de IA sem transferir a ele autoridade financeira. */
    public ApolloStoryboardPlanner(VideoManagementProperties properties,
                                   ObjectMapper objectMapper,
                                   ApolloPlanningAiClient aiClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.aiClient = aiClient;
    }

    /** Planeja somente jobs autônomos de Apolo e bloqueia o provider se o gate não for aprovado. */
    public SalesVideoJob planAndApprove(SalesVideoJob job,
                                        SalesVideoProfile profile,
                                        ProgressCallback progressCallback) {
        JsonNode metadata = readJson(job.metadataJson());
        if (job.jobType() != SalesVideoJobType.RENDER || !metadata.has("videoProductionCycleId")) {
            return job;
        }
        if (!properties.getApolloPlanner().isEnabled()) {
            throw blocked("Planejador de IA de Apolo está desabilitado; provider pago não foi chamado.");
        }
        progressCallback.onProgress(5, SalesVideoStatus.VIDEO_PROCESSING,
                "Apolo está planejando o storyboard antes do gate de orçamento");
        ObjectNode request = buildRequest(job, profile, metadata);
        JsonNode response = aiClient.plan(job.id(), request);
        JsonNode plan = extractPlan(response);
        progressCallback.onProgress(8, SalesVideoStatus.VIDEO_PROCESSING,
                "Apolo concluiu o planejamento criativo; gate determinístico pendente",
                planningAudit(request, response, plan));
        GateDecision decision = validate(metadata, plan, job.providerName());
        if (!decision.approved()) {
            throw blocked("Storyboard bloqueado: " + decision.reason());
        }
        ObjectNode enriched = metadata.deepCopy();
        enriched.set("cut_plan", toCanonicalCuts(plan.path("cuts")));
        enriched.set("apollo_ai_plan", plan);
        enriched.set("apollo_planner_request", request);
        enriched.set("apollo_planner_response", response);
        enriched.put("apollo_planner_model", properties.getApolloPlanner().getModel());
        enriched.put("apollo_planner_status", "APPROVED");
        enriched.put("expectedCredits", decision.expectedCredits());
        enriched.put("expectedCostUsd", decision.expectedCostUsd());
        enriched.put("budgetGate", "APPROVED_DETERMINISTICALLY");
        progressCallback.onProgress(10, SalesVideoStatus.VIDEO_PROCESSING,
                "Storyboard aprovado: %d créditos previstos dentro do teto de US$ %s"
                        .formatted(decision.expectedCredits(), decision.budgetLimitUsd()));
        return withMetadata(job, enriched.toString());
    }

    /** Serializa request, response e plano para persistência auditável antes do gate financeiro. */
    private String planningAudit(JsonNode request, JsonNode response, JsonNode plan) {
        ObjectNode audit = objectMapper.createObjectNode();
        audit.put("eventType", "APOLLO_STORYBOARD_PLANNED");
        audit.put("model", properties.getApolloPlanner().getModel());
        audit.set("request", request);
        audit.set("response", response);
        audit.set("plan", plan);
        return audit.toString();
    }

    /** Monta a requisição Responses API com prompt e schema versionados e modo Flex. */
    private ObjectNode buildRequest(SalesVideoJob job, SalesVideoProfile profile, JsonNode metadata) {
        String context = metadata.toString() + "\nPerfil: " + objectMapper.valueToTree(profile);
        String prompt = resource(PROMPT_PATH).replace("{{CONTEXT}}", context);
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.getApolloPlanner().getModel());
        request.put("service_tier", "flex");
        request.put("input", prompt);
        ObjectNode format = request.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "apollo_storyboard_v2");
        format.put("strict", true);
        format.set("schema", readJson(resource(SCHEMA_PATH)));
        request.put("store", true);
        request.put("metadata", objectMapper.valueToTree(Map.of("jobId", String.valueOf(job.id()), "agent", "APOLLO")));
        return request;
    }

    /** Valida custo, diversidade, cobertura comercial, duração e pós-produção sem confiar na IA. */
    GateDecision validate(JsonNode metadata, JsonNode plan, String providerName) {
        JsonNode cuts = plan.path("cuts");
        int originalCount = metadata.path("cut_plan").size();
        if (!cuts.isArray() || cuts.size() < 4 || cuts.size() != originalCount) {
            return GateDecision.blocked("quantidade de cortes diferente do plano aprovado");
        }
        int targetDuration = metadata.path("targetDurationSeconds").asInt(0);
        int totalDuration = 0;
        Set<String> roles = new HashSet<>();
        Set<String> objectives = new HashSet<>();
        for (int index = 0; index < cuts.size(); index++) {
            JsonNode cut = cuts.get(index);
            if (cut.path("order").asInt() != index + 1) return GateDecision.blocked("ordem de cortes inválida");
            totalDuration += cut.path("durationSeconds").asInt();
            roles.add(cut.path("commercialRole").asText());
            String objective = normalize(cut.path("visualObjective").asText());
            if (!objectives.add(objective)) return GateDecision.blocked("cenas visualmente repetidas");
            if (containsEmbeddedTextInstruction(objective)) {
                return GateDecision.blocked("texto solicitado dentro do vídeo do provider");
            }
        }
        if (totalDuration != targetDuration) return GateDecision.blocked("duração total diferente do projeto");
        if (!roles.containsAll(REQUIRED_ROLES)) return GateDecision.blocked("funções comerciais incompletas");
        int clipCount = metadata.path("sceneCount").asInt(0);
        int clipDuration = metadata.path("providerClipDurationSeconds").asInt(0);
        int credits = clipCount * clipDuration * creditsPerSecond(providerName);
        BigDecimal cost = BigDecimal.valueOf(credits).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        BigDecimal limit = metadata.path("budgetLimitUsd").decimalValue();
        if (clipCount < 1 || clipDuration < 1 || limit.signum() <= 0 || cost.compareTo(limit) > 0) {
            return GateDecision.blocked("custo previsto de US$ %s excede o teto de US$ %s".formatted(cost, limit));
        }
        return new GateDecision(true, "aprovado", credits, cost, limit);
    }

    /** Converte o contrato da IA para o contrato de cortes já consumido pelos providers. */
    private ArrayNode toCanonicalCuts(JsonNode cuts) {
        ArrayNode result = objectMapper.createArrayNode();
        cuts.forEach(cut -> {
            ObjectNode canonical = result.addObject();
            canonical.put("order", cut.path("order").asInt());
            canonical.put("duration_seconds", cut.path("durationSeconds").asInt());
            canonical.put("role", cut.path("commercialRole").asText());
            canonical.put("visual_objective", cut.path("visualObjective").asText());
            canonical.put("reuse_existing_material", cut.path("reuseExistingMaterial").asBoolean());
            canonical.put("post_production_text", cut.path("postProductionText").asText());
        });
        return result;
    }

    /** Extrai o JSON estruturado retornado pela Responses API. */
    private JsonNode extractPlan(JsonNode response) {
        if (response == null) throw blocked("Planejador retornou resposta vazia.");
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return readJson(content.path("text").asText());
                }
            }
        }
        throw blocked("Planejador não retornou storyboard estruturado.");
    }

    /** Calcula a tarifa conservadora por segundo para o provider selecionado. */
    private int creditsPerSecond(String providerName) {
        String provider = providerName == null ? "" : providerName.toUpperCase(Locale.ROOT);
        if (provider.contains("SEEDANCE_2_5")) return 30;
        if (provider.contains("GEN_4_TURBO")) return 5;
        return 12;
    }

    /** Detecta instruções que deveriam permanecer na pós-produção determinística. */
    private boolean containsEmbeddedTextInstruction(String objective) {
        return objective.matches(".*\\b(texto|legenda|palavra|preco|logo|cta escrito|interface)\\b.*");
    }

    /** Normaliza texto para comparação de redundância e termos proibidos. */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9áàâãéêíóôõúç ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    /** Lê JSON e converte falha de contrato em bloqueio anterior ao provider. */
    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            throw blocked("Contrato JSON do planejamento é inválido.", ex);
        }
    }

    /** Carrega integralmente prompt ou schema versionado do classpath. */
    private String resource(String path) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Recurso ausente: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw blocked("Não foi possível carregar o contrato do planejador.", ex);
        }
    }

    /** Copia o record imutável substituindo apenas o metadata aprovado. */
    private SalesVideoJob withMetadata(SalesVideoJob job, String metadata) {
        return new SalesVideoJob(job.id(), job.profileId(), job.scriptId(), job.tenantId(), job.providerFamily(),
                job.providerName(), job.providerJobId(), job.jobType(), job.status(), job.retryAttempt(),
                job.retryReason(), job.retryOfJobId(), job.retryNotes(), job.progressPercent(), job.failureCode(),
                job.failureDetail(), job.requestedBy(), job.requestedAt(), job.startedAt(), job.finishedAt(),
                job.expiresAt(), job.assetId(), job.posterAssetId(), job.vttAssetId(), metadata, job.createdAt(),
                job.updatedAt());
    }

    /** Cria uma falha funcional não recuperável que impede repetição automática. */
    private VideoProviderException blocked(String message) {
        return new VideoProviderException("APOLLO_STORYBOARD_BLOCKED", message);
    }

    /** Preserva a causa completa quando a integração de planejamento falha. */
    private VideoProviderException blocked(String message, Throwable cause) {
        return new VideoProviderException("APOLLO_STORYBOARD_BLOCKED", message, cause);
    }

    /** Representa o resultado determinístico do gate financeiro e editorial. */
    record GateDecision(boolean approved, String reason, int expectedCredits,
                        BigDecimal expectedCostUsd, BigDecimal budgetLimitUsd) {
        /** Cria uma decisão bloqueada sem inventar custo aprovado. */
        static GateDecision blocked(String reason) {
            return new GateDecision(false, reason, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
