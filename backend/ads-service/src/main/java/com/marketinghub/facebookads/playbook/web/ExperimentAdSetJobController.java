package com.marketinghub.facebookads.playbook.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookads.playbook.ExperimentAdSetJob;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetJobClaimRequest;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetJobFailureRequest;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetJobPayloadDto;
import com.marketinghub.facebookads.playbook.dto.ExperimentAdSetJobResultRequest;
import com.marketinghub.facebookads.playbook.service.ExperimentAdSetWorkflowJobCoordinator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal endpoints consumed by the AI Worker and Facebook Ads Worker to process jobs.
 */
@RestController
@RequestMapping("/internal/adset-playbook/jobs")
@Validated
public class ExperimentAdSetJobController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentAdSetJobController.class);

    private final ExperimentAdSetWorkflowJobCoordinator coordinator;
    private final ObjectMapper objectMapper;

    public ExperimentAdSetJobController(ExperimentAdSetWorkflowJobCoordinator coordinator,
                                        ObjectMapper objectMapper) {
        this.coordinator = coordinator;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/claim")
    public List<ExperimentAdSetJobPayloadDto> claimJobs(@Valid @RequestBody ExperimentAdSetJobClaimRequest request) {
        List<ExperimentAdSetJob> jobs = coordinator.claimJobs(request.worker(), request.workerId(), request.limit());
        return jobs.stream()
                .map(this::toPayload)
                .collect(Collectors.toList());
    }

    @PostMapping("/{jobId}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void completeJob(@PathVariable Long jobId,
                            @Valid @RequestBody ExperimentAdSetJobResultRequest request) {
        coordinator.completeJob(jobId, request.result(), request.apiCalls());
    }

    @PostMapping("/{jobId}/fail")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void failJob(@PathVariable Long jobId,
                        @Valid @RequestBody ExperimentAdSetJobFailureRequest request) {
        coordinator.failJob(jobId, request.errorMessage(), request.apiCalls());
    }

    private ExperimentAdSetJobPayloadDto toPayload(ExperimentAdSetJob job) {
        JsonNode payloadNode = readPayload(job.getPayload());
        return new ExperimentAdSetJobPayloadDto(
                job.getId(),
                job.getType(),
                job.getWorker(),
                job.getWorkflow() != null ? job.getWorkflow().getId() : null,
                job.getResourceId(),
                payloadNode,
                job.getCreatedAt()
        );
    }

    private JsonNode readPayload(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            LOGGER.error("Falha ao desserializar payload do job: {}", ex.getMessage());
            return null;
        }
    }
}
