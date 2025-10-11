package com.marketinghub.worker.instantform;

import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.FacebookInstantFormRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
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
import java.util.UUID;

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
    private final ExperimentGenerationRepository experimentGenerationRepository;

    public ExperimentInstantFormService(ExperimentRepository experimentRepository,
                                        JourneyStepRepository journeyStepRepository,
                                        FacebookInstantFormRepository instantFormRepository,
                                        InstantFormChatGptClient chatGptClient,
                                        ExperimentGenerationRepository experimentGenerationRepository) {
        this.experimentRepository = experimentRepository;
        this.journeyStepRepository = journeyStepRepository;
        this.instantFormRepository = instantFormRepository;
        this.chatGptClient = chatGptClient;
        this.experimentGenerationRepository = experimentGenerationRepository;
    }

    @Transactional
    public Map<Long, List<FacebookInstantForm>> generate() {
        Map<Long, List<FacebookInstantForm>> result = new LinkedHashMap<>();
        List<Experiment> experiments = experimentGenerationRepository.findAllToGenerateInstantForms();
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
                    InstantFormChatGptClient.InstantFormPlan plan = plans.get(i);
                    FacebookInstantForm entity = FacebookInstantForm.builder()
                            .hypothesis(experiment.getHypothesisRef())
                            .page(experiment.getFacebookPage())
                            .formId(generateFormId(experiment, i))
                            .name(plan.name().trim())
                            .status(sanitize(plan.status()))
                            .locale(sanitize(plan.locale()))
                            .followUpActionUrl(sanitize(plan.followUpActionUrl()))
                            .privacyPolicyUrl(sanitize(plan.privacyPolicyUrl()))
                            .leadsCount(0L)
                            .createdTime(Instant.now())
                            .updatedTime(Instant.now())
                            .model(generation.model())
                            .prompt(generation.auditTrail())
                            .build();
                    saved.add(instantFormRepository.save(entity));
                }
                experiment.setInstantFormsToGenerate(0);
                experimentRepository.save(experiment);
                result.put(experiment.getId(), saved);
                log.info("Instant forms persistidos para experimento {}: {}", experiment.getId(), saved.size());
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

    private String generateFormId(Experiment experiment, int index) {
        String experimentId = experiment.getId() != null ? experiment.getId().toString() : "exp";
        String suffix = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12);
        return "ai_form_" + experimentId + "_" + index + "_" + suffix;
    }
}
