package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.ApolloCodexShadowClient;
import com.marketinghub.videomanagement.client.ApolloPlanningAiClient;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a disputa segura entre o plano API persistido e a candidata Codex. */
class ApolloHybridShadowReplayTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Aprova a candidata Codex superior sem chamada de vídeo, gasto ou publicação. */
    @Test
    void shouldComparePersistedApiPlanWithCodexCandidateWithoutExternalEffect() throws Exception {
        JsonNode metadata = metadata();
        JsonNode baseline = baseline();
        JsonNode candidate = candidate();
        ApolloCodexShadowClient codex = mock(ApolloCodexShadowClient.class);
        when(codex.plan(21105L, metadata, baseline)).thenReturn(
                new ApolloCodexShadowClient.CodexShadowResult(candidate, "snapshot", candidate.toString(),
                        "gpt-5.6-sol", true, false, false));
        ApolloStoryboardPlanner planner = new ApolloStoryboardPlanner(new VideoManagementProperties(), objectMapper,
                mock(ApolloPlanningAiClient.class));
        ApolloHybridShadowReplay hybrid = new ApolloHybridShadowReplay(codex,
                new ApolloStoryboardShadowReplay(planner));

        ApolloHybridShadowReplay.HybridComparison result =
                hybrid.compare(21105L, metadata, baseline, "RUNWAY_SEEDANCE_2_5");

        assertThat(result.baselineOrigin()).isEqualTo("OPENAI_API_PERSISTED");
        assertThat(result.candidateOrigin()).isEqualTo("CODEX_SESSION");
        assertThat(result.providerCalled()).isFalse();
        assertThat(result.spendingAuthorized()).isFalse();
        assertThat(result.comparison().decision()).isEqualTo("CANDIDATE_ELIGIBLE_FOR_ONLINE_SHADOW");
    }

    /** Mantém congelado o teto financeiro e a estrutura do vídeo histórico. */
    private JsonNode metadata() throws Exception {
        return objectMapper.readTree("""
                {"budgetLimitUsd":10.00,"targetDurationSeconds":30,"sceneCount":2,
                 "providerClipDurationSeconds":15,"expectedCredits":900,"expectedCostUsd":9.00,
                 "cut_plan":[{},{},{},{},{}]}
                """);
    }

    /** Representa o plano API histórico insuficiente usado apenas como linha de base. */
    private JsonNode baseline() throws Exception {
        return objectMapper.readTree("""
                {"creativeRationale":"Repetição histórica","cuts":[
                 {"order":1,"durationSeconds":6,"commercialRole":"HOOK_DOR","narrativePhase":"HOOK","visualObjective":"Mulher no escritório com texto diagnóstico","continuityAnchor":"Mesma mulher","reuseExistingMaterial":false,"postProductionText":""},
                 {"order":2,"durationSeconds":6,"commercialRole":"HOOK_DOR","narrativePhase":"SETUP","visualObjective":"Mulher no escritório com texto diagnóstico","continuityAnchor":"Mesma mulher","reuseExistingMaterial":false,"postProductionText":""},
                 {"order":3,"durationSeconds":6,"commercialRole":"RESULTADO","narrativePhase":"TRANSFORMATION","visualObjective":"Mulher no escritório com texto diagnóstico","continuityAnchor":"Mesma mulher","reuseExistingMaterial":false,"postProductionText":""},
                 {"order":4,"durationSeconds":6,"commercialRole":"PROVA","narrativePhase":"PROOF","visualObjective":"Mulher no escritório com texto diagnóstico","continuityAnchor":"Mesma mulher","reuseExistingMaterial":false,"postProductionText":""},
                 {"order":5,"durationSeconds":6,"commercialRole":"CTA","narrativePhase":"CTA","visualObjective":"Mulher no escritório com texto diagnóstico","continuityAnchor":"Mesma mulher","reuseExistingMaterial":false,"postProductionText":""}]}
                """);
    }

    /** Representa uma candidata com arco, diversidade e pós-produção controlada. */
    private JsonNode candidate() throws Exception {
        return objectMapper.readTree("""
                {"creativeRationale":"História comercial completa","cuts":[
                 {"order":1,"durationSeconds":6,"commercialRole":"HOOK_DOR","narrativePhase":"HOOK","visualObjective":"Profissional observa horários vazios na agenda física","continuityAnchor":"Mesmo salão e profissional","reuseExistingMaterial":true,"postProductionText":"Agenda vazia custa caro"},
                 {"order":2,"durationSeconds":6,"commercialRole":"MECANISMO","narrativePhase":"DISCOVERY","visualObjective":"Mãos organizam cartões de acompanhamento por prioridade","continuityAnchor":"Mesma bancada pela manhã","reuseExistingMaterial":false,"postProductionText":"Método simples"},
                 {"order":3,"durationSeconds":6,"commercialRole":"RESULTADO","narrativePhase":"TRANSFORMATION","visualObjective":"Profissional recebe uma cliente na bancada preparada","continuityAnchor":"Mesmo figurino e iluminação","reuseExistingMaterial":true,"postProductionText":"Mais previsibilidade"},
                 {"order":4,"durationSeconds":6,"commercialRole":"PROVA","narrativePhase":"PROOF","visualObjective":"Agenda aberta revela horários ocupados escritos à mão","continuityAnchor":"Mesma bancada ao fim do dia","reuseExistingMaterial":false,"postProductionText":"Rotina comprovável"},
                 {"order":5,"durationSeconds":6,"commercialRole":"CTA","narrativePhase":"CTA","visualObjective":"Profissional encerra o salão satisfeita com espaço lateral limpo","continuityAnchor":"Mesma profissional conclui a história","reuseExistingMaterial":false,"postProductionText":"Comece agora"}]}
                """);
    }
}
