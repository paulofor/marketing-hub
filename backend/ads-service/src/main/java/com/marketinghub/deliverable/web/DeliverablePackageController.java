package com.marketinghub.deliverable.web;

import com.marketinghub.deliverable.dto.CreateDeliverablePackageRequest;
import com.marketinghub.deliverable.dto.DeliverablePackageDto;
import com.marketinghub.deliverable.dto.UpdateDeliverablePackageRequest;
import com.marketinghub.deliverable.mapper.DeliverablePackageMapper;
import com.marketinghub.deliverable.service.DeliverablePackageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for deliverable packages.
 */
@RestController
@RequestMapping("/api/deliverable-packages")
public class DeliverablePackageController {
    private final DeliverablePackageService service;
    private final DeliverablePackageMapper mapper;

    public DeliverablePackageController(DeliverablePackageService service, DeliverablePackageMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<DeliverablePackageDto> list() {
        return service.listAll().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public DeliverablePackageDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PostMapping
    public DeliverablePackageDto create(@RequestBody CreateDeliverablePackageRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public DeliverablePackageDto update(@PathVariable Long id, @RequestBody UpdateDeliverablePackageRequest request) {
        return mapper.toDto(service.update(id, request));
    }
}
