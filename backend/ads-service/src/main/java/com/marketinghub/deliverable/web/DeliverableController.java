package com.marketinghub.deliverable.web;

import com.marketinghub.deliverable.dto.CreateDeliverableRequest;
import com.marketinghub.deliverable.dto.DeliverableDto;
import com.marketinghub.deliverable.dto.UpdateDeliverableRequest;
import com.marketinghub.deliverable.mapper.DeliverableMapper;
import com.marketinghub.deliverable.service.DeliverableService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for managing deliverables.
 */
@RestController
@RequestMapping("/api/deliverables")
public class DeliverableController {
    private final DeliverableService service;
    private final DeliverableMapper mapper;

    public DeliverableController(DeliverableService service, DeliverableMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<DeliverableDto> list() {
        return service.listAll().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public DeliverableDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PostMapping
    public DeliverableDto create(@RequestBody CreateDeliverableRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public DeliverableDto update(@PathVariable Long id, @RequestBody UpdateDeliverableRequest request) {
        return mapper.toDto(service.update(id, request));
    }
}
