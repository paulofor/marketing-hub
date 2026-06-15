package com.marketinghub.hypothesis.pain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.hypothesis.pain.HypothesisPainCostCalculator;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.hypothesis.pain.service.recebeResposta.RecebeRespostaRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a orquestração das etapas do pipeline de hipótese por nicho. */
@ExtendWith(MockitoExtension.class)
class HypothesisPainStageServiceTest {

    @Mock
    private MarketNicheRepository marketNicheRepository;

    @Mock
    private HypothesisPainStageExecutionRepository executionRepository;

    @Mock
    private HypothesisPainCostCalculator costCalculator;

    private HypothesisPainStageService service;

    /** Prepara o serviço com dependências isoladas para cada teste. */
    @BeforeEach
    void setup() {
        service = new HypothesisPainStageService(
                marketNicheRepository,
                executionRepository,
                costCalculator);
    }

    /** Deve atribuir ao nicho somente o delta de custo calculado para evitar soma duplicada em reprocessamentos. */
    @Test
    void markCompletedFromResponseAttributesOnlyCostDeltaToNiche() {
        String idJob = "9bb83a22-3894-43bd-9752-374f84eb6a2c";
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        HypothesisPainStageExecution execution = HypothesisPainStageExecution.builder()
                .idJob(idJob.getBytes(StandardCharsets.UTF_8))
                .marketNicheId(18L)
                .marketNiche(niche)
                .stageCode("hypothesis-pain")
                .status("PROCESSANDO")
                .costUsd(new BigDecimal("0.01000000"))
                .openAiModel("gpt-5.2")
                .build();
        RecebeRespostaRequest request = new RecebeRespostaRequest(
                18L,
                "hypothesis-pain",
                "{\"pain\":\"dor validada\"}",
                "{\"id\":\"resp_1\"}",
                1200,
                300,
                new BigDecimal("999.00000000"),
                "openai-job-1",
                null,
                null);

        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(idJob.getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(any(HypothesisPainStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(costCalculator.calculateFlexCostUsd("gpt-5.2", 1200, 300))
                .thenReturn(new BigDecimal("0.01500000"));

        service.markCompletedFromResponse(idJob, request);

        verify(costCalculator).calculateFlexCostUsd("gpt-5.2", 1200, 300);
        verify(costCalculator).addFlexCostDeltaToNiche(niche, new BigDecimal("0.00500000"));
    }

    /** Deve recuperar job antigo em PROCESSANDO sem openai_job_id para impedir travamento operacional da fila. */
    @Test
    void listPendingRecoversOldProcessingJobWithoutOpenAiJobId() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        String idJob = "7bb83a22-3894-43bd-9752-374f84eb6a2c";
        HypothesisPainStageExecution expiredExecution = HypothesisPainStageExecution.builder()
                .idJob(idJob.getBytes(StandardCharsets.UTF_8))
                .marketNicheId(18L)
                .marketNiche(niche)
                .stageCode("hypothesis-pain")
                .status("PROCESSANDO")
                .processingStartedAt(Instant.parse("2026-06-11T10:00:00Z"))
                .executionRequestedAt(Instant.parse("2026-06-11T09:59:00Z"))
                .build();

        when(executionRepository.findTop50ByStageCodeAndStatusInAndCompletedAtIsNullAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
                        any(),
                        any(),
                        any()))
                .thenReturn(List.of(expiredExecution));
        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                        "hypothesis-pain",
                        "INICIADO"))
                .thenReturn(List.of(expiredExecution));

        var pending = service.listPending();

        assertEquals(1, pending.size());
        assertEquals(idJob, pending.getFirst().jobid());
        assertEquals("INICIADO", pending.getFirst().status());
        assertNull(pending.getFirst().processingStartedAt());
        ArgumentCaptor<List<HypothesisPainStageExecution>> captor = ArgumentCaptor.forClass(List.class);
        verify(executionRepository).saveAll(captor.capture());
        HypothesisPainStageExecution recovered = captor.getValue().getFirst();
        assertEquals("INICIADO", recovered.getStatus());
        assertNull(recovered.getProcessingStartedAt());
        assertEquals(
                "Timeout operacional: execução ficou em PROCESSANDO por mais de 45 minutos sem conclusão. Job recuperado automaticamente e devolvido para a fila de processamento.",
                recovered.getErrorMessage());
    }

    /** Deve falhar job antigo com openai_job_id para impedir recaptura duplicada de execução ativa na OpenAI. */
    @Test
    void listPendingFailsOldWaitingJobWithOpenAiJobId() {
        String idJob = "8bb83a22-3894-43bd-9752-374f84eb6a2c";
        HypothesisPainStageExecution expiredExecution = HypothesisPainStageExecution.builder()
                .idJob(idJob.getBytes(StandardCharsets.UTF_8))
                .marketNicheId(18L)
                .stageCode("hypothesis-pain")
                .status("AGUARDANDO_RETORNO_OPENAI")
                .openAiJobId("openai-active-1")
                .processingStartedAt(Instant.parse("2026-06-11T10:00:00Z"))
                .executionRequestedAt(Instant.parse("2026-06-11T09:59:00Z"))
                .build();

        when(executionRepository.findTop50ByStageCodeAndStatusInAndCompletedAtIsNullAndProcessingStartedAtBeforeOrderByProcessingStartedAtAsc(
                        any(),
                        any(),
                        any()))
                .thenReturn(List.of(expiredExecution));

        var pending = service.listPending();

        assertEquals(0, pending.size());
        ArgumentCaptor<List<HypothesisPainStageExecution>> captor = ArgumentCaptor.forClass(List.class);
        verify(executionRepository).saveAll(captor.capture());
        HypothesisPainStageExecution failed = captor.getValue().getFirst();
        assertEquals("FALHA", failed.getStatus());
        assertEquals(
                "Timeout operacional: execução ficou em AGUARDANDO_RETORNO_OPENAI por mais de 45 minutos sem conclusão. Job marcado como FALHA para evitar recaptura enquanto há possível execução ativa na OpenAI.",
                failed.getErrorMessage());
    }

    /** Deve bloquear a etapa Resultado quando a dor ainda não está concluída. */
    @Test
    void startResultRequiresCompletedPain() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        when(marketNicheRepository.findById(18L)).thenReturn(Optional.of(niche));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.startResult(18L));
    }

    /** Deve entregar a resposta da Dor concluída para contextualizar a etapa Resultado no Worker AI. */
    @Test
    void listResultPendingIncludesLatestCompletedPainResponse() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        niche.setName("Produtores digitais");
        String resultJob = "3bc56a94-45bc-48bf-8e8d-e1f4f8b881df";
        HypothesisPainStageExecution resultExecution = HypothesisPainStageExecution.builder()
                .idJob(resultJob.getBytes(StandardCharsets.UTF_8))
                .marketNicheId(18L)
                .marketNiche(niche)
                .stageCode("hypothesis-result")
                .status("INICIADO")
                .executionRequestedAt(Instant.parse("2026-06-11T12:00:00Z"))
                .build();
        HypothesisPainStageExecution completedPain = HypothesisPainStageExecution.builder()
                .marketNicheId(18L)
                .stageCode("hypothesis-pain")
                .status("CONCLUIDO")
                .modelResponse("{\"pain\":\"dor validada\"}")
                .build();

        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                        "hypothesis-result",
                        "INICIADO"))
                .thenReturn(List.of(resultExecution));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(completedPain));

        var pending = service.listResultPending();

        assertEquals(1, pending.size());
        assertEquals("{\"pain\":\"dor validada\"}", pending.getFirst().painModelResponse());
    }

    /** Deve bloquear a etapa Mecanismo quando o resultado ainda não está concluído. */
    @Test
    void startMechanismRequiresCompletedResult() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        when(marketNicheRepository.findById(18L)).thenReturn(Optional.of(niche));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-pain")
                        .status("CONCLUIDO")
                        .modelResponse("{\"pain\":\"dor validada\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-result",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.startMechanism(18L));
    }

    /** Deve entregar Dor e Resultado concluídos para contextualizar a etapa Mecanismo no Worker AI. */
    @Test
    void listMechanismPendingIncludesLatestCompletedPainAndResultResponses() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        niche.setName("Produtores digitais");
        String mechanismJob = "4cc56a94-45bc-48bf-8e8d-e1f4f8b881df";
        HypothesisPainStageExecution mechanismExecution = HypothesisPainStageExecution.builder()
                .idJob(mechanismJob.getBytes(StandardCharsets.UTF_8))
                .marketNicheId(18L)
                .marketNiche(niche)
                .stageCode("hypothesis-mechanism")
                .status("INICIADO")
                .executionRequestedAt(Instant.parse("2026-06-11T13:00:00Z"))
                .build();
        HypothesisPainStageExecution completedPain = HypothesisPainStageExecution.builder()
                .marketNicheId(18L)
                .stageCode("hypothesis-pain")
                .status("CONCLUIDO")
                .modelResponse("{\"pain\":\"dor validada\"}")
                .build();
        HypothesisPainStageExecution completedResult = HypothesisPainStageExecution.builder()
                .marketNicheId(18L)
                .stageCode("hypothesis-result")
                .status("CONCLUIDO")
                .modelResponse("{\"result\":\"resultado validado\"}")
                .build();

        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                        "hypothesis-mechanism",
                        "INICIADO"))
                .thenReturn(List.of(mechanismExecution));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(completedPain));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-result",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(completedResult));

        var pending = service.listMechanismPending();

        assertEquals(1, pending.size());
        assertEquals("{\"pain\":\"dor validada\"}", pending.getFirst().painModelResponse());
        assertEquals("{\"result\":\"resultado validado\"}", pending.getFirst().resultModelResponse());
    }

    /** Deve bloquear a etapa Oferta quando o mecanismo ainda não está concluído. */
    @Test
    void startOfferRequiresCompletedMechanism() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        when(marketNicheRepository.findById(18L)).thenReturn(Optional.of(niche));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-pain")
                        .status("CONCLUIDO")
                        .modelResponse("{\"pain\":\"dor validada\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-result",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-result")
                        .status("CONCLUIDO")
                        .modelResponse("{\"result\":\"resultado validado\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-mechanism",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.startOffer(18L));
    }


    /** Deve bloquear a etapa Oferta quando a prova ainda não está concluída. */
    @Test
    void startOfferRequiresCompletedProof() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        when(marketNicheRepository.findById(18L)).thenReturn(Optional.of(niche));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-pain")
                        .status("CONCLUIDO")
                        .modelResponse("{\"pain\":\"dor validada\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-result",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-result")
                        .status("CONCLUIDO")
                        .modelResponse("{\"result\":\"resultado validado\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-mechanism",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-mechanism")
                        .status("CONCLUIDO")
                        .modelResponse("{\"mechanism\":\"mecanismo validado\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-proof",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.startOffer(18L));
    }

    /** Deve entregar Dor, Resultado, Mecanismo e Prova concluídos para contextualizar a etapa Oferta no Worker AI. */
    @Test
    void listOfferPendingIncludesLatestCompletedPainResultMechanismAndProofResponses() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        niche.setName("Produtores digitais");
        String offerJob = "5cc56a94-45bc-48bf-8e8d-e1f4f8b881df";
        HypothesisPainStageExecution offerExecution = HypothesisPainStageExecution.builder()
                .idJob(offerJob.getBytes(StandardCharsets.UTF_8))
                .marketNicheId(18L)
                .marketNiche(niche)
                .stageCode("hypothesis-offer")
                .status("INICIADO")
                .executionRequestedAt(Instant.parse("2026-06-11T14:00:00Z"))
                .build();
        HypothesisPainStageExecution completedPain = HypothesisPainStageExecution.builder()
                .marketNicheId(18L)
                .stageCode("hypothesis-pain")
                .status("CONCLUIDO")
                .modelResponse("{\"pain\":\"dor validada\"}")
                .build();
        HypothesisPainStageExecution completedResult = HypothesisPainStageExecution.builder()
                .marketNicheId(18L)
                .stageCode("hypothesis-result")
                .status("CONCLUIDO")
                .modelResponse("{\"result\":\"resultado validado\"}")
                .build();
        HypothesisPainStageExecution completedMechanism = HypothesisPainStageExecution.builder()
                .marketNicheId(18L)
                .stageCode("hypothesis-mechanism")
                .status("CONCLUIDO")
                .modelResponse("{\"mechanism\":\"mecanismo validado\"}")
                .build();
        HypothesisPainStageExecution completedProof = HypothesisPainStageExecution.builder()
                .marketNicheId(18L)
                .stageCode("hypothesis-proof")
                .status("CONCLUIDO")
                .modelResponse("{\"proof\":\"prova validada\"}")
                .build();

        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                        "hypothesis-offer",
                        "INICIADO"))
                .thenReturn(List.of(offerExecution));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(completedPain));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-result",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(completedResult));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-mechanism",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(completedMechanism));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-proof",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(completedProof));

        var pending = service.listOfferPending();

        assertEquals(1, pending.size());
        assertEquals("{\"pain\":\"dor validada\"}", pending.getFirst().painModelResponse());
        assertEquals("{\"result\":\"resultado validado\"}", pending.getFirst().resultModelResponse());
        assertEquals("{\"mechanism\":\"mecanismo validado\"}", pending.getFirst().mechanismModelResponse());
        assertEquals("{\"proof\":\"prova validada\"}", pending.getFirst().proofModelResponse());
    }

    /** Deve impedir que Oferta seja entregue como pendente quando a Prova ainda não foi concluída. */
    @Test
    void listOfferPendingDoesNotReturnOfferWhenProofIsNotCompleted() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        String offerJob = "6cc56a94-45bc-48bf-8e8d-e1f4f8b881df";
        HypothesisPainStageExecution offerExecution = HypothesisPainStageExecution.builder()
                .idJob(offerJob.getBytes(StandardCharsets.UTF_8))
                .marketNicheId(18L)
                .marketNiche(niche)
                .stageCode("hypothesis-offer")
                .status("INICIADO")
                .executionRequestedAt(Instant.parse("2026-06-11T15:00:00Z"))
                .build();

        when(executionRepository.findTop20ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                        "hypothesis-offer",
                        "INICIADO"))
                .thenReturn(List.of(offerExecution));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-pain")
                        .status("CONCLUIDO")
                        .modelResponse("{\"pain\":\"dor validada\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-result",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-result")
                        .status("CONCLUIDO")
                        .modelResponse("{\"result\":\"resultado validado\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-mechanism",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode("hypothesis-mechanism")
                        .status("CONCLUIDO")
                        .modelResponse("{\"mechanism\":\"mecanismo validado\"}")
                        .build()));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-proof",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());

        var pending = service.listOfferPending();

        assertEquals(0, pending.size());
    }

    /** Deve listar somente o conteúdo final persistido e a origem no banco para cada etapa do framework. */
    @Test
    void listFinalSummaryReturnsFinalContentAndDatabaseSource() {
        String painJob = "9bb83a22-3894-43bd-9752-374f84eb6a2c";
        HypothesisPainStageExecution completedPain = HypothesisPainStageExecution.builder()
                .idJob(painJob.getBytes(StandardCharsets.UTF_8))
                .marketNicheId(18L)
                .stageCode("hypothesis-pain")
                .status("CONCLUIDO")
                .completedAt(Instant.parse("2026-06-11T01:36:19Z"))
                .modelResponse("{\"pain\":\"dor final\"}")
                .build();

        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.of(completedPain));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-result",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-mechanism",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-proof",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-offer",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());

        var summary = service.listFinalSummary(18L);

        assertEquals(5, summary.size());
        assertEquals("pain", summary.getFirst().slug());
        assertEquals(painJob, summary.getFirst().jobid());
        assertEquals("{\"pain\":\"dor final\"}", summary.getFirst().finalContent());
        assertEquals("hypothesis_pain_stage_execution", summary.getFirst().sourceTable());
        assertEquals("model_response", summary.getFirst().sourceField());
        assertEquals("result", summary.get(1).slug());
        assertEquals(null, summary.get(1).finalContent());
        assertEquals("hypothesis_pain_stage_execution", summary.get(1).sourceTable());
        assertEquals("model_response", summary.get(1).sourceField());
    }

}
