package com.marketinghub.worker.instantform;

import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.FacebookInstantFormRepository;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentInstantFormServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private JourneyRepository journeyRepository;

    @Mock
    private JourneyStepRepository journeyStepRepository;

    @Mock
    private FacebookInstantFormRepository instantFormRepository;

    @Mock
    private ExperimentInstantFormChatGptClient chatGptClient;

    @Mock
    private ExperimentGenerationRepository experimentGenerationRepository;

    private ExperimentInstantFormService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentInstantFormService(
                experimentRepository,
                journeyRepository,
                journeyStepRepository,
                instantFormRepository,
                chatGptClient,
                experimentGenerationRepository
        );
    }

    @Test
    void generateShouldPersistInstantFormAndResetCounter() {
        Experiment experiment = new Experiment();
        experiment.setId(42L);
        experiment.setInstantFormsToGenerate(1);
        Hypothesis hypothesis = new Hypothesis();
        experiment.setHypothesisRef(hypothesis);
        FacebookPage page = new FacebookPage();
        page.setId(99L);
        page.setName("Academia Fit");
        page.setPageId("123456");
        experiment.setFacebookPage(page);
        JourneyTemplate template = new JourneyTemplate();
        experiment.setJourneyTemplate(template);

        JourneyStep step = new JourneyStep();
        step.setId(7L);
        step.setStimulusType(JourneyStimulusType.INSTANT_FORM);
        step.setPosition(1);
        step.setName("Cadastro");
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("objetivo", "Qualificar leads para o trial");
        step.setMetadata(metadata);

        when(experimentGenerationRepository.findAllToGenerateInstantForms()).thenReturn(List.of(experiment));
        when(journeyStepRepository.findByTemplateOrderByPositionAsc(template)).thenReturn(List.of(step));
        when(journeyRepository.findFirstByExperimentIdOrderByCreatedAtDesc(42L)).thenReturn(Optional.empty());

        ExperimentInstantFormChatGptClient.InstantFormPlan plan =
                new ExperimentInstantFormChatGptClient.InstantFormPlan(
                        "AI Form Demo",
                        "Formulário de Conversão",
                        "draft",
                        "pt_br",
                        "https://example.com/obrigado",
                        "https://example.com/privacidade"
                );
        ExperimentInstantFormChatGptClient.Generation generation =
                new ExperimentInstantFormChatGptClient.Generation(
                        List.of(plan),
                        "prompt base",
                        "[{\"formId\":\"ai-form-demo\"}]",
                        "gpt-4o"
                );

        when(chatGptClient.generateInstantForms(eq(experiment), isNull(), eq(1), anyList())).thenReturn(generation);
        when(instantFormRepository.save(any(FacebookInstantForm.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentRepository.save(experiment)).thenAnswer(invocation -> invocation.getArgument(0));

        Map<Long, List<FacebookInstantForm>> generated = service.generate();

        assertThat(generated).containsKey(42L);
        assertThat(generated.get(42L)).hasSize(1);
        assertThat(experiment.getInstantFormsToGenerate()).isEqualTo(0);

        ArgumentCaptor<FacebookInstantForm> formCaptor = ArgumentCaptor.forClass(FacebookInstantForm.class);
        verify(instantFormRepository).save(formCaptor.capture());
        FacebookInstantForm saved = formCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Formulário de Conversão");
        assertThat(saved.getFormId()).isNull();
        assertThat(saved.getModel()).isEqualTo("gpt-4o");
        assertThat(saved.getPrompt()).contains("PROMPT").contains("RESPOSTA");
        assertThat(saved.getFollowUpActionUrl()).isEqualTo("https://example.com/obrigado");
        assertThat(saved.getPrivacyPolicyUrl()).isEqualTo("https://example.com/privacidade");

        verify(experimentRepository).save(experiment);
    }

    @Test
    void generateShouldSkipWhenNoInstantFormSteps() {
        Experiment experiment = new Experiment();
        experiment.setId(11L);
        experiment.setInstantFormsToGenerate(1);
        experiment.setHypothesisRef(new Hypothesis());
        experiment.setFacebookPage(new FacebookPage());
        experiment.setJourneyTemplate(new JourneyTemplate());

        when(experimentGenerationRepository.findAllToGenerateInstantForms()).thenReturn(List.of(experiment));
        when(journeyStepRepository.findByTemplateOrderByPositionAsc(any(JourneyTemplate.class))).thenReturn(List.of());

        Map<Long, List<FacebookInstantForm>> generated = service.generate();

        assertThat(generated).isEmpty();
        verify(chatGptClient, never()).generateInstantForms(any(), any(), anyInt(), anyList());
        verify(instantFormRepository, never()).save(any());
    }
}
