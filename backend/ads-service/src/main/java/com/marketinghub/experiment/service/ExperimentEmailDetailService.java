package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentEmailDetailDto;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.repository.jpa.journey.JourneyRepository;
import com.marketinghub.repository.jpa.journey.JourneyStepRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads and mutates the email metadata stored on journeys.
 */
@Service
public class ExperimentEmailDetailService {
    private static final String EMAIL_STEP_PREFIX = "email.step.";

    private final ExperimentRepository experimentRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyStepRepository journeyStepRepository;

    public ExperimentEmailDetailService(ExperimentRepository experimentRepository,
                                        JourneyRepository journeyRepository,
                                        JourneyStepRepository journeyStepRepository) {
        this.experimentRepository = experimentRepository;
        this.journeyRepository = journeyRepository;
        this.journeyStepRepository = journeyStepRepository;
    }

    @Transactional(readOnly = true)
    public ExperimentEmailDetailDto get(Long experimentId, Long stepId) {
        Context context = loadContext(experimentId, stepId);
        return toDto(context);
    }

    @Transactional
    public ExperimentEmailDetailDto updateApproval(Long experimentId, Long stepId, boolean approved) {
        Context context = loadContext(experimentId, stepId);
        Map<String, String> metadata = new LinkedHashMap<>(context.journey().getMetadata() != null
                ? context.journey().getMetadata()
                : Map.of());
        metadata.put(metadataKey(stepId, "status"), approved ? "approved" : "review");
        context.journey().setMetadata(metadata);
        Journey saved = journeyRepository.save(context.journey());
        context = context.withJourney(saved);
        return toDto(context);
    }

    @Transactional
    public void delete(Long experimentId, Long stepId) {
        Context context = loadContext(experimentId, stepId);
        Map<String, String> metadata = new LinkedHashMap<>(context.journey().getMetadata() != null
                ? context.journey().getMetadata()
                : Map.of());
        metadata.remove(metadataKey(stepId, "subject"));
        metadata.remove(metadataKey(stepId, "templateId"));
        metadata.remove(metadataKey(stepId, "status"));
        metadata.remove(metadataKey(stepId, "notes"));
        metadata.remove(metadataKey(stepId, "preheader"));
        metadata.remove(metadataKey(stepId, "model"));
        metadata.remove(metadataKey(stepId, "prompt"));
        context.journey().setMetadata(metadata);
        journeyRepository.save(context.journey());
    }

    private Context loadContext(Long experimentId, Long stepId) {
        if (experimentId == null || stepId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "experimentId and stepId are required");
        }
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found"));
        if (experiment.getJourneyTemplate() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment does not have a journey template");
        }
        JourneyStep step = journeyStepRepository.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey step not found"));
        if (!step.getTemplate().getId().equals(experiment.getJourneyTemplate().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step does not belong to experiment template");
        }
        if (step.getStimulusType() != JourneyStimulusType.EMAIL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step is not configured for email");
        }
        Journey journey = journeyRepository.findFirstByExperimentIdOrderByCreatedAtDesc(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment does not have a journey"));
        return new Context(experiment, journey, step);
    }

    private ExperimentEmailDetailDto toDto(Context context) {
        Map<String, String> metadata = context.journey().getMetadata() != null
                ? context.journey().getMetadata()
                : Map.of();
        Long stepId = context.step().getId();
        String status = metadata.get(metadataKey(stepId, "status"));
        boolean approved = "approved".equalsIgnoreCase(status);
        return new ExperimentEmailDetailDto(
                context.experiment().getId(),
                context.journey().getId(),
                stepId,
                context.step().getName(),
                context.step().getPosition(),
                context.step().getPhase() != null ? context.step().getPhase().name() : null,
                context.step().getDescription(),
                context.step().getMetadata(),
                metadata.get(metadataKey(stepId, "subject")),
                metadata.get(metadataKey(stepId, "templateId")),
                status,
                metadata.get(metadataKey(stepId, "notes")),
                metadata.get(metadataKey(stepId, "preheader")),
                metadata.get(metadataKey(stepId, "model")),
                metadata.get(metadataKey(stepId, "prompt")),
                approved,
                context.journey().getCreatedAt(),
                context.journey().getUpdatedAt()
        );
    }

    private String metadataKey(Long stepId, String field) {
        return EMAIL_STEP_PREFIX + stepId + "." + field;
    }

    private record Context(Experiment experiment, Journey journey, JourneyStep step) {
        Context withJourney(Journey journey) {
            return new Context(experiment, journey, step);
        }
    }
}
