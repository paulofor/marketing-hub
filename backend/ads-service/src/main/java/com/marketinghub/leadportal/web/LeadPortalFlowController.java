package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.dto.CreateLeadPortalFlowRequest;
import com.marketinghub.leadportal.dto.LeadPortalFlowDto;
import com.marketinghub.leadportal.dto.UpdateLeadPortalFlowApprovalRequest;
import com.marketinghub.leadportal.dto.UpdateLeadPortalFlowRequest;
import com.marketinghub.leadportal.integration.LeadPortalIntegrationProperties;
import com.marketinghub.leadportal.mapper.LeadPortalFlowMapper;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
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
    private final LeadPortalIntegrationProperties integrationProperties;

    public LeadPortalFlowController(LeadPortalFlowService service,
                                    LeadPortalFlowMapper mapper,
                                    LeadPortalIntegrationProperties integrationProperties) {
        this.service = service;
        this.mapper = mapper;
        this.integrationProperties = integrationProperties;
    }

    @GetMapping
    public List<LeadPortalFlowDto> list(@RequestParam(value = "experimentId", required = false) Long experimentId) {
        List<LeadPortalFlow> flows = experimentId == null ? service.listAll() : service.listByExperiment(experimentId);
        return flows.stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public LeadPortalFlowDto get(@PathVariable Long id) {
        return toDto(service.get(id));
    }

    @PostMapping
    public LeadPortalFlowDto create(@RequestBody CreateLeadPortalFlowRequest request) {
        return toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public LeadPortalFlowDto update(@PathVariable Long id, @RequestBody UpdateLeadPortalFlowRequest request) {
        return toDto(service.update(id, request));
    }

    @PatchMapping("/{id}/approval")
    public LeadPortalFlowDto updateApproval(@PathVariable Long id,
                                            @RequestBody UpdateLeadPortalFlowApprovalRequest request) {
        return toDto(service.updateApproval(id, request.isApproved()));
    }

    private LeadPortalFlowDto toDto(LeadPortalFlow flow) {
        LeadPortalFlowDto dto = mapper.toDto(flow);
        dto.setPublicUrl(resolvePublicUrl(flow));
        return dto;
    }

    private String resolvePublicUrl(LeadPortalFlow flow) {
        if (!flow.isApproved()) {
            return null;
        }
        if (!integrationProperties.isEnabled()) {
            return null;
        }
        if (!StringUtils.hasText(integrationProperties.getBaseUrl())) {
            return null;
        }
        return UriComponentsBuilder.fromHttpUrl(integrationProperties.getBaseUrl())
                .path("/flows/{slug}")
                .buildAndExpand(flow.getSlug())
                .toUriString();
    }
}
