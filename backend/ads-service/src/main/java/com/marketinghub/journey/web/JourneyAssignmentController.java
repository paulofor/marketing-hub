package com.marketinghub.journey.web;

import com.marketinghub.journey.dto.JourneyAssignmentRequest;
import com.marketinghub.journey.dto.JourneyAssignmentResponse;
import com.marketinghub.journey.mapper.JourneyMapper;
import com.marketinghub.journey.model.JourneyAssignmentStatus;
import com.marketinghub.journey.service.JourneyAssignmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST surface for journey assignments.
 */
@RestController
@RequestMapping("/api")
public class JourneyAssignmentController {
    private final JourneyAssignmentService assignmentService;
    private final JourneyMapper mapper;

    public JourneyAssignmentController(JourneyAssignmentService assignmentService, JourneyMapper mapper) {
        this.assignmentService = assignmentService;
        this.mapper = mapper;
    }

    @GetMapping("/journeys/{journeyId}/assignments")
    public Page<JourneyAssignmentResponse> list(@PathVariable Long journeyId,
                                                @RequestParam(value = "status", required = false) JourneyAssignmentStatus status,
                                                @PageableDefault(size = 50) Pageable pageable) {
        return assignmentService.list(journeyId, status, pageable).map(mapper::toAssignmentResponse);
    }

    @PostMapping("/journeys/{journeyId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public List<JourneyAssignmentResponse> assign(@PathVariable Long journeyId,
                                                  @Valid @RequestBody JourneyAssignmentRequest request) {
        return assignmentService.assign(journeyId, request).stream()
                .map(mapper::toAssignmentResponse)
                .toList();
    }

    @DeleteMapping("/journey-assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long assignmentId) {
        assignmentService.delete(assignmentId);
    }
}
