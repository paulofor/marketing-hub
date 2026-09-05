package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoScriptStatus;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: planejar por IA e aprovar deterministicamente storyboards antes do render pago de Apolo. */
@Service
public class ApolloStoryboardPlanner {
    private static final Logger log = LoggerFactory.getLogger(ApolloStoryboardPlanner.class);
    private static final String PROMPT_PATH = "prompts/apollo/v2/storyboard-planner.md";
    private static final String SCHEMA_PATH = "prompts/apollo/v2/storyboard-planner-schema.json";
    private static final Set<String> REQUIRED_ROLES = Set.of("HOOK_DOR", "RESULTADO", "MECANISMO", "PROVA", "CTA");
    private static final Map<String, Integer> NARRATIVE_PHASES = Map.of(
            "HOOK", 1, "SETUP", 2, "DISCOVERY", 3, "DEMONSTRATION", 4,
            "TRANSFORMATION", 5, "PROOF", 6, "CTA", 7);
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
        if (isProductUgc(job, metadata)) {
            return approveProductUgc(job, metadata, progressCallback);
        }
        if (!properties.getApolloPlanner().isEnabled()) {
            throw blocked("Planejador de IA de Apolo está desabilitado; provider pago não foi chamado.");
        }
        validatePrerequisites(profile, metadata);
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

    /** Aprova a receita pinada com poucos planos estáveis sem realizar outra chamada de IA. */
    private SalesVideoJob approveProductUgc(
            SalesVideoJob job, JsonNode metadata, ProgressCallback progressCallback) {
        progressCallback.onProgress(
                5,
                SalesVideoStatus.VIDEO_PROCESSING,
                "Apolo está validando a receita Product UGC, as fontes e os gates técnicos");
        String issue = productUgcIssue(metadata);
        if (issue != null) throw blocked("Product UGC bloqueado: " + issue);
        int expectedCredits = productUgcExpectedCredits(metadata);
        BigDecimal reservedCredits = metadata.path("providerReservedCredits").decimalValue();
        BigDecimal expectedCost = BigDecimal.valueOf(expectedCredits).movePointLeft(2);
        BigDecimal reservedCost = metadata.path("providerReservedCostUsd").decimalValue();
        BigDecimal budgetLimit = metadata.path("budgetLimitUsd").decimalValue();
        if (reservedCredits.compareTo(BigDecimal.valueOf(expectedCredits)) != 0
                || reservedCost.compareTo(expectedCost) != 0
                || expectedCost.compareTo(budgetLimit) > 0) {
            throw blocked("reserva, tarifa pinada ou teto financeiro divergentes");
        }
        ObjectNode audit = objectMapper.createObjectNode();
        audit.put("mode", "DETERMINISTIC_PINNED_RECIPE");
        audit.put("recipe", "product_ugc");
        audit.put("version", "2026-06");
        audit.put("researchApplicationRationale",
                "Cartões de vídeo e prazer audiovisual orientam planos estáveis, ritmo, clareza e recompensa sensorial.");
        ArrayNode cards = audit.putArray("appliedCardIds");
        apolloResearchCards(metadata).forEach(cards::add);
        ObjectNode enriched = metadata.deepCopy();
        enriched.set("apollo_planner_request", audit);
        enriched.set("apollo_planner_response", audit.deepCopy());
        enriched.put("apollo_planner_model", "DETERMINISTIC_PRODUCT_UGC_V1");
        enriched.put("apollo_planner_status", "APPROVED_PINNED_RECIPE");
        enriched.put("expectedCredits", expectedCredits);
        enriched.put("expectedCostUsd", expectedCost);
        enriched.put("budgetGate", "APPROVED_DETERMINISTICALLY");
        progressCallback.onProgress(
                10,
                SalesVideoStatus.VIDEO_PROCESSING,
                "Apolo aprovou Product UGC: " + expectedCredits
                        + " créditos dentro do teto de US$ " + budgetLimit);
        return withMetadata(job, enriched.toString());
    }

    /** Confere a receita, os cortes limitados, as referências, a copy única e as revisões exigidas. */
    private String productUgcIssue(JsonNode metadata) {
        if (!"product_ugc@2026-06".equals(metadata.path("runwayRouterConfigId").asText())
                || !"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION"
                        .equals(metadata.path("generation_strategy").asText())
                || metadata.path("targetDurationSeconds").asInt() < 4
                || metadata.path("targetDurationSeconds").asInt() > 15
                || metadata.path("sceneCount").asInt() != 1
                || metadata.path("assemblyRequired").asBoolean(true)) {
            return "receita, duração ou requisição única divergentes";
        }
        JsonNode gate = metadata.path("technicalQualityGate");
        if (gate.path("continuousTakeRequired").asBoolean(true)
                || !gate.path("intentionalSceneCutsAllowed").asBoolean(false)
                || gate.path("maximumSceneCuts").asInt() != 4
                || !gate.path("captionMustMatchNarration").asBoolean(false)
                || !gate.path("forbidMirrorOrReflection").asBoolean(false)
                || gate.path("maximumMeanMotionDelta").decimalValue()
                        .compareTo(new BigDecimal("1.25")) != 0
                || gate.path("maximumPeakMotionDelta").decimalValue()
                        .compareTo(new BigDecimal("12.0")) != 0) {
            return "gate de estabilidade, reflexo ou sincronismo incompleto";
        }
        JsonNode governance = metadata.path("referenceGovernance");
        if (!governance.path("productIsDigitalExperience").asBoolean(false)
                || !StringUtils.hasText(governance.path("presenterConsentEvidence").asText())
                || !StringUtils.hasText(governance.path("referenceRightsEvidence").asText())) {
            return "consentimento, direitos ou identidade PDE ausentes";
        }
        JsonNode finalization = metadata.path("premiumFinalization");
        if (!finalization.path("enabled").asBoolean(false)
                || !sameSpokenText(
                        finalization.path("captionText").asText(),
                        finalization.path("voiceOverScript").asText())
                || !reviewersComplete(finalization.path("requiredReviewers"))) {
            return "narração, legenda ou revisores obrigatórios divergentes";
        }
        Set<String> collections = apolloResearchCollections(metadata);
        if (!collections.containsAll(Set.of("video", "prazer-audio-visual"))) {
            return "cartões de vídeo e prazer audiovisual não foram entregues a Apolo";
        }
        return null;
    }

    /** Recalcula a tarifa da requisição congelada para não confiar apenas na reserva recebida. */
    private int productUgcExpectedCredits(JsonNode metadata) {
        JsonNode requests = readJson(metadata.path("runwayRouterRequestsJson").asText(""));
        if (!requests.isArray() || requests.size() != 1) {
            throw blocked("Product UGC exige uma única requisição congelada");
        }
        JsonNode request = requests.get(0);
        int duration = request.path("duration").asInt();
        String ratio = request.path("ratio").asText();
        if (!"2026-06".equals(request.path("version").asText())
                || duration != metadata.path("targetDurationSeconds").asInt()
                || duration < 4
                || duration > 15
                || !Set.of("720:1280", "1080:1920").contains(ratio)) {
            throw blocked("requisição Product UGC diverge da versão, duração ou resolução pinadas");
        }
        boolean fullHd = "1080:1920".equals(ratio);
        int base = fullHd ? 208 : 192;
        int additional = fullHd ? 40 : 36;
        return base + additional * (duration - 4);
    }

    /** Identifica o provider e a estratégia exatos sem inferir pela copy comercial. */
    private boolean isProductUgc(SalesVideoJob job, JsonNode metadata) {
        return "RUNWAY_PRODUCT_UGC".equalsIgnoreCase(job.providerName())
                && "RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION"
                        .equals(metadata.path("generation_strategy").asText());
    }

    /** Lista as coleções realmente selecionadas para Apolo no harness. */
    private Set<String> apolloResearchCollections(JsonNode metadata) {
        Set<String> result = new HashSet<>();
        for (JsonNode route : metadata.path("researchIntelligence").path("routes")) {
            if (!"videomaker".equals(route.path("agentKey").asText())) continue;
            for (JsonNode card : route.path("cards")) {
                if (StringUtils.hasText(card.path("collection").asText())) {
                    result.add(card.path("collection").asText());
                }
            }
        }
        return result;
    }

    /** Lista os IDs dos cartões preservados no parecer determinístico de Apolo. */
    private List<String> apolloResearchCards(JsonNode metadata) {
        List<String> result = new java.util.ArrayList<>();
        for (JsonNode route : metadata.path("researchIntelligence").path("routes")) {
            if (!"videomaker".equals(route.path("agentKey").asText())) continue;
            for (JsonNode card : route.path("cards")) {
                if (StringUtils.hasText(card.path("cardId").asText())) {
                    result.add(card.path("cardId").asText());
                }
            }
        }
        return List.copyOf(result);
    }

    /** Compara a sequência falada ignorando somente pausas, acentos e pontuação. */
    private boolean sameSpokenText(String first, String second) {
        return normalizeSpokenText(first).equals(normalizeSpokenText(second));
    }

    /** Normaliza a copy falada sem permitir troca, inserção ou remoção de palavras. */
    private String normalizeSpokenText(String value) {
        String decomposed = java.text.Normalizer.normalize(
                value == null ? "" : value.replace('|', ' '), java.text.Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    /** Exige revisão psicológica, de integridade e humana antes de publicação. */
    private boolean reviewersComplete(JsonNode reviewers) {
        if (!reviewers.isArray()) return false;
        Set<String> values = new HashSet<>();
        reviewers.forEach(value -> values.add(value.asText()));
        return values.containsAll(Set.of("Psique", "Temis", "HUMAN"));
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
        if (!cuts.isArray() || cuts.size() < 5 || cuts.size() != originalCount) {
            return GateDecision.blocked("quantidade de cortes diferente do plano aprovado");
        }
        String researchIssue = researchApplicationIssue(metadata, plan);
        if (researchIssue != null) return GateDecision.blocked(researchIssue);
        int targetDuration = metadata.path("targetDurationSeconds").asInt(0);
        int totalDuration = 0;
        Set<String> roles = new HashSet<>();
        Set<String> objectives = new HashSet<>();
        int previousPhase = 0;
        for (int index = 0; index < cuts.size(); index++) {
            JsonNode cut = cuts.get(index);
            if (cut.path("order").asInt() != index + 1) return GateDecision.blocked("ordem de cortes inválida");
            totalDuration += cut.path("durationSeconds").asInt();
            roles.add(cut.path("commercialRole").asText());
            String objective = normalize(cut.path("visualObjective").asText());
            String phase = cut.path("narrativePhase").asText();
            int phaseOrder = NARRATIVE_PHASES.getOrDefault(phase, 0);
            if (phaseOrder == 0 || phaseOrder < previousPhase) {
                return GateDecision.blocked("sequência narrativa inválida");
            }
            previousPhase = phaseOrder;
            if (!StringUtils.hasText(cut.path("continuityAnchor").asText())) {
                return GateDecision.blocked("continuidade visual não definida");
            }
            if (!objectives.add(objective)) return GateDecision.blocked("cenas visualmente repetidas");
            if (containsEmbeddedTextInstruction(objective)) {
                return GateDecision.blocked("texto solicitado dentro do vídeo do provider");
            }
        }
        if (!"HOOK".equals(cuts.get(0).path("narrativePhase").asText())
                || !"CTA".equals(cuts.get(cuts.size() - 1).path("narrativePhase").asText())) {
            return GateDecision.blocked("história deve começar no gancho e terminar no CTA");
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

    /** Valida que Apolo aplicou somente cartões entregues e cobriu cada coleção selecionada. */
    private String researchApplicationIssue(JsonNode metadata, JsonNode plan) {
        JsonNode intelligence = metadata.path("researchIntelligence");
        if (intelligence.isMissingNode() || intelligence.isNull()) return null;
        Map<String, String> collectionByCard = new java.util.LinkedHashMap<>();
        for (JsonNode route : intelligence.path("routes")) {
            if (!"videomaker".equals(route.path("agentKey").asText())) continue;
            for (JsonNode card : route.path("cards")) {
                collectionByCard.put(card.path("cardId").asText(), card.path("collection").asText());
            }
        }
        if (collectionByCard.isEmpty()) return "biblioteca de pesquisa entregue sem cartões de Apolo";
        JsonNode applied = plan.path("appliedCardIds");
        if (!applied.isArray() || applied.isEmpty()) {
            return "storyboard não declarou os cartões de pesquisa aplicados";
        }
        Set<String> appliedCollections = new HashSet<>();
        for (JsonNode cardId : applied) {
            String collection = collectionByCard.get(cardId.asText());
            if (collection == null) return "storyboard citou cartão de pesquisa não entregue";
            appliedCollections.add(collection);
        }
        Set<String> deliveredCollections = new HashSet<>(collectionByCard.values());
        if (!appliedCollections.containsAll(deliveredCollections)) {
            return "storyboard não aplicou ao menos um cartão de cada coleção entregue";
        }
        if (!StringUtils.hasText(plan.path("researchApplicationRationale").asText())) {
            return "storyboard não explicou a aplicação da pesquisa";
        }
        return null;
    }

    /** Bloqueia a IA e o provider quando roteiro, duração ou plano-base ainda não estão prontos. */
    private void validatePrerequisites(SalesVideoProfile profile, JsonNode metadata) {
        SalesVideoScript script = profile.latestScript();
        if (script == null || script.status() != SalesVideoScriptStatus.APPROVED
                || !StringUtils.hasText(script.scriptText())
                || !StringUtils.hasText(script.hookText())
                || !StringUtils.hasText(script.ctaText())) {
            throw blocked("roteiro aprovado, gancho e CTA são obrigatórios antes do planejamento");
        }
        int target = metadata.path("targetDurationSeconds").asInt(0);
        int clipDuration = metadata.path("providerClipDurationSeconds").asInt(0);
        int sceneCount = metadata.path("sceneCount").asInt(0);
        int cutCount = metadata.path("cut_plan").size();
        if (target < 15 || clipDuration < 1 || sceneCount != (target + clipDuration - 1) / clipDuration
                || cutCount < 5 || cutCount > 48) {
            throw blocked("duração, clipes do provider e plano de cortes ainda não estão consistentes");
        }
    }

    /** Converte o contrato da IA para o contrato de cortes já consumido pelos providers. */
    private ArrayNode toCanonicalCuts(JsonNode cuts) {
        ArrayNode result = objectMapper.createArrayNode();
        cuts.forEach(cut -> {
            ObjectNode canonical = result.addObject();
            canonical.put("order", cut.path("order").asInt());
            canonical.put("duration_seconds", cut.path("durationSeconds").asInt());
            canonical.put("role", cut.path("commercialRole").asText());
            canonical.put("narrative_phase", cut.path("narrativePhase").asText());
            canonical.put("visual_objective", cut.path("visualObjective").asText());
            canonical.put("continuity_anchor", cut.path("continuityAnchor").asText());
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

    /** Detecta ordens positivas de texto sem confundir proibições visuais com solicitação. */
    private boolean containsEmbeddedTextInstruction(String objective) {
        String normalized = normalize(objective);
        String withoutProhibitions = normalized.replaceAll(
                "\\b(sem|nao|não|evitar)\\s+(?:[a-z0-9áàâãéêíóôõúç]+\\s+){0,3}"
                        + "(texto|legenda|palavra|preco|logo|cta escrito|interface)\\b",
                " ");
        return withoutProhibitions.matches(
                ".*\\b(mostrar|exibir|incluir|aplicar|gerar|inserir|desenhar|revelar)\\b"
                        + "(?:\\s+[a-z0-9áàâãéêíóôõúç]+){0,4}\\s+"
                        + "\\b(texto|legenda|palavra|preco|logo|cta escrito|interface)\\b.*");
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
            log.error("Contrato JSON inválido no planejamento determinístico de Apolo", ex);
            throw blocked("Contrato JSON do planejamento é inválido.", ex);
        }
    }

    /** Carrega integralmente prompt ou schema versionado do classpath. */
    private String resource(String path) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Recurso ausente: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar recurso versionado do planejador de Apolo; path={}", path, ex);
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
