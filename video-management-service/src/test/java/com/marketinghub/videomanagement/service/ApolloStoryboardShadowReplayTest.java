package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.ApolloPlanningAiClient;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: homologar o replay sombra de Apolo sem IA externa, provider ou gasto. */
class ApolloStoryboardShadowReplayTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ApolloStoryboardShadowReplay replay;

    /** Monta o ambiente com cliente de IA inerte para impedir chamadas externas durante o replay. */
    @BeforeEach
    void setUp() {
        ApolloStoryboardPlanner planner = new ApolloStoryboardPlanner(new VideoManagementProperties(), objectMapper,
                mock(ApolloPlanningAiClient.class));
        replay = new ApolloStoryboardShadowReplay(planner);
    }

    /** Promove apenas para sombra online a candidata melhor e dentro do mesmo orçamento congelado. */
    @Test
    void shouldCompareHistoricalAndCandidatePlansWithoutProviderOrSpending() throws Exception {
        ApolloStoryboardShadowReplay.ReplayComparison comparison = replay.compare(metadata(), currentPlan(),
                candidatePlan(), "RUNWAY_SEEDANCE_2_5");

        assertThat(comparison.shadowMode()).isTrue();
        assertThat(comparison.providerCalled()).isFalse();
        assertThat(comparison.spendingAuthorized()).isFalse();
        assertThat(comparison.current().qualityScore()).isLessThan(70);
        assertThat(comparison.candidate().qualityScore()).isGreaterThanOrEqualTo(70);
        assertThat(comparison.candidate().expectedCredits()).isLessThanOrEqualTo(comparison.current().expectedCredits());
        assertThat(comparison.decision()).isEqualTo("CANDIDATE_ELIGIBLE_FOR_ONLINE_SHADOW");
    }

    /** Bloqueia localmente uma candidata que exceda o teto financeiro congelado. */
    @Test
    void shouldBlockCandidateAboveHistoricalBudget() throws Exception {
        JsonNode metadata = metadata().deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) metadata).put("budgetLimitUsd", 8.99);

        ApolloStoryboardShadowReplay.ReplayComparison comparison = replay.compare(metadata, currentPlan(),
                candidatePlan(), "RUNWAY_SEEDANCE_2_5");

        assertThat(comparison.candidate().gateApproved()).isFalse();
        assertThat(comparison.decision()).isEqualTo("CANDIDATE_BLOCKED_WITHOUT_PROVIDER_CALL");
        assertThat(comparison.providerCalled()).isFalse();
    }

    /** Representa a execução histórica congelada com teto e consumo conhecidos. */
    private JsonNode metadata() throws Exception {
        return objectMapper.readTree("""
                {"budgetLimitUsd":10.00,"targetDurationSeconds":30,"sceneCount":2,
                 "providerClipDurationSeconds":15,"expectedCredits":900,"expectedCostUsd":9.00,
                 "cut_plan":[{},{},{},{}]}
                """);
    }

    /** Reproduz o padrão histórico de repetição e texto embutido sem chamar o modelo. */
    private JsonNode currentPlan() throws Exception {
        return objectMapper.readTree("""
                {"creativeRationale":"Cenas históricas repetitivas","cuts":[
                 {"order":1,"durationSeconds":8,"commercialRole":"HOOK_DOR","visualObjective":"Mulher no escritório com texto diagnóstico","reuseExistingMaterial":false,"postProductionText":""},
                 {"order":2,"durationSeconds":8,"commercialRole":"HOOK_DOR","visualObjective":"Mulher no escritório com texto diagnóstico","reuseExistingMaterial":false,"postProductionText":""},
                 {"order":3,"durationSeconds":7,"commercialRole":"RESULTADO","visualObjective":"Mulher no escritório com texto diagnóstico","reuseExistingMaterial":false,"postProductionText":""},
                 {"order":4,"durationSeconds":7,"commercialRole":"CTA","visualObjective":"Mulher no escritório com texto diagnóstico","reuseExistingMaterial":false,"postProductionText":""}]}
                """);
    }

    /** Representa uma candidata comercialmente completa e com reaproveitamento do material existente. */
    private JsonNode candidatePlan() throws Exception {
        return objectMapper.readTree("""
                {"creativeRationale":"Progressão comercial distinta com material preservado","cuts":[
                 {"order":1,"durationSeconds":8,"commercialRole":"HOOK_DOR","visualObjective":"Cliente percebe lacunas vazias na agenda durante a manhã","reuseExistingMaterial":true,"postProductionText":"Agenda vazia custa caro"},
                 {"order":2,"durationSeconds":8,"commercialRole":"RESULTADO","visualObjective":"Profissional atende cliente em bancada organizada e iluminada","reuseExistingMaterial":true,"postProductionText":"Previsibilidade de clientes"},
                 {"order":3,"durationSeconds":7,"commercialRole":"MECANISMO","visualObjective":"Mãos organizam cartões físicos em sequência simples de acompanhamento","reuseExistingMaterial":false,"postProductionText":"Método em etapas"},
                 {"order":4,"durationSeconds":7,"commercialRole":"CTA","visualObjective":"Profissional encerra o dia satisfeita com espaço visual limpo ao lado","reuseExistingMaterial":false,"postProductionText":"Comece agora"}]}
                """);
    }
}
