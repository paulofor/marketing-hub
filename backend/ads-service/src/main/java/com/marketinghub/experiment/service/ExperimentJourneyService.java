package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.journey.model.*;
import com.marketinghub.journey.repository.JourneyAssignmentRepository;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates the lifecycle of journeys associated with experiments.
 */
@Service
public class ExperimentJourneyService {
    private final ExperimentRepository experimentRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyAssignmentRepository assignmentRepository;
    private final JourneyStepRepository stepRepository;

    public ExperimentJourneyService(ExperimentRepository experimentRepository,
                                    JourneyRepository journeyRepository,
                                    JourneyAssignmentRepository assignmentRepository,
                                    JourneyStepRepository stepRepository) {
        this.experimentRepository = experimentRepository;
        this.journeyRepository = journeyRepository;
        this.assignmentRepository = assignmentRepository;
        this.stepRepository = stepRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Result> findCurrent(Long experimentId) {
        return journeyRepository.findFirstByExperimentIdOrderByCreatedAtDesc(experimentId)
                .map(journey -> {
                    List<JourneyAssignment> assignments = assignmentRepository.findAllByJourneyId(journey.getId());
                    assignments.sort(byStepPosition());
                    return new Result(journey, assignments);
                });
    }

    @Transactional
    public Result rebuild(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found"));
        JourneyTemplate template = experiment.getJourneyTemplate();
        if (template == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Experiment is not linked to a journey template");
        }

        List<Journey> existing = journeyRepository.findByExperimentId(experimentId);
        if (!existing.isEmpty()) {
            journeyRepository.deleteAll(existing);
            journeyRepository.flush();
        }

        Journey journey = Journey.builder()
                .template(template)
                .name(experiment.getName())
                .description(experiment.getHypothesis())
                .status(JourneyStatus.DRAFT)
                .marketNiche(experiment.getNiche())
                .experiment(experiment)
                .metadata(Collections.emptyMap())
                .build();
        Journey savedJourney = journeyRepository.save(journey);

        List<JourneyStep> steps = stepRepository.findByTemplateOrderByPositionAsc(template);
        List<JourneyAssignment> assignments = new ArrayList<>();
        for (JourneyStep step : steps) {
            JourneyAssignment assignment = JourneyAssignment.builder()
                    .journey(savedJourney)
                    .type(JourneyAssignmentType.SEGMENT)
                    .segmentIdentifier(resolveSegmentIdentifier(step))
                    .status(JourneyAssignmentStatus.PENDING)
                    .currentStep(null)
                    .nextStep(step)
                    .build();
            assignments.add(assignment);
        }

        List<JourneyAssignment> persisted = assignments.isEmpty()
                ? List.of()
                : assignmentRepository.saveAll(assignments);
        persisted.sort(byStepPosition());
        return new Result(savedJourney, persisted);
    }

    private Comparator<JourneyAssignment> byStepPosition() {
        return Comparator.comparing(
                (JourneyAssignment assignment) -> assignment.getNextStep() != null ? assignment.getNextStep().getPosition() : Integer.MAX_VALUE,
                Comparator.nullsLast(Integer::compareTo))
                .thenComparing(JourneyAssignment::getId, Comparator.nullsLast(Long::compareTo));
    }

    private String resolveSegmentIdentifier(JourneyStep step) {
        if (step.getName() != null && !step.getName().isBlank()) {
            return step.getName();
        }
        String phase = step.getPhase() != null ? step.getPhase().name() : "STEP";
        Integer position = step.getPosition();
        return position != null ? phase + " " + position : phase;
    }

    public record Result(Journey journey, List<JourneyAssignment> assignments) {
        public Result {
            Objects.requireNonNull(journey, "journey must not be null");
            assignments = assignments != null ? assignments : List.of();
        }

        public Long templateId() {
            return journey.getTemplate() != null ? journey.getTemplate().getId() : null;
        }
    }
}
