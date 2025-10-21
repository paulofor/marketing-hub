package com.marketinghub.deliverable.web;

import com.marketinghub.deliverable.dto.CreateDeliverablePackageRequest;
import com.marketinghub.deliverable.dto.DeliverablePackageDto;
import com.marketinghub.deliverable.mapper.DeliverablePackageMapper;
import com.marketinghub.deliverable.service.DeliverablePackageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nested endpoints to manage packages under a specific experiment.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/deliverable-packages")
public class ExperimentDeliverablePackageController {
    private final DeliverablePackageService service;
    private final DeliverablePackageMapper mapper;

    public ExperimentDeliverablePackageController(DeliverablePackageService service,
                                                  DeliverablePackageMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<DeliverablePackageDto> list(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId).stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public DeliverablePackageDto create(@PathVariable Long experimentId,
                                        @RequestBody CreateDeliverablePackageRequest request) {
        request.setExperimentId(experimentId);
        return mapper.toDto(service.create(request));
    }
}
