package com.marketinghub.experiment.web;

import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.ExperimentDiagnosticsDto;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.UpdateSelectedSampleEmailRequest;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.service.ExperimentDiagnosticsService;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for experiments.
 */
@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {
    private final ExperimentService service;
    private final ExperimentMapper mapper;
    private final ExperimentDiagnosticsService diagnosticsService;
    private final ExperimentReadinessService readinessService;

    public ExperimentController(ExperimentService service, ExperimentMapper mapper,
                                ExperimentDiagnosticsService diagnosticsService,
                                ExperimentReadinessService readinessService) {
        this.service = service;
        this.mapper = mapper;
        this.diagnosticsService = diagnosticsService;
        this.readinessService = readinessService;
    }

    @PostMapping
    public ExperimentDto create(@RequestBody CreateExperimentRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PostMapping("/{id}/duplicate")
    public ExperimentDto duplicate(@PathVariable Long id) {
        return mapper.toDto(service.duplicate(id));
    }

    @GetMapping("/{id}")
    public ExperimentDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping("/{id}/diagnostics")
    public ExperimentDiagnosticsDto diagnostics(@PathVariable Long id) {
        return diagnosticsService.diagnose(id);
    }

    @GetMapping("/{id}/readiness")
    public ExperimentReadinessSummaryDto readiness(@PathVariable Long id) {
        return readinessService.summarize(id);
    }

    @GetMapping
    public List<ExperimentDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Atualiza apenas o status do experimento.
     */
    @PatchMapping("/{id}/status")
    public ExperimentDto updateStatus(
            @PathVariable Long id,
            @RequestParam com.marketinghub.experiment.ExperimentStatus status) {
        return mapper.toDto(service.updateStatus(id, status));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ExperimentDto update(@PathVariable Long id, @RequestBody UpdateExperimentRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @PatchMapping("/{id}/creatives-to-generate")
    public ExperimentDto requestCreatives(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestCreatives(id, quantity));
    }

    @PatchMapping("/{id}/instant-forms-to-generate")
    public ExperimentDto requestInstantForms(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestInstantForms(id, quantity));
    }

    @PatchMapping("/{id}/emails-to-generate")
    public ExperimentDto requestEmails(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestEmails(id, quantity));
    }

    @PatchMapping("/{id}/sample-emails-to-generate")
    public ExperimentDto requestSampleEmails(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestSampleEmails(id, quantity));
    }
    @PutMapping("/{id}/selected-sample-email")
    public ExperimentDto updateSelectedSampleEmail(
            @PathVariable Long id,
            @RequestBody UpdateSelectedSampleEmailRequest request) {
        return mapper.toDto(service.updateSelectedSampleEmail(id, request.sampleEmailId()));
    }

    @PatchMapping("/{id}/deliverables-to-generate")
    public ExperimentDto requestDeliverables(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestDeliverables(id, quantity));
    }

    @PatchMapping("/{id}/lead-portal-flows-to-generate")
    public ExperimentDto requestLeadPortalFlows(@PathVariable Long id, @RequestParam("quantity") int quantity) {
        return mapper.toDto(service.requestLeadPortalFlows(id, quantity));
    }

    @PostMapping("/{id}/facebook-release")
    public ExperimentDto releaseForFacebook(@PathVariable Long id) {
        return mapper.toDto(service.releaseForFacebook(id));
    }

}
