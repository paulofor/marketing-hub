package com.marketinghub.hypothesis.pain.service;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.hypothesis.pain.service.recebeResposta.RecebeRespostaRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a orquestração de custos da etapa Dor do pipeline de hipótese. */
@ExtendWith(MockitoExtension.class)
class HypothesisPainStageServiceTest {

    @Mock
    private MarketNicheRepository marketNicheRepository;

    @Mock
    private HypothesisPainStageExecutionRepository executionRepository;

    @Mock
    private CostAttributionService costAttributionService;

    private HypothesisPainStageService service;

    /** Prepara o serviço com dependências isoladas para cada teste. */
    @BeforeEach
    void setup() {
        service = new HypothesisPainStageService(
                marketNicheRepository,
                executionRepository,
                costAttributionService);
    }

    /** Deve atribuir ao nicho somente o delta de custo recebido para evitar soma duplicada em reprocessamentos. */
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
                .costUsd(new BigDecimal("0.010000"))
                .build();
        RecebeRespostaRequest request = new RecebeRespostaRequest(
                18L,
                "hypothesis-pain",
                "{\"pain\":\"dor validada\"}",
                1200,
                300,
                new BigDecimal("0.015000"),
                "openai-job-1",
                null,
                null);

        when(executionRepository.findTopByIdJobOrderByExecutionRequestedAtDesc(idJob.getBytes(StandardCharsets.UTF_8)))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(any(HypothesisPainStageExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.markCompletedFromResponse(idJob, request);

        verify(costAttributionService).addUsdCostToNiche(niche, new BigDecimal("0.005000"));
    }
    /** Deve bloquear a etapa Resultado quando a dor ainda não está concluída. */
    @Test
    void startResultRequiresCompletedPain() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        HypothesisPainStageExecution painExecution = HypothesisPainStageExecution.builder()
                .marketNicheId(18L)
                .marketNiche(niche)
                .stageCode("hypothesis-pain")
                .status("INICIADO")
                .build();

        when(marketNicheRepository.findById(18L)).thenReturn(Optional.of(niche));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeOrderByExecutionRequestedAtDesc(18L, "hypothesis-pain"))
                .thenReturn(Optional.of(painExecution));

        assertThrows(IllegalStateException.class, () -> service.startResult(18L));
    }

}
