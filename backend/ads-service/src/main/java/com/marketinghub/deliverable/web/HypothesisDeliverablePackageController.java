package com.marketinghub.deliverable.web;

import com.marketinghub.deliverable.dto.CreateDeliverablePackageRequest;
import com.marketinghub.deliverable.dto.DeliverablePackageDto;
import com.marketinghub.deliverable.mapper.DeliverablePackageMapper;
import com.marketinghub.deliverable.service.DeliverablePackageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints to manage deliverable packages linked directly to a hypothesis.
 */
@RestController
@RequestMapping("/api/hypotheses/{hypothesisId}/deliverable-packages")
public class HypothesisDeliverablePackageController {
    private final DeliverablePackageService service;
    private final DeliverablePackageMapper mapper;

    public HypothesisDeliverablePackageController(DeliverablePackageService service,
                                                  DeliverablePackageMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<DeliverablePackageDto> list(@PathVariable UUID hypothesisId) {
        return service.listByHypothesis(hypothesisId).stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public DeliverablePackageDto create(@PathVariable UUID hypothesisId,
                                        @RequestBody CreateDeliverablePackageRequest request) {
        request.setHypothesisId(hypothesisId);
        return mapper.toDto(service.create(request));
    }
}
