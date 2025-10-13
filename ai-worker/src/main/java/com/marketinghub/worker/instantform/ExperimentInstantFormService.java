package com.marketinghub.worker.instantform;

import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.FacebookInstantFormRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import com.marketinghub.worker.facebook.FacebookLeadGenFormClient;
import com.marketinghub.worker.facebook.FacebookWorkerConfigurationClient;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.repository.JourneyStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável por gerar Instant Forms aprovados para os experimentos.
 */
@Service
public class ExperimentInstantFormService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentInstantFormService.class);

    private final ExperimentRepository experimentRepository;
    private final JourneyStepRepository journeyStepRepository;
    private final FacebookInstantFormRepository instantFormRepository;
    private final InstantFormChatGptClient chatGptClient;
    private final FacebookWorkerConfigurationClient configurationClient;
    private final FacebookLeadGenFormClient leadGenFormClient;
    private final ExperimentGenerationRepository experimentGenerationRepository;

    public ExperimentInstantFormService(ExperimentRepository experimentRepository,
                                        JourneyStepRepository journeyStepRepository,
                                        FacebookInstantFormRepository instantFormRepository,
                                        InstantFormChatGptClient chatGptClient,
                                        FacebookWorkerConfigurationClient configurationClient,
                                        FacebookLeadGenFormClient leadGenFormClient,
                                        ExperimentGenerationRepository experimentGenerationRepository) {
        this.experimentRepository = experimentRepository;
        this.journeyStepRepository = journeyStepRepository;
        this.instantFormRepository = instantFormRepository;
        this.chatGptClient = chatGptClient;
        this.configurationClient = configurationClient;
        this.leadGenFormClient = leadGenFormClient;
        this.experimentGenerationRepository = experimentGenerationRepository;
    }

    @Transactional
    public Map<Long, List<FacebookInstantForm>> generate() {
        Map<Long, List<FacebookInstantForm>> result = new LinkedHashMap<>();
        List<Experiment> experiments = experimentGenerationRepository.findAllToGenerateInstantForms();
        FacebookWorkerConfigurationClient.FacebookWorkerConfiguration configuration = configurationClient
                .fetchConfiguration()
                .orElse(null);
        if (configuration == null || !StringUtils.hasText(configuration.accessToken())) {
            log.warn("Configuração do worker do Facebook indisponível ou sem access token; criação de instant forms será pulada");
        }
        for (Experiment experiment : experiments) {
            Integer quantity = experiment.getInstantFormsToGenerate();
            if (quantity == null || quantity <= 0) {
                log.debug("Ignorando experimento {} sem quantidade de instant forms a gerar", experiment.getId());
                continue;
            }
            if (experiment.getHypothesisRef() == null) {
                log.warn("Experimento {} não possui hipótese carregada; instant forms não serão gerados", experiment.getId());
                continue;
            }
            if (experiment.getFacebookPage() == null) {
                log.warn("Experimento {} não possui página do Facebook vinculada; instant forms não serão gerados", experiment.getId());
                continue;
            }
            if (configuration == null || !StringUtils.hasText(configuration.accessToken())) {
                log.warn("Não foi possível criar instant forms no experimento {} por falta de access token válido", experiment.getId());
                continue;
            }
            String pageId = sanitize(experiment.getFacebookPage().getPageId());
            if (!StringUtils.hasText(pageId)) {
                log.warn("Página do experimento {} não possui pageId válido; instant forms não serão gerados", experiment.getId());
                continue;
            }

            log.info("Gerando {} instant forms para o experimento {}", quantity, experiment.getId());
            try {
                List<InstantFormChatGptClient.StepContext> contexts = loadStepContexts(experiment);
                InstantFormChatGptClient.Generation generation = chatGptClient.generateInstantForms(experiment, quantity, contexts);
                List<InstantFormChatGptClient.InstantFormPlan> plans = generation.plans();
                if (plans.isEmpty()) {
                    log.warn("ChatGPT não retornou instant forms para o experimento {}", experiment.getId());
                }
                List<FacebookInstantForm> saved = new ArrayList<>();
                int limit = Math.min(quantity, plans.size());
                for (int i = 0; i < limit; i++) {
                    InstantFormChatGptClient.InstantFormPlan plan = sanitizePlan(plans.get(i));
                    if (plan == null || !StringUtils.hasText(plan.name())) {
                        log.warn("Plano de instant form inválido retornado para o experimento {}", experiment.getId());
                        continue;
                    }
                    FacebookLeadGenFormClient.LeadGenForm createdForm = leadGenFormClient
                            .createAndActivateLeadGenForm(configuration.accessToken(), pageId, plan);
                    if (createdForm == null || !StringUtils.hasText(createdForm.id())) {
                        log.warn("Falha ao criar instant form no Facebook para experimento {}", experiment.getId());
                        continue;
                    }
                    Instant createdTime = createdForm.createdTime() != null ? createdForm.createdTime() : Instant.now();
                    Instant updatedTime = createdForm.updatedTime() != null ? createdForm.updatedTime() : createdTime;
                    String status = sanitize(createdForm.status());
                    if (!StringUtils.hasText(status)) {
                        status = sanitize(plan.status());
                    }
                    FacebookInstantForm entity = FacebookInstantForm.builder()
                            .hypothesis(experiment.getHypothesisRef())
                            .page(experiment.getFacebookPage())
                            .formId(createdForm.id())
                            .name(plan.name())
                            .status(status)
                            .locale(plan.locale())
                            .followUpActionUrl(plan.followUpActionUrl())
                            .privacyPolicyUrl(plan.privacyPolicyUrl())
                            .leadsCount(createdForm.leadsCount() != null ? createdForm.leadsCount() : 0L)
                            .createdTime(createdTime)
                            .updatedTime(updatedTime)
                            .model(generation.model())
                            .prompt(generation.auditTrail())
                            .build();
                    saved.add(instantFormRepository.save(entity));
                }
                if (!saved.isEmpty()) {
                    experiment.setInstantFormsToGenerate(0);
                    experimentRepository.save(experiment);
                    result.put(experiment.getId(), saved);
                    log.info("Instant forms persistidos para experimento {}: {}", experiment.getId(), saved.size());
                } else {
                    log.warn("Nenhum instant form foi persistido para o experimento {}; manteremos a quantidade para nova tentativa", experiment.getId());
                }
            } catch (Exception ex) {
                log.error("Falha ao gerar instant forms para o experimento {}", experiment.getId(), ex);
            }
        }
        return result;
    }

    private List<InstantFormChatGptClient.StepContext> loadStepContexts(Experiment experiment) {
        if (experiment.getJourneyTemplate() == null) {
            return List.of();
        }
        List<JourneyStep> steps = journeyStepRepository.findByTemplateOrderByPositionAsc(experiment.getJourneyTemplate());
        List<InstantFormChatGptClient.StepContext> contexts = new ArrayList<>();
        for (JourneyStep step : steps) {
            if (step.getStimulusType() == JourneyStimulusType.LANDING_PAGE || step.getStimulusType() == JourneyStimulusType.INSTANT_FORM) {
                contexts.add(new InstantFormChatGptClient.StepContext(
                        step.getId(),
                        step.getPosition(),
                        step.getName(),
                        step.getDescription(),
                        step.getMetadata()
                ));
            }
        }
        return contexts;
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private InstantFormChatGptClient.InstantFormPlan sanitizePlan(InstantFormChatGptClient.InstantFormPlan plan) {
        if (plan == null) {
            return null;
        }
        List<InstantFormChatGptClient.InstantFormPlan.Question> questions = new ArrayList<>();
        if (plan.questions() != null) {
            for (InstantFormChatGptClient.InstantFormPlan.Question question : plan.questions()) {
                if (question == null) {
                    continue;
                }
                List<String> options = new ArrayList<>();
                if (question.options() != null) {
                    question.options().forEach(opt -> {
                        String sanitized = sanitize(opt);
                        if (StringUtils.hasText(sanitized)) {
                            options.add(sanitized);
                        }
                    });
                }
                String label = sanitize(question.label());
                String helpText = sanitize(question.helpText());
                if (!StringUtils.hasText(label) && options.isEmpty()) {
                    continue;
                }
                questions.add(new InstantFormChatGptClient.InstantFormPlan.Question(question.type(), label, options.isEmpty() ? null : options, helpText));
            }
        }
        return new InstantFormChatGptClient.InstantFormPlan(
                sanitize(plan.name()),
                sanitize(plan.status()),
                sanitize(plan.locale()),
                sanitize(plan.followUpActionUrl()),
                sanitize(plan.privacyPolicyUrl()),
                sanitize(plan.valueProposition()),
                sanitize(plan.leadMagnet()),
                questions.isEmpty() ? null : questions,
                sanitize(plan.automationNotes()),
                sanitize(plan.complianceNotes())
        );
    }
}
