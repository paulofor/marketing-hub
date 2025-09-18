package com.marketinghub.journey.service;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.EmotionalTrigger;
import com.marketinghub.creative.label.VisualProof;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.creative.label.repository.EmotionalTriggerRepository;
import com.marketinghub.creative.label.repository.VisualProofRepository;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.journey.dto.JourneyStepRequest;
import com.marketinghub.journey.dto.JourneyStepUpdateRequest;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyStepRepository;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Service responsible for managing steps of a journey template.
 */
@Service
public class JourneyStepService {
    private final JourneyTemplateRepository templateRepository;
    private final JourneyStepRepository stepRepository;
    private final CreativeRepository creativeRepository;
    private final AngleRepository angleRepository;
    private final VisualProofRepository visualProofRepository;
    private final EmotionalTriggerRepository emotionalTriggerRepository;

    public JourneyStepService(JourneyTemplateRepository templateRepository,
                              JourneyStepRepository stepRepository,
                              CreativeRepository creativeRepository,
                              AngleRepository angleRepository,
                              VisualProofRepository visualProofRepository,
                              EmotionalTriggerRepository emotionalTriggerRepository) {
        this.templateRepository = templateRepository;
        this.stepRepository = stepRepository;
        this.creativeRepository = creativeRepository;
        this.angleRepository = angleRepository;
        this.visualProofRepository = visualProofRepository;
        this.emotionalTriggerRepository = emotionalTriggerRepository;
    }

    @Transactional(readOnly = true)
    public List<JourneyStep> listByTemplate(Long templateId) {
        JourneyTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey template not found"));
        return stepRepository.findByTemplateOrderByPositionAsc(template);
    }

    @Transactional(readOnly = true)
    public JourneyStep get(Long stepId) {
        return stepRepository.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey step not found"));
    }

    @Transactional
    public JourneyStep create(Long templateId, JourneyStepRequest request) {
        JourneyTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey template not found"));

        JourneyStep step = JourneyStep.builder()
                .template(template)
                .name(request.name())
                .description(request.description())
                .phase(request.phase())
                .stimulusType(request.stimulusType())
                .entryCondition(request.entryCondition())
                .exitCondition(request.exitCondition())
                .delayMinutes(request.delayMinutes())
                .metadata(request.metadata() != null ? new LinkedHashMap<>(request.metadata()) : new LinkedHashMap<>())
                .build();

        step.setCreative(resolveCreative(request.creativeId()));
        step.setAngle(resolveAngle(request.angleId()));
        step.setVisualProof(resolveVisualProof(request.visualProofId()));
        step.setEmotionalTrigger(resolveEmotionalTrigger(request.emotionalTriggerId()));

        int initialPosition = resolveInitialPosition(template, request.position());
        step.setPosition(initialPosition);

        JourneyStep saved = stepRepository.save(step);
        reorderSteps(template, saved, request.position());
        return saved;
    }

    @Transactional
    public JourneyStep update(Long stepId, JourneyStepUpdateRequest request) {
        JourneyStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey step not found"));

        if (request.name() != null) {
            step.setName(request.name());
        }
        if (request.description() != null) {
            step.setDescription(request.description());
        }
        if (request.phase() != null) {
            step.setPhase(request.phase());
        }
        if (request.stimulusType() != null) {
            step.setStimulusType(request.stimulusType());
        }
        if (request.entryCondition() != null) {
            step.setEntryCondition(request.entryCondition());
        }
        if (request.exitCondition() != null) {
            step.setExitCondition(request.exitCondition());
        }
        if (request.delayMinutes() != null) {
            step.setDelayMinutes(request.delayMinutes());
        }
        if (request.metadata() != null) {
            step.setMetadata(new LinkedHashMap<>(request.metadata()));
        }

        boolean positionUpdated = request.position() != null && !Objects.equals(request.position(), step.getPosition());
        if (positionUpdated) {
            step.setPosition(resolveInitialPosition(step.getTemplate(), request.position()));
        }

        if (request.creativeId() != null) {
            step.setCreative(resolveCreative(request.creativeId()));
        }
        if (request.angleId() != null) {
            step.setAngle(resolveAngle(request.angleId()));
        }
        if (request.visualProofId() != null) {
            step.setVisualProof(resolveVisualProof(request.visualProofId()));
        }
        if (request.emotionalTriggerId() != null) {
            step.setEmotionalTrigger(resolveEmotionalTrigger(request.emotionalTriggerId()));
        }

        JourneyStep saved = stepRepository.save(step);
        if (positionUpdated) {
            reorderSteps(step.getTemplate(), saved, request.position());
        }
        return saved;
    }

    @Transactional
    public void delete(Long stepId) {
        JourneyStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey step not found"));
        JourneyTemplate template = step.getTemplate();
        stepRepository.delete(step);
        normalizeTemplatePositions(template);
    }

    private int resolveInitialPosition(JourneyTemplate template, Integer desiredPosition) {
        if (desiredPosition != null && desiredPosition > 0) {
            return desiredPosition;
        }
        return stepRepository.findByTemplateOrderByPositionAsc(template).stream()
                .map(JourneyStep::getPosition)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(pos -> pos + 1)
                .orElse(1);
    }

    private void reorderSteps(JourneyTemplate template, JourneyStep targetStep, Integer desiredPosition) {
        List<JourneyStep> steps = stepRepository.findByTemplateOrderByPositionAsc(template);
        steps.removeIf(existing -> existing.getId() != null && existing.getId().equals(targetStep.getId()));

        int totalSize = steps.size() + 1;
        int insertionIndex = computeInsertionIndex(desiredPosition, totalSize);
        steps.add(insertionIndex, targetStep);

        persistNormalizedPositions(steps);
    }

    private int computeInsertionIndex(Integer desiredPosition, int totalSize) {
        if (desiredPosition == null || desiredPosition <= 0) {
            return totalSize - 1;
        }
        int zeroBased = desiredPosition - 1;
        return Math.min(zeroBased, totalSize - 1);
    }

    private void normalizeTemplatePositions(JourneyTemplate template) {
        List<JourneyStep> steps = stepRepository.findByTemplateOrderByPositionAsc(template);
        persistNormalizedPositions(steps);
    }

    private void persistNormalizedPositions(List<JourneyStep> steps) {
        int index = 1;
        boolean dirty = false;
        for (JourneyStep existing : steps) {
            if (!Objects.equals(existing.getPosition(), index)) {
                existing.setPosition(index);
                dirty = true;
            }
            index++;
        }
        if (dirty) {
            stepRepository.saveAll(steps);
        }
    }

    private Creative resolveCreative(Long creativeId) {
        if (creativeId == null) {
            return null;
        }
        return creativeRepository.findById(creativeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creative not found"));
    }

    private Angle resolveAngle(Long angleId) {
        if (angleId == null) {
            return null;
        }
        return angleRepository.findById(angleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Angle not found"));
    }

    private VisualProof resolveVisualProof(Long visualProofId) {
        if (visualProofId == null) {
            return null;
        }
        return visualProofRepository.findById(visualProofId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visual proof not found"));
    }

    private EmotionalTrigger resolveEmotionalTrigger(Long emotionalTriggerId) {
        if (emotionalTriggerId == null) {
            return null;
        }
        return emotionalTriggerRepository.findById(emotionalTriggerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emotional trigger not found"));
    }
}
