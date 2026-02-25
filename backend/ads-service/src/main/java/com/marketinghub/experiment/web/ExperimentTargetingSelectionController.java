package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.ExperimentTargetingSelectionDto;
import com.marketinghub.experiment.dto.SaveExperimentTargetingSelectionsRequest;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/experiments/{experimentId}/targeting-selections")
public class ExperimentTargetingSelectionController {
    private final ExperimentTargetingSelectionService service;

    public ExperimentTargetingSelectionController(ExperimentTargetingSelectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExperimentTargetingSelectionDto> list(@PathVariable Long experimentId) {
        return service.list(experimentId);
    }

    @PutMapping
    public List<ExperimentTargetingSelectionDto> save(@PathVariable Long experimentId,
                                                       @Valid @RequestBody SaveExperimentTargetingSelectionsRequest request) {
        return service.save(experimentId, request);
    }

    @PostMapping("/run-simple-flow")
    public TargetingRequestDto runSimpleFlow(@PathVariable Long experimentId) {
        return service.runSimpleFlow(experimentId);
    }
}
