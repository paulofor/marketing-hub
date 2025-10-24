package com.marketinghub.worker.instantform;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.marketinghub.worker.settings.PrivacyPolicyProvider;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private PrivacyPolicyProvider privacyPolicyProvider;

    @Mock
    private ExperimentFollowUpResolver experimentFollowUpResolver;

    private ExperimentInstantFormService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentInstantFormService(
                experimentRepository,
                journeyRepository,
                journeyStepRepository,
                instantFormRepository,
                chatGptClient,
                experimentGenerationRepository,
                privacyPolicyProvider,
                experimentFollowUpResolver,
                new ObjectMapper()
        );
        lenient().when(experimentFollowUpResolver.resolveFollowUpActionUrl(anyLong()))
                .thenReturn(Optional.empty());
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
        experiment.setFollowUpActionUrl("https://example.com/obrigado");
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
        when(privacyPolicyProvider.getPrivacyPolicyUrl()).thenReturn(Optional.of("https://example.com/privacidade"));

        ExperimentInstantFormChatGptClient.InstantFormPlan plan =
                new ExperimentInstantFormChatGptClient.InstantFormPlan(
                        "ai-form-demo",
                        "Formulário de Conversão",
                        "draft",
                        "pt_br",
                        "https://example.com/obrigado",
                        "Visite nosso site",
                        "https://example.com/privacidade",
                        new ExperimentInstantFormChatGptClient.PrivacyPolicyPlan(
                                "https://example.com/privacidade",
                                "Política de Privacidade"
                        ),
                        List.of(
                                new ExperimentInstantFormChatGptClient.QuestionPlan(
                                        "SHORT_ANSWER",
                                        "email",
                                        "Qual é o seu e-mail?",
                                        null,
                                        true,
                                        false,
                                        List.of()
                                )
                        )
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
        assertThat(saved.getQuestions()).contains("Qual é o seu e-mail?");

        verify(experimentRepository).save(experiment);
    }

    @Test
    void generateShouldPreferPlanUrlsWhenExperimentDefaultsMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(55L);
        experiment.setInstantFormsToGenerate(1);
        Hypothesis hypothesis = new Hypothesis();
        experiment.setHypothesisRef(hypothesis);
        FacebookPage page = new FacebookPage();
        page.setId(321L);
        experiment.setFacebookPage(page);
        JourneyTemplate template = new JourneyTemplate();
        experiment.setJourneyTemplate(template);

        JourneyStep step = new JourneyStep();
        step.setId(9L);
        step.setStimulusType(JourneyStimulusType.INSTANT_FORM);
        step.setPosition(1);
        step.setName("Cadastro");

        when(experimentGenerationRepository.findAllToGenerateInstantForms()).thenReturn(List.of(experiment));
        when(journeyStepRepository.findByTemplateOrderByPositionAsc(template)).thenReturn(List.of(step));
        when(journeyRepository.findFirstByExperimentIdOrderByCreatedAtDesc(55L)).thenReturn(Optional.empty());
        when(privacyPolicyProvider.getPrivacyPolicyUrl()).thenReturn(Optional.empty());

        ExperimentInstantFormChatGptClient.InstantFormPlan plan =
                new ExperimentInstantFormChatGptClient.InstantFormPlan(
                        null,
                        "Formulário Completo",
                        "draft",
                        "pt_BR",
                        "https://example.com/contato",
                        null,
                        null,
                        new ExperimentInstantFormChatGptClient.PrivacyPolicyPlan(
                                "https://example.com/politica",
                                null
                        ),
                        List.of()
                );
        ExperimentInstantFormChatGptClient.Generation generation =
                new ExperimentInstantFormChatGptClient.Generation(
                        List.of(plan),
                        "prompt",
                        "[]",
                        "gpt-4o"
                );

        when(chatGptClient.generateInstantForms(eq(experiment), isNull(), eq(1), anyList())).thenReturn(generation);
        when(instantFormRepository.save(any(FacebookInstantForm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<Long, List<FacebookInstantForm>> generated = service.generate();

        assertThat(generated).containsKey(55L);
        FacebookInstantForm saved = generated.get(55L).get(0);
        assertThat(saved.getFollowUpActionUrl()).isEqualTo("https://example.com/contato");
        assertThat(saved.getPrivacyPolicyUrl()).isEqualTo("https://example.com/politica");
        assertThat(saved.getQuestions()).isNull();
    }

    @Test
    void generateShouldUseExperimentFollowUpWhenDefined() {
        Experiment experiment = new Experiment();
        experiment.setId(77L);
        experiment.setInstantFormsToGenerate(1);
        Hypothesis hypothesis = new Hypothesis();
        experiment.setHypothesisRef(hypothesis);
        FacebookPage page = new FacebookPage();
        page.setId(654L);
        experiment.setFacebookPage(page);
        JourneyTemplate template = new JourneyTemplate();
        experiment.setJourneyTemplate(template);
        experiment.setFollowUpActionUrl("https://marketinghub.com/obrigado");

        JourneyStep step = new JourneyStep();
        step.setId(12L);
        step.setStimulusType(JourneyStimulusType.INSTANT_FORM);
        step.setPosition(1);
        step.setName("Cadastro");

        when(experimentGenerationRepository.findAllToGenerateInstantForms()).thenReturn(List.of(experiment));
        when(journeyStepRepository.findByTemplateOrderByPositionAsc(template)).thenReturn(List.of(step));
        when(journeyRepository.findFirstByExperimentIdOrderByCreatedAtDesc(77L)).thenReturn(Optional.empty());
        when(privacyPolicyProvider.getPrivacyPolicyUrl()).thenReturn(Optional.empty());

        ExperimentInstantFormChatGptClient.InstantFormPlan plan =
                new ExperimentInstantFormChatGptClient.InstantFormPlan(
                        "ai-form-generated",
                        "Formulário com CTA",
                        "draft",
                        "pt_BR",
                        "https://example.com/cta",
                        null,
                        null,
                        null,
                        List.of()
                );
        ExperimentInstantFormChatGptClient.Generation generation =
                new ExperimentInstantFormChatGptClient.Generation(
                        List.of(plan),
                        "prompt",
                        "[]",
                        "gpt-4o"
                );

        when(chatGptClient.generateInstantForms(eq(experiment), isNull(), eq(1), anyList())).thenReturn(generation);
        when(instantFormRepository.save(any(FacebookInstantForm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<Long, List<FacebookInstantForm>> generated = service.generate();

        assertThat(generated).containsKey(77L);
        FacebookInstantForm saved = generated.get(77L).get(0);
        assertThat(saved.getFollowUpActionUrl()).isEqualTo("https://marketinghub.com/obrigado");
        assertThat(saved.getQuestions()).isNull();
    }

    @Test
    void generateShouldResolveFollowUpFromBackendWhenExperimentValueMissing() {
        Experiment experiment = new Experiment();
        experiment.setId(88L);
        experiment.setInstantFormsToGenerate(1);
        Hypothesis hypothesis = new Hypothesis();
        experiment.setHypothesisRef(hypothesis);
        FacebookPage page = new FacebookPage();
        page.setId(111L);
        experiment.setFacebookPage(page);
        JourneyTemplate template = new JourneyTemplate();
        experiment.setJourneyTemplate(template);

        JourneyStep step = new JourneyStep();
        step.setId(21L);
        step.setStimulusType(JourneyStimulusType.INSTANT_FORM);
        step.setPosition(1);
        step.setName("Cadastro");

        when(experimentGenerationRepository.findAllToGenerateInstantForms()).thenReturn(List.of(experiment));
        when(journeyStepRepository.findByTemplateOrderByPositionAsc(template)).thenReturn(List.of(step));
        when(journeyRepository.findFirstByExperimentIdOrderByCreatedAtDesc(88L)).thenReturn(Optional.empty());
        when(privacyPolicyProvider.getPrivacyPolicyUrl()).thenReturn(Optional.empty());
        when(experimentFollowUpResolver.resolveFollowUpActionUrl(88L))
                .thenReturn(Optional.of("https://example.com/follow-up"));

        ExperimentInstantFormChatGptClient.InstantFormPlan plan =
                new ExperimentInstantFormChatGptClient.InstantFormPlan(
                        "ai-form-fallback",
                        "Formulário", 
                        "draft",
                        "pt_BR",
                        null,
                        null,
                        null,
                        null,
                        List.of()
                );
        ExperimentInstantFormChatGptClient.Generation generation =
                new ExperimentInstantFormChatGptClient.Generation(
                        List.of(plan),
                        "prompt",
                        "[]",
                        "gpt-4o"
                );

        when(chatGptClient.generateInstantForms(eq(experiment), isNull(), eq(1), anyList())).thenReturn(generation);
        when(instantFormRepository.save(any(FacebookInstantForm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<Long, List<FacebookInstantForm>> generated = service.generate();

        assertThat(generated).containsKey(88L);
        FacebookInstantForm saved = generated.get(88L).get(0);
        assertThat(saved.getFollowUpActionUrl()).isEqualTo("https://example.com/follow-up");
        assertThat(saved.getQuestions()).isNull();
        verify(experimentFollowUpResolver).resolveFollowUpActionUrl(88L);
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
