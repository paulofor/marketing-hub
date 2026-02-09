package com.marketinghub.facebookads.playbook.web;

import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetWorkflowDto;
import com.marketinghub.facebookads.playbook.dto.StartExperimentAdSetWorkflowRequest;
import com.marketinghub.facebookads.playbook.service.ExperimentAdSetWorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints so the UI can inspect and trigger the playbook workflow.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/adset-playbook")
public class ExperimentAdSetWorkflowController {

    private final ExperimentAdSetWorkflowService workflowService;

    public ExperimentAdSetWorkflowController(ExperimentAdSetWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public ExperimentAdSetWorkflowDto getWorkflow(@PathVariable Long experimentId) {
        return workflowService.getDetails(experimentId);
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExperimentAdSetWorkflowDto startWorkflow(@PathVariable Long experimentId,
                                                     @Validated @RequestBody(required = false)
                                                     StartExperimentAdSetWorkflowRequest request) {
        StartExperimentAdSetWorkflowRequest payload = request != null ? request : new StartExperimentAdSetWorkflowRequest(false);
        return workflowService.start(experimentId, payload);
    }
}
