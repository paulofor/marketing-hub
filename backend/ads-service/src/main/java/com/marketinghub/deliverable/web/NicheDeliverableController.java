package com.marketinghub.deliverable.web;

import com.marketinghub.deliverable.dto.CreateDeliverableRequest;
import com.marketinghub.deliverable.dto.DeliverableDto;
import com.marketinghub.deliverable.mapper.DeliverableMapper;
import com.marketinghub.deliverable.service.DeliverableService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nested routes for deliverables under a market niche.
 */
@RestController
@RequestMapping("/api/niches/{nicheId}/deliverables")
public class NicheDeliverableController {
    private final DeliverableService service;
    private final DeliverableMapper mapper;

    public NicheDeliverableController(DeliverableService service, DeliverableMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<DeliverableDto> list(@PathVariable Long nicheId) {
        return service.listByNiche(nicheId).stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public DeliverableDto create(@PathVariable Long nicheId, @RequestBody CreateDeliverableRequest request) {
        request.setMarketNicheId(nicheId);
        return mapper.toDto(service.create(request));
    }
}
