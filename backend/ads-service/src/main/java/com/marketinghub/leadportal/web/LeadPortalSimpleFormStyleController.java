package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.dto.CreateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.dto.LeadPortalSimpleFormStyleDto;
import com.marketinghub.leadportal.dto.UpdateLeadPortalSimpleFormStyleRequest;
import com.marketinghub.leadportal.service.LeadPortalSimpleFormStyleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lead-portal/simple-form-styles")
public class LeadPortalSimpleFormStyleController {

    private final LeadPortalSimpleFormStyleService service;

    public LeadPortalSimpleFormStyleController(LeadPortalSimpleFormStyleService service) {
        this.service = service;
    }

    @GetMapping
    public List<LeadPortalSimpleFormStyleDto> list() {
        return service.listAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public LeadPortalSimpleFormStyleDto get(@PathVariable Long id) {
        return toDto(service.get(id));
    }

    @PostMapping
    public LeadPortalSimpleFormStyleDto create(@Valid @RequestBody CreateLeadPortalSimpleFormStyleRequest request) {
        return toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public LeadPortalSimpleFormStyleDto update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateLeadPortalSimpleFormStyleRequest request) {
        return toDto(service.update(id, request));
    }

    private LeadPortalSimpleFormStyleDto toDto(LeadPortalSimpleFormStyle style) {
        LeadPortalSimpleFormStyleDto dto = new LeadPortalSimpleFormStyleDto();
        dto.setId(style.getId());
        dto.setName(style.getName());
        dto.setSlug(style.getSlug());
        dto.setDescription(style.getDescription());
        dto.setTextModel(style.getTextModel());
        dto.setTextPrompt(style.getTextPrompt());
        dto.setTextParameters(style.getTextParameters());
        dto.setImageModel(style.getImageModel());
        dto.setImagePrompt(style.getImagePrompt());
        dto.setImageNegativePrompt(style.getImageNegativePrompt());
        dto.setImageParameters(style.getImageParameters());
        dto.setImageBatchSize(style.getImageBatchSize());
        dto.setImageAspectRatio(style.getImageAspectRatio());
        dto.setPreviewImageUrl(style.getPreviewImageUrl());
        dto.setDefinition(style.getDefinition());
        dto.setCreatedAt(style.getCreatedAt());
        dto.setUpdatedAt(style.getUpdatedAt());
        return dto;
    }
}
