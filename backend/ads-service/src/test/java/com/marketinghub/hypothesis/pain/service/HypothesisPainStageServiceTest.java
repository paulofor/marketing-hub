package com.marketinghub.hypothesis.pain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    /** Deve entregar Dor, Resultado e Mecanismo concluídos para contextualizar a etapa Oferta no Worker AI. */
    @Test
    void listOfferPendingIncludesLatestCompletedPainResultAndMechanismResponses() {
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

        var pending = service.listOfferPending();

        assertEquals(1, pending.size());
        assertEquals("{\"pain\":\"dor validada\"}", pending.getFirst().painModelResponse());
        assertEquals("{\"result\":\"resultado validado\"}", pending.getFirst().resultModelResponse());
        assertEquals("{\"mechanism\":\"mecanismo validado\"}", pending.getFirst().mechanismModelResponse());
    }

}
