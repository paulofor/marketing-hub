package com.marketinghub.experiment.learning.web;

import com.marketinghub.experiment.learning.dto.CreateExperimentLearningRequest;
import com.marketinghub.experiment.learning.dto.ExperimentLearningRequestDto;
import com.marketinghub.experiment.learning.mapper.ExperimentLearningRequestMapper;
import com.marketinghub.experiment.learning.service.ExperimentLearningRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints utilizados pela UI para acompanhar as solicitações vinculadas a um experimento.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/learning-requests")
public class ExperimentLearningRequestController {

    private final ExperimentLearningRequestService service;
    private final ExperimentLearningRequestMapper mapper;

    public ExperimentLearningRequestController(ExperimentLearningRequestService service,
                                               ExperimentLearningRequestMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ExperimentLearningRequestDto> list(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @PostMapping
    public ExperimentLearningRequestDto create(@PathVariable Long experimentId,
                                               @Valid @RequestBody(required = false) CreateExperimentLearningRequest request) {
        return mapper.toDto(service.create(experimentId, request));
    }
}
