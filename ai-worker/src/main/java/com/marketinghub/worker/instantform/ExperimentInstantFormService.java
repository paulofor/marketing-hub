package com.marketinghub.worker.instantform;

import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.FacebookInstantFormRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço que gera instant forms planejados para hipóteses vinculadas a experimentos.
 */
@Service
public class ExperimentInstantFormService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentInstantFormService.class);

    private final ExperimentRepository experimentRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyStepRepository journeyStepRepository;
    private final FacebookInstantFormRepository instantFormRepository;
    private final ExperimentInstantFormChatGptClient chatGptClient;
    private final ExperimentGenerationRepository experimentGenerationRepository;

    public ExperimentInstantFormService(ExperimentRepository experimentRepository,
                                        JourneyRepository journeyRepository,
                                        JourneyStepRepository journeyStepRepository,
                                        FacebookInstantFormRepository instantFormRepository,
                                        ExperimentInstantFormChatGptClient chatGptClient,
                                        ExperimentGenerationRepository experimentGenerationRepository) {
        this.experimentRepository = experimentRepository;
        this.journeyRepository = journeyRepository;
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
                log.debug("Ignorando experimento {} sem solicitação de instant forms", experiment.getId());
                continue;
            }
            if (experiment.getHypothesisRef() == null) {
                log.warn("Experimento {} não possui hipótese vinculada; instant forms não serão gerados", experiment.getId());
                continue;
            }
            if (experiment.getFacebookPage() == null) {
                log.warn("Experimento {} não possui página do Facebook configurada; instant forms não serão gerados", experiment.getId());
                continue;
            }
            if (experiment.getJourneyTemplate() == null) {
                log.warn("Experimento {} não possui template de jornada configurado; instant forms não serão gerados", experiment.getId());
                continue;
            }

            List<JourneyStep> steps = journeyStepRepository.findByTemplateOrderByPositionAsc(experiment.getJourneyTemplate());
            List<ExperimentInstantFormChatGptClient.StepContext> contexts = new ArrayList<>();
            for (JourneyStep step : steps) {
                if (step.getStimulusType() == JourneyStimulusType.INSTANT_FORM) {
                    contexts.add(new ExperimentInstantFormChatGptClient.StepContext(
                            step.getId(),
                            step.getPosition(),
                            step.getName(),
                            step.getDescription(),
                            step.getMetadata()
                    ));
                }
            }
            if (contexts.isEmpty()) {
                log.warn("Template da jornada do experimento {} não possui etapas de instant form", experiment.getId());
                continue;
            }

            Optional<Journey> journeyOpt = journeyRepository.findFirstByExperimentIdOrderByCreatedAtDesc(experiment.getId());
            Journey journey = journeyOpt.orElse(null);

            log.info("Gerando {} instant forms para o experimento {}", quantity, experiment.getId());
            try {
                ExperimentInstantFormChatGptClient.Generation generation = chatGptClient.generateInstantForms(
                        experiment,
                        journey,
                        quantity,
                        contexts
                );
                List<ExperimentInstantFormChatGptClient.InstantFormPlan> plans = generation.plans();
                if (plans.isEmpty()) {
                    log.warn("ChatGPT não retornou instant forms para o experimento {}", experiment.getId());
                }
                List<FacebookInstantForm> persisted = new ArrayList<>();
                int processed = 0;
                for (ExperimentInstantFormChatGptClient.InstantFormPlan plan : plans) {
                    if (processed >= quantity) {
                        break;
                    }
                    FacebookInstantForm entity = buildEntity(plan, experiment, generation);
                    if (entity == null) {
                        continue;
                    }
                    persisted.add(instantFormRepository.save(entity));
                    processed++;
                }
                experiment.setInstantFormsToGenerate(0);
                experimentRepository.save(experiment);
                result.put(experiment.getId(), persisted);
                log.info("Instant forms gerados para experimento {}: {}", experiment.getId(), persisted.size());
            } catch (Exception ex) {
                log.error("Falha ao gerar instant forms para o experimento {}", experiment.getId(), ex);
            }
        }
        return result;
    }

    private FacebookInstantForm buildEntity(ExperimentInstantFormChatGptClient.InstantFormPlan plan,
                                            Experiment experiment,
                                            ExperimentInstantFormChatGptClient.Generation generation) {
        String name = sanitize(plan.name());
        if (!StringUtils.hasText(name)) {
            log.warn("Ignorando plano de instant form sem nome para o experimento {}", experiment.getId());
            return null;
        }
        String followUpActionUrl = sanitizeUrl(plan.followUpActionUrl());
        String privacyPolicyUrl = sanitizeUrl(plan.privacyPolicyUrl());
        if (!StringUtils.hasText(privacyPolicyUrl) && plan.privacyPolicy() != null) {
            privacyPolicyUrl = sanitizeUrl(plan.privacyPolicy().url());
        }

        FacebookInstantForm.FacebookInstantFormBuilder builder = FacebookInstantForm.builder()
                .hypothesis(experiment.getHypothesisRef())
                .page(experiment.getFacebookPage())
                .name(truncate(name, 255))
                .status(truncate(normalizeStatus(plan.status()), 50))
                .locale(truncate(normalizeLocale(plan.locale()), 12))
                .followUpActionUrl(followUpActionUrl)
                .privacyPolicyUrl(privacyPolicyUrl)
                .model(generation.model())
                .prompt(generation.auditTrail())
                .approved(false)
                .published(false);
        return builder.build();
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String normalizeStatus(String status) {
        String sanitized = sanitize(status);
        if (!StringUtils.hasText(sanitized)) {
            return null;
        }
        return sanitized.toUpperCase(Locale.ROOT);
    }

    private String normalizeLocale(String locale) {
        String sanitized = sanitize(locale);
        if (!StringUtils.hasText(sanitized)) {
            return null;
        }
        String normalized = sanitized.replace('-', '_');
        if (normalized.length() == 5 && normalized.charAt(2) == '_') {
            String lang = normalized.substring(0, 2).toLowerCase(Locale.ROOT);
            String country = normalized.substring(3).toUpperCase(Locale.ROOT);
            return lang + "_" + country;
        }
        return normalized;
    }

    private String sanitizeUrl(String url) {
        String sanitized = sanitize(url);
        if (!StringUtils.hasText(sanitized)) {
            return null;
        }
        if (sanitized.startsWith("http://") || sanitized.startsWith("https://")) {
            return truncate(sanitized, 512);
        }
        log.warn("Ignorando URL inválida gerada para instant form: {}", sanitized);
        return null;
    }
}
