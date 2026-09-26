package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marketinghub.videomanagement.client.ApolloPlanningAiClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoJobType;
import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.client.dto.SalesVideoScript;
import com.marketinghub.videomanagement.client.dto.SalesVideoScriptStatus;
import com.marketinghub.videomanagement.client.dto.SalesVideoProviderFamily;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.ProgressCallback;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: comprovar o gate determinístico anterior ao consumo audiovisual de Apolo. */
class ApolloStoryboardPlannerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ApolloStoryboardPlanner planner;
    private ApolloPlanningAiClient aiClient;

    /** Cria o planejador sem executar integrações externas. */
    @BeforeEach
    void setUp() {
        aiClient = mock(ApolloPlanningAiClient.class);
        planner = new ApolloStoryboardPlanner(new VideoManagementProperties(), objectMapper, aiClient);
    }

    /** Exige raciocínio máximo no request efetivo e na evidência preservada pelo fluxo completo. */
    @Test
    void shouldPlanWithMaximumReasoningAndPreserveRequestInResult() throws Exception {
        var metadata = (com.fasterxml.jackson.databind.node.ObjectNode) metadata("20.00");
        metadata.put("videoProductionCycleId", 1);
        SalesVideoJob job = mock(SalesVideoJob.class);
        when(job.id()).thenReturn(1L);
        when(job.jobType()).thenReturn(SalesVideoJobType.RENDER);
        when(job.providerName()).thenReturn("RUNWAY_SEEDANCE_2_5");
        when(job.metadataJson()).thenReturn(metadata.toString());
        SalesVideoScript script = new SalesVideoScript(1L, 1, "Roteiro", "Gancho", "CTA", null,
                null, null, null, null, SalesVideoScriptStatus.APPROVED, null, null, null);
        SalesVideoProfile profile = new SalesVideoProfile(1L, 1L, null, "TEST", "Teste local",
                null, null, null, "pt-BR", 30, SalesVideoStatus.VIDEO_REQUESTED,
                null, null, script, null);
        JsonNode approvedPlan = plan(false);
        when(aiClient.plan(eq(1L), any(), any())).thenAnswer(invocation -> {
            JsonNode request = invocation.getArgument(1);
            assertThat(request.at("/reasoning/effort").asText()).isEqualTo("max");
            assertThat(request.path("service_tier").asText()).isEqualTo("flex");
            var response = objectMapper.createObjectNode();
            response.putArray("output").addObject().putArray("content").addObject()
                    .put("type", "output_text").put("text", approvedPlan.toString());
            return response;
        });

        SalesVideoJob result = planner.planAndApprove(job, profile, mock(ProgressCallback.class));

        JsonNode persisted = objectMapper.readTree(result.metadataJson());
        assertThat(persisted.at("/apollo_planner_request/reasoning/effort").asText()).isEqualTo("max");
        assertThat(persisted.path("apollo_planner_status").asText()).isEqualTo("APPROVED");
    }

    /** Encaminha o erro HTTP auditado ao backend antes de bloquear o storyboard e o render. */
    @Test
    void shouldPersistHttpFailureWithoutCallingItStoryboardRejection() throws Exception {
        var metadata = (com.fasterxml.jackson.databind.node.ObjectNode) metadata("20.00");
        metadata.put("videoProductionCycleId", 91001);
        SalesVideoJob job = mock(SalesVideoJob.class);
        when(job.id()).thenReturn(91001L);
        when(job.jobType()).thenReturn(SalesVideoJobType.RENDER);
        when(job.providerName()).thenReturn("RUNWAY_SEEDANCE_2_5");
        when(job.metadataJson()).thenReturn(metadata.toString());
        SalesVideoScript script = new SalesVideoScript(91001L, 1, "Roteiro", "Gancho", "CTA", null,
                null, null, null, null, SalesVideoScriptStatus.APPROVED, null, null, null);
        SalesVideoProfile profile = new SalesVideoProfile(91001L, 91001L, null, "TEST", "Teste local",
                null, null, null, "pt-BR", 30, SalesVideoStatus.VIDEO_REQUESTED,
                null, null, script, null);
        var audit = objectMapper.readTree("{\"status\":\"REJECTED\",\"httpStatus\":429,\"rawResponse\":\"quota sintética\"}");
        when(aiClient.plan(eq(91001L), any(), any())).thenAnswer(invocation -> {
            java.util.function.Consumer<JsonNode> sink = invocation.getArgument(2);
            sink.accept(audit);
            throw new com.marketinghub.videomanagement.service.provider.VideoProviderException(
                    "APOLLO_PLANNING_ACCOUNT_BLOCKED", "Quota sintética");
        });
        var callback = mock(ProgressCallback.class);
        assertThatThrownBy(() -> planner.planAndApprove(job, profile, callback))
                .isInstanceOfSatisfying(com.marketinghub.videomanagement.service.provider.VideoProviderException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("APOLLO_PLANNING_ACCOUNT_BLOCKED"));
        org.mockito.Mockito.verify(callback).onProgress(eq(5), eq(SalesVideoStatus.VIDEO_PROCESSING), any(), eq(audit.toString()));
    }

    /** Aprova um storyboard distinto cujo custo previsto permanece dentro do teto. */
    @Test
    void shouldApproveCommerciallyCompletePlanWithinBudget() throws Exception {
        ApolloStoryboardPlanner.GateDecision decision = planner.validate(metadata("20.00"), plan(false),
                "RUNWAY_SEEDANCE_2_5");

        assertThat(decision.approved()).isTrue();
        assertThat(decision.expectedCredits()).isEqualTo(900);
        assertThat(decision.expectedCostUsd()).isEqualByComparingTo("9.00");
    }

    /** Bloqueia o provider quando o custo previsto ultrapassa o teto aprovado. */
    @Test
    void shouldBlockPlanAboveBudget() throws Exception {
        ApolloStoryboardPlanner.GateDecision decision = planner.validate(metadata("8.99"), plan(false),
                "RUNWAY_SEEDANCE_2_5");

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).contains("excede o teto");
    }

    /** Bloqueia orientação para desenhar texto no vídeo em vez de usar pós-produção. */
    @Test
    void shouldBlockEmbeddedTextInstruction() throws Exception {
        ApolloStoryboardPlanner.GateDecision decision = planner.validate(metadata("20.00"), plan(true),
                "RUNWAY_SEEDANCE_2_5");

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).contains("texto solicitado");
    }

    /** Aceita proibições de texto e interface sem convertê-las em ordem positiva ao provider. */
    @Test
    void shouldAllowExplicitProhibitionOfEmbeddedText() throws Exception {
        JsonNode safePlan = plan(false);
        ((com.fasterxml.jackson.databind.node.ObjectNode) safePlan.path("cuts").get(4))
                .put("visualObjective", "Tocar o celular sem revelar interface legível e sem mostrar texto");

        ApolloStoryboardPlanner.GateDecision decision = planner.validate(
                metadata("20.00"), safePlan, "RUNWAY_SEEDANCE_2_5");

        assertThat(decision.approved()).isTrue();
    }

    /** Reproduz respostas reais de sucesso e falso bloqueio, preservando o plano e sua auditoria. */
    @ParameterizedTest
    @ValueSource(strings = {"21237", "21240"})
    void shouldReplayHistoricalPlanWithoutChangingItsInstructions(String historicalJob) throws Exception {
        JsonNode savedPlan;
        try (var input = getClass().getResourceAsStream("/fixtures/apollo/storyboard-" + historicalJob + ".json")) {
            savedPlan = objectMapper.readTree(input);
        }
        var metadata = (com.fasterxml.jackson.databind.node.ObjectNode) metadata("8.00");
        metadata.put("videoProductionCycleId", 91001);
        metadata.put("targetDurationSeconds", 15);
        metadata.put("providerClipDurationSeconds", 10);
        SalesVideoJob job = mock(SalesVideoJob.class);
        when(job.id()).thenReturn(91001L);
        when(job.jobType()).thenReturn(SalesVideoJobType.RENDER);
        when(job.providerName()).thenReturn("RUNWAY_ROUTER");
        when(job.metadataJson()).thenReturn(metadata.toString());
        SalesVideoScript script = new SalesVideoScript(91001L, 1, "Roteiro de teste", "Gancho", "CTA", null,
                null, null, null, null, SalesVideoScriptStatus.APPROVED, null, null, null);
        SalesVideoProfile profile = new SalesVideoProfile(91001L, 91001L, null, "TEST", "Replay local",
                null, null, null, "pt-BR", 15, SalesVideoStatus.VIDEO_REQUESTED,
                null, null, script, null);
        var response = objectMapper.createObjectNode();
        response.putArray("output").addObject().putArray("content").addObject()
                .put("type", "output_text").put("text", savedPlan.toString());
        when(aiClient.plan(eq(91001L), any(), any())).thenReturn(response);

        SalesVideoJob approved = planner.planAndApprove(job, profile, mock(ProgressCallback.class));

        JsonNode persisted = objectMapper.readTree(approved.metadataJson());
        assertThat(persisted.path("apollo_planner_status").asText()).isEqualTo("APPROVED");
        assertThat(persisted.path("apollo_ai_plan")).isEqualTo(savedPlan);
        assertThat(persisted.path("apollo_planner_response")).isEqualTo(response);
        assertThat(persisted.path("expectedCostUsd").decimalValue()).isLessThanOrEqualTo(new java.math.BigDecimal("8.00"));
    }

    /** Aprova o storyboard quando Apolo aplica ao menos um cartão de cada coleção entregue. */
    @Test
    void shouldApproveDeliveredResearchCardsAcrossCollections() throws Exception {
        JsonNode plan = plan(false);
        appliedCards(plan, "RI1-AAAAAAAAAAAA", "RI1-BBBBBBBBBBBB");

        ApolloStoryboardPlanner.GateDecision decision = planner.validate(metadataWithResearch(), plan,
                "RUNWAY_SEEDANCE_2_5");

        assertThat(decision.approved()).isTrue();
    }

    /** Bloqueia antes do provider quando Apolo inventa um cartão não entregue. */
    @Test
    void shouldBlockUndeliveredResearchCard() throws Exception {
        JsonNode plan = plan(false);
        appliedCards(plan, "RI1-AAAAAAAAAAAA", "RI1-CCCCCCCCCCCC");

        ApolloStoryboardPlanner.GateDecision decision = planner.validate(metadataWithResearch(), plan,
                "RUNWAY_SEEDANCE_2_5");

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).contains("não entregue");
    }

    /** Bloqueia antes do provider quando uma coleção selecionada não influencia o storyboard. */
    @Test
    void shouldBlockIgnoredResearchCollection() throws Exception {
        JsonNode plan = plan(false);
        appliedCards(plan, "RI1-AAAAAAAAAAAA");

        ApolloStoryboardPlanner.GateDecision decision = planner.validate(metadataWithResearch(), plan,
                "RUNWAY_SEEDANCE_2_5");

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).contains("cada coleção");
    }

    /** Aprova Product UGC por receita pinada sem criar cortes ou chamar outra IA. */
    @Test
    void shouldApprovePinnedProductUgcWithoutCallingPlanningAi() throws Exception {
        SalesVideoJob result = planner.planAndApprove(
                productUgcJob(productUgcMetadata(true).toString()),
                mock(SalesVideoProfile.class),
                mock(ProgressCallback.class));

        JsonNode metadata = objectMapper.readTree(result.metadataJson());
        assertThat(metadata.path("apollo_planner_status").asText())
                .isEqualTo("APPROVED_PINNED_RECIPE");
        assertThat(metadata.path("expectedCredits").asInt()).isEqualTo(648);
        assertThat(metadata.path("expectedCostUsd").decimalValue()).isEqualByComparingTo("6.48");
        assertThat(metadata.at("/apollo_planner_request/appliedCardIds").size()).isEqualTo(2);
        verifyNoInteractions(aiClient);
    }

    /** Bloqueia Product UGC quando o harness não entrega as duas coleções de Apolo. */
    @Test
    void shouldBlockProductUgcWithoutAudiovisualResearchCoverage() throws Exception {
        assertThatThrownBy(() -> planner.planAndApprove(
                productUgcJob(productUgcMetadata(false).toString()),
                mock(SalesVideoProfile.class),
                mock(ProgressCallback.class)))
                .hasMessageContaining("prazer audiovisual");

        verifyNoInteractions(aiClient);
    }

    /** Aprova o plano editorial local com prova real sem chamar o planejador de IA. */
    @Test
    void shouldApproveEditorialMotionWithoutCallingPlanningAi() throws Exception {
        String metadata = """
                {"videoProductionCycleId":93,"budgetLimitUsd":15.38,
                 "providerReservedCredits":0,"providerReservedCostUsd":0,
                 "generation_strategy":"DETERMINISTIC_EDITORIAL_MOTION_FROM_APPROVED_PRODUCT_PROOF",
                 "publicationAllowed":false,"targetDurationSeconds":15,"sceneCount":1,
                 "assemblyRequired":false,
                 "post_production":{"product_proof":{
                   "contractVersion":"PDE_PRIVATE_VIDEO_PROOF_V1",
                   "contentPath":"/api/sales-videos/projects/61/product-proof",
                   "sha256":"%s","commercialEvidenceClaimed":false}},
                 "cut_plan":[
                   {"duration_seconds":3,"role":"HOOK_DOR"},
                   {"duration_seconds":3,"role":"MECANISMO"},
                   {"duration_seconds":3,"role":"RESULTADO"},
                   {"duration_seconds":3,"role":"PROVA"},
                   {"duration_seconds":3,"role":"CTA"}],
                 "premiumFinalization":{"enabled":true,
                   "captionText":"Organize o que você já tem | Veja sua rotina completa",
                   "voiceOverScript":"Organize o que você já tem Veja sua rotina completa",
                   "requiredReviewers":["Psique","Temis","HUMAN"]}}
                """.formatted("a".repeat(64));

        SalesVideoJob result = planner.planAndApprove(
                editorialJob(metadata), mock(SalesVideoProfile.class), mock(ProgressCallback.class));

        JsonNode persisted = objectMapper.readTree(result.metadataJson());
        assertThat(persisted.path("apollo_planner_status").asText())
                .isEqualTo("APPROVED_LOCAL_EDITORIAL");
        assertThat(persisted.path("expectedCostUsd").decimalValue()).isZero();
        verifyNoInteractions(aiClient);
    }

    /** Monta o contexto financeiro e editorial já aprovado no backend. */
    private JsonNode metadata(String budget) throws Exception {
        return objectMapper.readTree("""
                {"budgetLimitUsd":%s,"targetDurationSeconds":30,"sceneCount":2,
                 "providerClipDurationSeconds":15,"cut_plan":[{},{},{},{},{}]}
                """.formatted(budget));
    }

    /** Monta o mesmo metadata com a rota seletiva e rastreável de Apolo. */
    private JsonNode metadataWithResearch() throws Exception {
        return objectMapper.readTree("""
                {"budgetLimitUsd":20.00,"targetDurationSeconds":30,"sceneCount":2,
                 "providerClipDurationSeconds":15,"cut_plan":[{},{},{},{},{}],
                 "researchIntelligence":{"contractVersion":"HARNESS_RESEARCH_INTELLIGENCE_V1","routes":[
                   {"agentKey":"videomaker","cards":[
                     {"cardId":"RI1-AAAAAAAAAAAA","collection":"video"},
                     {"cardId":"RI1-BBBBBBBBBBBB","collection":"prazer-audio-visual"}
                   ]}
                 ]}}
                """);
    }

    /** Monta o contrato completo da receita premium com ou sem a segunda coleção obrigatória. */
    private JsonNode productUgcMetadata(boolean completeResearch) throws Exception {
        String secondCard = completeResearch
                ? ",{\"cardId\":\"RI1-BBBBBBBBBBBB\",\"collection\":\"prazer-audio-visual\"}"
                : "";
        return objectMapper.readTree("""
                {"videoProductionCycleId":91,"budgetLimitUsd":10.00,
                 "providerReservedCredits":648,"providerReservedCostUsd":6.48,
                 "runwayRouterConfigId":"product_ugc@2026-06",
                 "runwayRouterRequestsJson":"[{\\"version\\":\\"2026-06\\",\\"duration\\":15,\\"ratio\\":\\"1080:1920\\"}]",
                 "generation_strategy":"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION",
                 "targetDurationSeconds":15,"sceneCount":1,"assemblyRequired":false,
                 "technicalQualityGate":{"continuousTakeRequired":false,
                   "intentionalSceneCutsAllowed":true,"maximumSceneCuts":4,
                   "captionMustMatchNarration":true,"forbidMirrorOrReflection":true,
                   "maximumMeanMotionDelta":1.25,"maximumPeakMotionDelta":12.0},
                 "referenceGovernance":{"productIsDigitalExperience":true,
                   "presenterConsentEvidence":"consent-91","referenceRightsEvidence":"rights-91"},
                 "premiumFinalization":{"enabled":true,
                   "captionText":"Você se arruma | Faça o diagnóstico gratuito",
                   "voiceOverScript":"Você se arruma Faça o diagnóstico gratuito",
                   "requiredReviewers":["Psique","Temis","HUMAN"]},
                 "researchIntelligence":{"contractVersion":"HARNESS_RESEARCH_INTELLIGENCE_V1","routes":[
                   {"agentKey":"videomaker","cards":[
                     {"cardId":"RI1-AAAAAAAAAAAA","collection":"video"}%s
                   ]}
                 ]}}
                """.formatted(secondCard));
    }

    /** Cria um job Product UGC autônomo sem depender do backend durante o teste. */
    private SalesVideoJob productUgcJob(String metadata) {
        Instant now = Instant.parse("2026-09-04T12:00:00Z");
        return new SalesVideoJob(
                91L,
                57L,
                14L,
                "tenant-a",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "RUNWAY_PRODUCT_UGC",
                null,
                SalesVideoJobType.RENDER,
                SalesVideoStatus.VIDEO_REQUESTED,
                0,
                null,
                null,
                null,
                0,
                null,
                null,
                "Apolo",
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                metadata,
                now,
                now);
    }

    /** Cria um job da rota editorial governada sem depender de integração externa. */
    private SalesVideoJob editorialJob(String metadata) {
        Instant now = Instant.parse("2026-09-26T12:00:00Z");
        return new SalesVideoJob(
                93L,
                57L,
                14L,
                "default",
                SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE,
                "EDITORIAL_MOTION",
                null,
                SalesVideoJobType.RENDER,
                SalesVideoStatus.VIDEO_REQUESTED,
                0,
                null,
                null,
                null,
                0,
                null,
                null,
                "time@marketinghub.io",
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                metadata,
                now,
                now);
    }

    /** Registra no plano os IDs que a resposta estruturada declara ter aplicado. */
    private void appliedCards(JsonNode plan, String... cardIds) {
        ArrayNode values = ((com.fasterxml.jackson.databind.node.ObjectNode) plan)
                .put("researchApplicationRationale", "Os cartões orientaram ritmo e recompensa sensorial.")
                .putArray("appliedCardIds");
        for (String cardId : cardIds) values.add(cardId);
    }

    /** Monta uma resposta estruturada da IA para validar sem chamada paga. */
    private JsonNode plan(boolean embeddedText) throws Exception {
        String firstObjective = embeddedText
                ? "Mostrar texto PROMESSA dentro do vídeo gerado"
                : "Mostrar pessoa reconhecendo uma dificuldade cotidiana";
        return objectMapper.readTree("""
                {"creativeRationale":"Progressão comercial concreta e sem repetição","researchApplicationRationale":"Legado sem seleção entregue.","appliedCardIds":[],"cuts":[
                 {"order":1,"durationSeconds":6,"commercialRole":"HOOK_DOR","narrativePhase":"HOOK","visualObjective":"%s","continuityAnchor":"Mesma protagonista no espelho","reuseExistingMaterial":false,"postProductionText":"Dor"},
                 {"order":2,"durationSeconds":6,"commercialRole":"MECANISMO","narrativePhase":"DISCOVERY","visualObjective":"Demonstrar uma ação simples do método","continuityAnchor":"Mesma protagonista e figurino","reuseExistingMaterial":false,"postProductionText":"Mecanismo"},
                 {"order":3,"durationSeconds":6,"commercialRole":"RESULTADO","narrativePhase":"TRANSFORMATION","visualObjective":"Mostrar mudança prática em ambiente iluminado","continuityAnchor":"Mesmo figurino após o ajuste","reuseExistingMaterial":false,"postProductionText":"Resultado"},
                 {"order":4,"durationSeconds":6,"commercialRole":"PROVA","narrativePhase":"PROOF","visualObjective":"Mostrar entregável físico como prova concreta","continuityAnchor":"Mesmo ambiente e luz natural","reuseExistingMaterial":false,"postProductionText":"Prova"},
                 {"order":5,"durationSeconds":6,"commercialRole":"CTA","narrativePhase":"CTA","visualObjective":"Encerrar com gesto de decisão e espaço limpo","continuityAnchor":"Mesma protagonista conclui a história","reuseExistingMaterial":false,"postProductionText":"Começar agora"}]}
                """.formatted(firstObjective));
    }
}
