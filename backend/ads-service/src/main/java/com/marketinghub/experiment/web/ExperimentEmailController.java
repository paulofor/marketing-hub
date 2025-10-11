package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.ExperimentEmailDetailDto;
import com.marketinghub.experiment.dto.UpdateExperimentEmailApprovalRequest;
import com.marketinghub.experiment.service.ExperimentEmailDetailService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints that expose AI generated email metadata for experiments.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/emails")
public class ExperimentEmailController {
    private final ExperimentEmailDetailService service;

    public ExperimentEmailController(ExperimentEmailDetailService service) {
        this.service = service;
    }

    @GetMapping("/{stepId}")
    public ExperimentEmailDetailDto get(@PathVariable Long experimentId, @PathVariable Long stepId) {
        return service.get(experimentId, stepId);
    }

    @PatchMapping("/{stepId}/approval")
    public ExperimentEmailDetailDto updateApproval(@PathVariable Long experimentId,
                                                    @PathVariable Long stepId,
                                                    @RequestBody UpdateExperimentEmailApprovalRequest request) {
        return service.updateApproval(experimentId, stepId, request.approved());
    }

    @DeleteMapping("/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long experimentId, @PathVariable Long stepId) {
        service.delete(experimentId, stepId);
    }
}
