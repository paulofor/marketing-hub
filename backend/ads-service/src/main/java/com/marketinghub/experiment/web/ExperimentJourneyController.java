package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.ExperimentJourneyAssignmentsResponse;
import com.marketinghub.experiment.service.ExperimentJourneyService;
import com.marketinghub.journey.mapper.JourneyMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints orchestrating journeys attached to experiments.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/journey")
public class ExperimentJourneyController {
    private final ExperimentJourneyService journeyService;
    private final JourneyMapper journeyMapper;

    public ExperimentJourneyController(ExperimentJourneyService journeyService, JourneyMapper journeyMapper) {
        this.journeyService = journeyService;
        this.journeyMapper = journeyMapper;
    }

    @GetMapping("/assignments")
    public ExperimentJourneyAssignmentsResponse getAssignments(@PathVariable Long experimentId) {
        return journeyService.findCurrent(experimentId)
                .map(result -> new ExperimentJourneyAssignmentsResponse(
                        result.journey().getId(),
                        result.templateId(),
                        result.assignments().stream().map(journeyMapper::toAssignmentResponse).toList()
                ))
                .orElseGet(() -> new ExperimentJourneyAssignmentsResponse(null, null, java.util.List.of()));
    }

    @PostMapping("/rebuild")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public ExperimentJourneyAssignmentsResponse rebuild(@PathVariable Long experimentId) {
        var result = journeyService.rebuild(experimentId);
        return new ExperimentJourneyAssignmentsResponse(
                result.journey().getId(),
                result.templateId(),
                result.assignments().stream().map(journeyMapper::toAssignmentResponse).toList()
        );
    }
}
