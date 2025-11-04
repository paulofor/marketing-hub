package com.marketinghub.worker.leadportal;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    private ExperimentLeadPortalFlowService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentLeadPortalFlowService(
                experimentRepository,
                leadPortalFlowRepository,
                generationRepository,
                chatGptClient
        );
    }

    @Test
    void generateShouldPersistFlowsAndResetCounter() {
        Experiment experiment = new Experiment();
        experiment.setId(77L);
        experiment.setLeadPortalFlowsToGenerate(1);
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setProblem("Queda de vendas");
        hypothesis.setPromise("Aumentar conversões em 30%");
        hypothesis.setPersona("Donos de pequenos e-commerces");
        experiment.setHypothesisRef(hypothesis);

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
                        "gpt-4o"
                );

        when(generationRepository.findAllToGenerateLeadPortalFlows()).thenReturn(List.of(experiment));
        when(chatGptClient.generateFlows(experiment, 1)).thenReturn(generation);
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
        assertThat(savedFlow.getQuestions()).isNotEmpty();
        LeadPortalFlowQuestion lastQuestion = savedFlow.getQuestions().get(savedFlow.getQuestions().size() - 1);
        assertThat(lastQuestion.getType()).isEqualTo(LeadPortalQuestionType.IMAGE_UPLOAD);
        assertThat(lastQuestion.getDataKey()).startsWith("foto_problema");
    }
}
