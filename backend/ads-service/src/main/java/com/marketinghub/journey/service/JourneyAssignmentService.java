package com.marketinghub.journey.service;

import com.marketinghub.journey.dto.JourneyAssignmentRequest;
import com.marketinghub.journey.model.*;
import com.marketinghub.journey.repository.JourneyAssignmentRepository;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import com.marketinghub.model.Lead;
import com.marketinghub.repository.LeadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for linking leads or segments to journeys.
 */
@Service
public class JourneyAssignmentService {
    private final JourneyRepository journeyRepository;
    private final JourneyAssignmentRepository assignmentRepository;
    private final JourneyStepRepository stepRepository;
    private final LeadRepository leadRepository;

    public JourneyAssignmentService(JourneyRepository journeyRepository,
                                    JourneyAssignmentRepository assignmentRepository,
                                    JourneyStepRepository stepRepository,
                                    LeadRepository leadRepository) {
        this.journeyRepository = journeyRepository;
        this.assignmentRepository = assignmentRepository;
        this.stepRepository = stepRepository;
        this.leadRepository = leadRepository;
    }

    @Transactional(readOnly = true)
    public Page<JourneyAssignment> list(Long journeyId, JourneyAssignmentStatus status, Pageable pageable) {
        verifyJourneyExists(journeyId);
        if (status != null) {
            return assignmentRepository.findByJourneyIdAndStatus(journeyId, status, pageable);
        }
        return assignmentRepository.findByJourneyId(journeyId, pageable);
    }

    @Transactional
    public List<JourneyAssignment> assign(Long journeyId, JourneyAssignmentRequest request) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey not found"));

        JourneyStep currentStep = resolveStep(journey.getTemplate(), request.currentStepId());
        JourneyStep nextStep = resolveNextStep(journey.getTemplate(), request.nextStepId());
        JourneyAssignmentStatus status = request.status() != null ? request.status() : JourneyAssignmentStatus.PENDING;
        String contextPayload = request.contextPayload();

        List<JourneyAssignment> assignments = new ArrayList<>();

        if (request.leadIds() != null && !request.leadIds().isEmpty()) {
            assignments.addAll(createLeadAssignments(journey, request.leadIds(), status, currentStep, nextStep, contextPayload));
        }

        if (request.segmentIdentifiers() != null && !request.segmentIdentifiers().isEmpty()) {
            for (String identifier : request.segmentIdentifiers()) {
                JourneyAssignment assignment = JourneyAssignment.builder()
                        .journey(journey)
                        .type(JourneyAssignmentType.SEGMENT)
                        .segmentIdentifier(identifier)
                        .status(status)
                        .currentStep(currentStep)
                        .nextStep(nextStep)
                        .contextPayload(contextPayload)
                        .build();
                assignments.add(assignment);
            }
        }

        if (assignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one leadId or segmentIdentifier must be provided");
        }

        journey.getAssignments().addAll(assignments);
        return assignmentRepository.saveAll(assignments);
    }

    @Transactional
    public void delete(Long assignmentId) {
        JourneyAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey assignment not found"));
        assignmentRepository.delete(assignment);
    }

    private List<JourneyAssignment> createLeadAssignments(Journey journey,
                                                          List<UUID> leadIds,
                                                          JourneyAssignmentStatus status,
                                                          JourneyStep currentStep,
                                                          JourneyStep nextStep,
                                                          String contextPayload) {
        List<Lead> leads = leadRepository.findAllById(leadIds);
        Set<UUID> retrievedIds = leads.stream().map(Lead::getId).collect(Collectors.toSet());
        for (UUID requestedId : leadIds) {
            if (!retrievedIds.contains(requestedId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + requestedId);
            }
        }
        List<JourneyAssignment> assignments = new ArrayList<>();
        for (Lead lead : leads) {
            JourneyAssignment assignment = JourneyAssignment.builder()
                    .journey(journey)
                    .type(JourneyAssignmentType.LEAD)
                    .lead(lead)
                    .status(status)
                    .currentStep(currentStep)
                    .nextStep(nextStep)
                    .contextPayload(contextPayload)
                    .build();
            assignments.add(assignment);
        }
        return assignments;
    }

    private JourneyStep resolveStep(JourneyTemplate template, Long stepId) {
        if (stepId == null) {
            return null;
        }
        JourneyStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey step not found"));
        if (!step.getTemplate().getId().equals(template.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step does not belong to template");
        }
        return step;
    }

    private JourneyStep resolveNextStep(JourneyTemplate template, Long stepId) {
        JourneyStep explicitStep = resolveStep(template, stepId);
        if (explicitStep != null) {
            return explicitStep;
        }
        return stepRepository.findByTemplateOrderByPositionAsc(template).stream()
                .findFirst()
                .orElse(null);
    }

    private void verifyJourneyExists(Long journeyId) {
        if (!journeyRepository.existsById(journeyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Journey not found");
        }
    }
}
