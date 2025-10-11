package com.marketinghub.worker.email;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço que gera e preenche os e-mails planejados da jornada de um experimento.
 */
@Service
public class ExperimentEmailService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentEmailService.class);

    private final ExperimentRepository experimentRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyStepRepository journeyStepRepository;
    private final ExperimentEmailChatGptClient chatGptClient;

    public ExperimentEmailService(ExperimentRepository experimentRepository,
                                  JourneyRepository journeyRepository,
                                  JourneyStepRepository journeyStepRepository,
                                  ExperimentEmailChatGptClient chatGptClient) {
        this.experimentRepository = experimentRepository;
        this.journeyRepository = journeyRepository;
        this.journeyStepRepository = journeyStepRepository;
        this.chatGptClient = chatGptClient;
    }

    @Transactional
    public Map<Long, List<ExperimentEmailChatGptClient.EmailPlan>> generate() {
        Map<Long, List<ExperimentEmailChatGptClient.EmailPlan>> result = new LinkedHashMap<>();
        List<Experiment> experiments = experimentRepository.findAllToGenerateEmails();
        for (Experiment experiment : experiments) {
            Integer quantity = experiment.getEmailsToGenerate();
            if (quantity == null || quantity <= 0) {
                log.debug("Ignorando experimento {} sem solicitação de e-mails", experiment.getId());
                continue;
            }
            if (experiment.getJourneyTemplate() == null) {
                log.warn("Experimento {} não está vinculado a um template de jornada; e-mails não serão gerados", experiment.getId());
                continue;
            }
            Optional<Journey> journeyOpt = journeyRepository.findFirstByExperimentIdOrderByCreatedAtDesc(experiment.getId());
            if (journeyOpt.isEmpty()) {
                log.warn("Experimento {} não possui jornada construída; e-mails não serão gerados", experiment.getId());
                continue;
            }
            Journey journey = journeyOpt.get();
            List<JourneyStep> steps = journeyStepRepository.findByTemplateOrderByPositionAsc(experiment.getJourneyTemplate());
            Map<Long, JourneyStep> emailSteps = new LinkedHashMap<>();
            List<ExperimentEmailChatGptClient.StepContext> contexts = new ArrayList<>();
            for (JourneyStep step : steps) {
                if (step.getStimulusType() == JourneyStimulusType.EMAIL) {
                    emailSteps.put(step.getId(), step);
                    contexts.add(new ExperimentEmailChatGptClient.StepContext(
                            step.getId(),
                            step.getPosition(),
                            step.getName(),
                            step.getDescription(),
                            step.getMetadata()
                    ));
                }
            }
            if (emailSteps.isEmpty()) {
                log.warn("Template da jornada do experimento {} não possui etapas de e-mail", experiment.getId());
                continue;
            }

            log.info("Gerando {} e-mails para o experimento {}", quantity, experiment.getId());
            try {
                ExperimentEmailChatGptClient.Generation generation = chatGptClient.generateEmails(experiment, journey, quantity, contexts);
                List<ExperimentEmailChatGptClient.EmailPlan> plans = generation.plans();
                if (plans.isEmpty()) {
                    log.warn("ChatGPT não retornou e-mails para o experimento {}", experiment.getId());
                }
                List<ExperimentEmailChatGptClient.EmailPlan> persistedPlans = new ArrayList<>();
                Map<String, String> metadata = new LinkedHashMap<>(journey.getMetadata() != null ? journey.getMetadata() : Map.of());
                int processed = 0;
                for (ExperimentEmailChatGptClient.EmailPlan plan : plans) {
                    if (processed >= quantity) {
                        break;
                    }
                    JourneyStep step = emailSteps.get(plan.stepId());
                    if (step == null) {
                        log.warn("Ignorando plano de e-mail com stepId {} inexistente para o experimento {}", plan.stepId(), experiment.getId());
                        continue;
                    }
                    String subject = sanitize(plan.subject());
                    String templateId = sanitize(plan.templateId());
                    if (!StringUtils.hasText(templateId)) {
                        templateId = generateTemplateId(step.getId());
                    }
                    String status = normaliseStatus(plan.status());
                    String notes = combineNotes(plan.notes(), plan.callToAction());
                    putOrRemove(metadata, key(step.getId(), "subject"), subject);
                    putOrRemove(metadata, key(step.getId(), "templateId"), templateId);
                    putOrRemove(metadata, key(step.getId(), "status"), status);
                    putOrRemove(metadata, key(step.getId(), "notes"), notes);
                    putOrRemove(metadata, key(step.getId(), "preheader"), sanitize(plan.preheader()));
                    putOrRemove(metadata, key(step.getId(), "model"), generation.model());
                    putOrRemove(metadata, key(step.getId(), "prompt"), generation.auditTrail());
                    persistedPlans.add(plan);
                    processed++;
                }
                journey.setMetadata(metadata);
                journeyRepository.save(journey);
                experiment.setEmailsToGenerate(0);
                experimentRepository.save(experiment);
                result.put(experiment.getId(), persistedPlans);
                log.info("Planos de e-mail aplicados para experimento {}: {}", experiment.getId(), persistedPlans.size());
            } catch (Exception ex) {
                log.error("Falha ao gerar e-mails para o experimento {}", experiment.getId(), ex);
            }
        }
        return result;
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String combineNotes(String notes, String callToAction) {
        String cleanedNotes = sanitize(notes);
        String cleanedCta = sanitize(callToAction);
        if (!StringUtils.hasText(cleanedCta)) {
            return cleanedNotes;
        }
        if (!StringUtils.hasText(cleanedNotes)) {
            return "CTA sugerido: " + cleanedCta;
        }
        return cleanedNotes + "\nCTA sugerido: " + cleanedCta;
    }

    private String normaliseStatus(String status) {
        String cleaned = sanitize(status);
        if (!StringUtils.hasText(cleaned)) {
            return "draft";
        }
        String lower = cleaned.toLowerCase();
        return switch (lower) {
            case "draft", "review", "approved" -> lower;
            default -> "draft";
        };
    }

    private void putOrRemove(Map<String, String> metadata, String key, String value) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
    }

    private String key(Long stepId, String field) {
        return "email.step." + stepId + "." + field;
    }

    private String generateTemplateId(Long stepId) {
        String suffix = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        return "ai-email-" + stepId + "-" + suffix;
    }
}
