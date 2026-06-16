package com.marketinghub.hypothesis.service.finalizeHypothesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkMapperSupport;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a etapa de fechamento do pipeline de hipótese. */
@ExtendWith(MockitoExtension.class)
class HypothesisPipelineFinalizationServiceTest {
    @Mock
    private MarketNicheRepository marketNicheRepository;

    @Mock
    private HypothesisPainStageExecutionRepository executionRepository;

    @Mock
    private HypothesisRepository hypothesisRepository;

    private HypothesisPipelineFinalizationService service;

    /** Prepara a etapa de fechamento com dependências isoladas para cada teste. */
    @BeforeEach
    void setup() {
        service = new HypothesisPipelineFinalizationService(
                marketNicheRepository,
                executionRepository,
                hypothesisRepository,
                new HypothesisFrameworkMapperSupport(new ObjectMapper()));
    }

    /** Deve materializar o framework concluído como hipótese BACKLOG fora da etapa Dor. */
    @Test
    void finalizeHypothesisCreatesBacklogHypothesisFromCompletedStages() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        niche.setName("Salões de beleza");
        when(marketNicheRepository.findById(18L)).thenReturn(Optional.of(niche));
        mockCompletedStage("hypothesis-pain", "dor validada", "gpt-5.2", new BigDecimal("0.01000000"));
        mockCompletedStage("hypothesis-result", "resultado claro", "gpt-5.2", new BigDecimal("0.02000000"));
        mockCompletedStage("hypothesis-mechanism", "mecanismo plausível", "gpt-5.2", new BigDecimal("0.03000000"));
        mockCompletedStage("hypothesis-proof", "prova segura", "gpt-5.2", new BigDecimal("0.04000000"));
        mockCompletedStage("hypothesis-offer", "oferta low-ticket", "gpt-5.2", new BigDecimal("0.05000000"));
        when(hypothesisRepository.save(any(Hypothesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Hypothesis hypothesis = service.finalizeHypothesis(18L, new FinalizeHypothesisRequest(" Hipótese final "));

        assertEquals("Hipótese final", hypothesis.getTitle());
        assertEquals("Salões de beleza", hypothesis.getPersona());
        assertEquals("dor validada", hypothesis.getProblem());
        assertEquals("resultado claro", hypothesis.getPromise());
        assertEquals("mecanismo plausível", hypothesis.getMechanism());
        assertEquals("oferta low-ticket", hypothesis.getEntrega());
        assertEquals("prova segura", hypothesis.getSuccessRule());
        assertEquals("gpt-5.2", hypothesis.getModel());
        assertEquals(new BigDecimal("0.15000000"), hypothesis.getCostUsd());
        assertEquals(HypothesisStatus.BACKLOG, hypothesis.getStatus());
        ArgumentCaptor<Hypothesis> captor = ArgumentCaptor.forClass(Hypothesis.class);
        verify(hypothesisRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getFrameworkJson())
                .contains("dor validada")
                .contains("oferta low-ticket");
    }

    /** Deve bloquear o fechamento quando alguma etapa obrigatória ainda não tem resposta concluída. */
    @Test
    void finalizeHypothesisRequiresCompletedPainStage() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        when(marketNicheRepository.findById(18L)).thenReturn(Optional.of(niche));
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        "hypothesis-pain",
                        "CONCLUIDO"))
                .thenReturn(Optional.empty());

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.finalizeHypothesis(18L, new FinalizeHypothesisRequest("Hipótese")));

        assertEquals("A etapa Dor precisa estar concluída antes de fechar a hipótese.", error.getMessage());
    }

    /** Cria uma execução concluída simulada para a etapa informada. */
    private void mockCompletedStage(String stageCode, String response, String model, BigDecimal cost) {
        when(executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusOrderByExecutionRequestedAtDesc(
                        18L,
                        stageCode,
                        "CONCLUIDO"))
                .thenReturn(Optional.of(HypothesisPainStageExecution.builder()
                        .marketNicheId(18L)
                        .stageCode(stageCode)
                        .status("CONCLUIDO")
                        .modelResponse(response)
                        .openAiModel(model)
                        .costUsd(cost)
                        .build()));
    }
}
