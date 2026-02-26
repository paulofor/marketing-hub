package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.dto.CreateLeadPortalFlowRequest;
import com.marketinghub.leadportal.dto.LeadPortalFlowDto;
import com.marketinghub.leadportal.dto.UpdateLeadPortalFlowApprovalRequest;
import com.marketinghub.leadportal.dto.UpdateLeadPortalFlowRequest;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
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
    private final LeadPortalPublicUrlResolver publicUrlResolver;

    public LeadPortalFlowController(LeadPortalFlowService service,
                                    LeadPortalFlowMapper mapper,
                                    LeadPortalPublicUrlResolver publicUrlResolver) {
        this.service = service;
        this.mapper = mapper;
        this.publicUrlResolver = publicUrlResolver;
    }

    @GetMapping
    public List<LeadPortalFlowDto> list(@RequestParam(value = "experimentId", required = false) Long experimentId,
                                            @RequestParam(value = "nicheId", required = false) Long nicheId) {
        List<LeadPortalFlow> flows;
        if (nicheId != null) {
            flows = service.listByMarketNiche(nicheId);
        } else if (experimentId != null) {
            flows = service.listByExperiment(experimentId);
        } else {
            flows = service.listAll();
        }
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
        return publicUrlResolver.resolve(flow);
    }
}
