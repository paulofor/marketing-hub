package com.marketinghub.imagedeliverable.web;

import com.marketinghub.imagedeliverable.dto.ImageDeliverablePackageDto;
import com.marketinghub.imagedeliverable.mapper.ImageDeliverablePackageMapper;
import com.marketinghub.imagedeliverable.service.ImageDeliverablePackageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Nested endpoints to list image deliverables for a lead.
 */
@RestController
@RequestMapping("/api/leads/{leadId}/image-deliverable-packages")
public class LeadImageDeliverablePackageController {
    private final ImageDeliverablePackageService service;
    private final ImageDeliverablePackageMapper mapper;

    public LeadImageDeliverablePackageController(ImageDeliverablePackageService service,
                                                 ImageDeliverablePackageMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ImageDeliverablePackageDto> list(@PathVariable UUID leadId) {
        return service.listByLead(leadId).stream().map(mapper::toDto).toList();
    }
}
