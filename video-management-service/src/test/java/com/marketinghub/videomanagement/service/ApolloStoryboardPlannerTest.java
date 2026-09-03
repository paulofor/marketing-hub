package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.client.ApolloPlanningAiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

/** Responsabilidade: comprovar o gate determinístico anterior ao consumo audiovisual de Apolo. */
class ApolloStoryboardPlannerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ApolloStoryboardPlanner planner;

    /** Cria o planejador sem executar integrações externas. */
    @BeforeEach
    void setUp() {
        planner = new ApolloStoryboardPlanner(new VideoManagementProperties(), objectMapper,
                mock(ApolloPlanningAiClient.class));
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
