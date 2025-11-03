package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.CreateLeadPortalFlowRequest;
import com.marketinghub.leadportal.dto.LeadPortalFlowDto;
import com.marketinghub.leadportal.dto.UpdateLeadPortalFlowRequest;
import com.marketinghub.leadportal.mapper.LeadPortalFlowMapper;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for managing lead portal flows.
 */
@RestController
@RequestMapping("/api/lead-portal-flows")
public class LeadPortalFlowController {
    private final LeadPortalFlowService service;
    private final LeadPortalFlowMapper mapper;

    public LeadPortalFlowController(LeadPortalFlowService service, LeadPortalFlowMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<LeadPortalFlowDto> list() {
        return service.listAll().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public LeadPortalFlowDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PostMapping
    public LeadPortalFlowDto create(@RequestBody CreateLeadPortalFlowRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public LeadPortalFlowDto update(@PathVariable Long id, @RequestBody UpdateLeadPortalFlowRequest request) {
        return mapper.toDto(service.update(id, request));
    }
}
