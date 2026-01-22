
package com.marketinghub.worker.leadportal;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentLeadPortalFlowServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private LeadPortalFlowRepository leadPortalFlowRepository;

    @Mock
    private ExperimentGenerationRepository generationRepository;

    @Mock
    private ExperimentLeadPortalFlowChatGptClient chatGptClient;

    @Mock
    private HypothesisRepository hypothesisRepository;

    @Mock
    private MarketNicheRepository marketNicheRepository;

    private ExperimentLeadPortalFlowService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentLeadPortalFlowService(
                experimentRepository,
                leadPortalFlowRepository,
                generationRepository,
                hypothesisRepository,
                marketNicheRepository,
                chatGptClient
        );
    }

    @Test
    void generateShouldPersistFlowsAndResetCounter() {
        Experiment experiment = new Experiment();
        experiment.setId(77L);
        experiment.setLeadPortalFlowsToGenerate(1);
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(UUID.randomUUID());
        hypothesis.setProblem("Queda de vendas");
        hypothesis.setPromise("Aumentar conversões em 30%");
        hypothesis.setPersona("Donos de pequenos e-commerces");
        experiment.setHypothesisRef(hypothesis);
        MarketNiche niche = new MarketNiche();
        niche.setId(15L);
        experiment.setNiche(niche);

        ExperimentLeadPortalFlowChatGptClient.QuestionPlan questionPlan =
                new ExperimentLeadPortalFlowChatGptClient.QuestionPlan(
                        "Qual o principal desafio hoje?",
                        "desafio_principal",
                        LeadPortalQuestionType.SINGLE_CHOICE,
                        true,
                        "Escolha a opção que melhor resume sua situação",
                        null,
                        List.of("Poucas vendas", "Pouco tráfego", "Custo alto")
                );
        ExperimentLeadPortalFlowChatGptClient.FlowPlan flowPlan =
                new ExperimentLeadPortalFlowChatGptClient.FlowPlan(
                        "Fluxo Diagnóstico",
                        "fluxo-diagnostico",
                        "Coleta dados iniciais do lead",
                        List.of(questionPlan)
                );
        ExperimentLeadPortalFlowChatGptClient.Generation generation =
                new ExperimentLeadPortalFlowChatGptClient.Generation(
                        List.of(flowPlan),
                        "prompt-base",
                        "[{\"name\":\"Fluxo Diagnóstico\"}]",
                        "gpt-4o",
                        BigDecimal.valueOf(0.12)
                );

        when(generationRepository.findAllToGenerateLeadPortalFlows()).thenReturn(List.of(experiment));
        when(chatGptClient.generateFlowsBatch(any())).thenReturn(Map.of(77L, generation));
        when(leadPortalFlowRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(leadPortalFlowRepository.save(any(LeadPortalFlow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentRepository.save(experiment)).thenAnswer(invocation -> invocation.getArgument(0));

        Map<Long, List<LeadPortalFlow>> generated = service.generate();

        assertThat(generated).containsKey(77L);
        assertThat(generated.get(77L)).hasSize(1);
        assertThat(experiment.getLeadPortalFlowsToGenerate()).isEqualTo(0);

        ArgumentCaptor<LeadPortalFlow> flowCaptor = ArgumentCaptor.forClass(LeadPortalFlow.class);
        verify(leadPortalFlowRepository).save(flowCaptor.capture());
        LeadPortalFlow savedFlow = flowCaptor.getValue();
        assertThat(savedFlow.getModel()).isEqualTo("gpt-4o");
        assertThat(savedFlow.getPrompt()).contains("PROMPT:");
        assertThat(savedFlow.getPrompt()).contains("RESPOSTA:");
        assertThat(savedFlow.getExperiment()).isEqualTo(experiment);
        assertThat(savedFlow.getCostUsd()).isEqualByComparingTo(BigDecimal.valueOf(0.12));
        assertThat(savedFlow.getQuestions()).isNotEmpty();
        LeadPortalFlowQuestion lastQuestion = savedFlow.getQuestions().get(savedFlow.getQuestions().size() - 1);
        assertThat(lastQuestion.getType()).isEqualTo(LeadPortalQuestionType.IMAGE_UPLOAD);
        assertThat(lastQuestion.getDataKey()).startsWith("foto_problema");
        BigDecimal expectedCost = new BigDecimal("0.12");
        verify(experimentRepository).incrementTotalCost(77L, argThat(cost -> cost.compareTo(expectedCost) == 0));
        verify(hypothesisRepository).incrementTotalCost(hypothesis.getId(), argThat(cost -> cost.compareTo(expectedCost) == 0));
        verify(marketNicheRepository).incrementTotalCost(niche.getId(), argThat(cost -> cost.compareTo(expectedCost) == 0));
    }
}
