package com.marketinghub.nichocnaev2.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

/** Valida a classificação operacional de falhas feita no executor NichoCNAE v2. */
class NichoCnaeV2PendingExecutionServiceTest {
    /** Deve bloquear loop quando contrato de entrada inválido chega à etapa de segurança. */
    @Test
    void classifiesInvalidStageInputAsValidationFailureWithoutTechnicalRetry() {
        NichoCnaeV2BackendClient backendClient = mock(NichoCnaeV2BackendClient.class);
        NichoCnaeV2StageDefinitions stageDefinitions = new NichoCnaeV2StageDefinitions();
        NichoCnaeV2PendingExecutionService service = new NichoCnaeV2PendingExecutionService(backendClient, stageDefinitions);
        NichoCnaeV2PendingExecution pending = new NichoCnaeV2PendingExecution(
                "54",
                "job-1",
                "4781400",
                "Comércio varejista de artigos do vestuário e acessórios",
                1L,
                2L,
                1,
                26,
                1,
                false,
                "{\"stage\":\"candidate-generator\"}",
                Map.of());
        when(backendClient.listPending(any())).thenAnswer(invocation -> {
            NichoCnaeV2StageDefinition stage = invocation.getArgument(0);
            return "source-safety-filter".equals(stage.stageCode()) ? List.of(pending) : List.of();
        });
        when(backendClient.parseInput(pending.inputPayload())).thenReturn(Map.of("stage", "candidate-generator"));

        service.processAllPending();

        verify(backendClient).fail(
                any(NichoCnaeV2StageDefinition.class),
                eq(pending),
                any(IllegalArgumentException.class),
                eq("VALIDATION"),
                eq("INVALID_STAGE_INPUT_CONTRACT"));
    }

    /** Deve executar a etapa 2 mesmo quando metadados opcionais do pending chegarem nulos. */
    @Test
    void processesSourceSafetyFilterWhenOptionalPendingMetadataIsNull() {
        NichoCnaeV2BackendClient backendClient = mock(NichoCnaeV2BackendClient.class);
        NichoCnaeV2StageDefinitions stageDefinitions = new NichoCnaeV2StageDefinitions();
        NichoCnaeV2PendingExecutionService service = new NichoCnaeV2PendingExecutionService(backendClient, stageDefinitions);
        NichoCnaeV2PendingExecution pending = new NichoCnaeV2PendingExecution(
                "98",
                "nichocnae-v2-candidate-2-job-2",
                "4781400",
                null,
                null,
                2L,
                1,
                0,
                1,
                false,
                "{\"candidateUrls\":[\"https://www.gov.br/empresas-e-negocios/pt-br/empreendedor\"]}",
                Map.of());
        when(backendClient.listPending(any())).thenAnswer(invocation -> {
            NichoCnaeV2StageDefinition stage = invocation.getArgument(0);
            return "source-safety-filter".equals(stage.stageCode()) ? List.of(pending) : List.of();
        });
        when(backendClient.parseInput(pending.inputPayload()))
                .thenReturn(Map.of("candidateUrls", List.of("https://www.gov.br/empresas-e-negocios/pt-br/empreendedor")));
        when(backendClient.toJson(any())).thenReturn("{\"stage\":\"source-safety-filter\"}");

        service.processAllPending();

        verify(backendClient).complete(any(NichoCnaeV2StageDefinition.class), eq(pending), any());
        verify(backendClient).createNextStage(any(NichoCnaeV2StageDefinition.class), eq(pending), any(), eq(null), eq(null));
    }

    /** Deve preservar candidatos e trocar apenas o próximo estágio ao criar a pendência seguinte. */
    @Test
    void preservesAccumulatedContextWhenCreatingNextStage() {
        NichoCnaeV2BackendClient backendClient = mock(NichoCnaeV2BackendClient.class);
        NichoCnaeV2StageDefinitions stageDefinitions = new NichoCnaeV2StageDefinitions();
        NichoCnaeV2PendingExecutionService service = new NichoCnaeV2PendingExecutionService(backendClient, stageDefinitions);
        NichoCnaeV2PendingExecution pending = new NichoCnaeV2PendingExecution(
                "133",
                "nichocnae-v2-candidate-2-job-7",
                "4781400",
                "Comércio varejista de artigos do vestuário e acessórios",
                null,
                2L,
                1,
                0,
                1,
                false,
                "{\"nextStageCode\":\"source-safety-filter\"}",
                Map.of());
        Map<String, Object> input = Map.of(
                "nextStageCode", "source-safety-filter",
                "candidateUrls", List.of("https://www.gov.br/empresas-e-negocios/pt-br/empreendedor"),
                "candidateCount", 1,
                "candidates", List.of(Map.of("candidateId", "C1", "job", "VESTUARIO_ATENDIMENTO_LOJA")));
        when(backendClient.listPending(any())).thenAnswer(invocation -> {
            NichoCnaeV2StageDefinition stage = invocation.getArgument(0);
            return "source-safety-filter".equals(stage.stageCode()) ? List.of(pending) : List.of();
        });
        when(backendClient.parseInput(pending.inputPayload())).thenReturn(input);
        when(backendClient.toJson(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());

        service.processAllPending();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(backendClient).createNextStage(
                any(NichoCnaeV2StageDefinition.class), eq(pending), payloadCaptor.capture(), eq(null), eq(null));
        assertThat(payloadCaptor.getValue())
                .contains("candidates")
                .contains("candidateCount=1")
                .contains("nextStageCode=adaptive-query-planner")
                .doesNotContain("nextStageCode=source-safety-filter");
    }

    /** Deve propagar tentativa e versão calculadas pelo controlador de reprocessamento para a próxima pendência. */
    @Test
    void propagatesReprocessAttemptAndKnowledgeVersionToNextStage() {
        NichoCnaeV2BackendClient backendClient = mock(NichoCnaeV2BackendClient.class);
        NichoCnaeV2StageDefinitions stageDefinitions = new NichoCnaeV2StageDefinitions();
        NichoCnaeV2PendingExecutionService service = new NichoCnaeV2PendingExecutionService(backendClient, stageDefinitions);
        NichoCnaeV2PendingExecution pending = new NichoCnaeV2PendingExecution(
                "109",
                "nichocnae-v2-candidate-2-job-4",
                "4781400",
                "Comércio varejista de artigos do vestuário e acessórios",
                4L,
                2L,
                1,
                0,
                1,
                false,
                "{\"tournamentDecision\":\"NO_VIABLE_SUBNICHE\",\"informationGain\":0.2}",
                Map.of());
        when(backendClient.listPending(any())).thenAnswer(invocation -> {
            NichoCnaeV2StageDefinition stage = invocation.getArgument(0);
            return "reprocess-controller".equals(stage.stageCode()) ? List.of(pending) : List.of();
        });
        when(backendClient.parseInput(pending.inputPayload()))
                .thenReturn(Map.of("tournamentDecision", "NO_VIABLE_SUBNICHE", "informationGain", 0.2));
        when(backendClient.toJson(any())).thenReturn("{\"stage\":\"reprocess-controller\"}");

        service.processAllPending();

        verify(backendClient).complete(any(NichoCnaeV2StageDefinition.class), eq(pending), any());
        verify(backendClient).createNextStage(
                any(NichoCnaeV2StageDefinition.class), eq(pending), any(), eq(2), eq(2));
    }

    /** Deve encerrar como falha de evidência quando o job repete pesquisa sem ganho novo. */
    @Test
    void failsControlledResearchLoopBeforeCreatingAnotherPendingStage() {
        NichoCnaeV2BackendClient backendClient = mock(NichoCnaeV2BackendClient.class);
        NichoCnaeV2StageDefinitions stageDefinitions = new NichoCnaeV2StageDefinitions();
        NichoCnaeV2PendingExecutionService service = new NichoCnaeV2PendingExecutionService(backendClient, stageDefinitions);
        NichoCnaeV2PendingExecution pending = new NichoCnaeV2PendingExecution(
                "211",
                "nichocnae-v2-candidate-3-job-3",
                "7319002",
                "Outras atividades profissionais, científicas e técnicas",
                9L,
                3L,
                1,
                0,
                1,
                false,
                "{\"sourceCandidates\":[],\"stageVisitCounts\":{\"source-fetcher-reranker\":2},\"noInformationGainStreak\":2}",
                Map.of());
        when(backendClient.listPending(any())).thenAnswer(invocation -> {
            NichoCnaeV2StageDefinition stage = invocation.getArgument(0);
            return "source-fetcher-reranker".equals(stage.stageCode()) ? List.of(pending) : List.of();
        });
        when(backendClient.parseInput(pending.inputPayload()))
                .thenReturn(Map.of(
                        "sourceCandidates",
                        List.of(),
                        "stageVisitCounts",
                        Map.of("source-fetcher-reranker", 2),
                        "noInformationGainStreak",
                        2));

        service.processAllPending();

        verify(backendClient).fail(
                any(NichoCnaeV2StageDefinition.class),
                eq(pending),
                any(RuntimeException.class),
                eq("MARKET_EVIDENCE"),
                eq("RESEARCH_LOOP_WITHOUT_INFORMATION_GAIN"));
        verify(backendClient, never()).complete(any(NichoCnaeV2StageDefinition.class), eq(pending), any());
        verify(backendClient, never())
                .createNextStage(any(NichoCnaeV2StageDefinition.class), eq(pending), any(), any(), any());
    }
}
