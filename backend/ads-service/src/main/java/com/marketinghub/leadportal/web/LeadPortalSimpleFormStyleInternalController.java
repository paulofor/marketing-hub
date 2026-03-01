package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.dto.LeadPortalSimpleFormStyleDto;
import com.marketinghub.leadportal.dto.LeadPortalSimpleFormStyleGenerationResultRequest;
import com.marketinghub.leadportal.service.LeadPortalSimpleFormStyleService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/lead-portal/simple-form-styles")
public class LeadPortalSimpleFormStyleInternalController {

    private final LeadPortalSimpleFormStyleService service;

    public LeadPortalSimpleFormStyleInternalController(LeadPortalSimpleFormStyleService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<LeadPortalSimpleFormStyleDto> listPending(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        int safeLimit = limit != null ? limit : 10;
        return service.listPendingForGeneration(safeLimit).stream().map(this::toDto).toList();
    }

    @PatchMapping("/{id}/generation")
    public LeadPortalSimpleFormStyleDto saveGeneration(
            @PathVariable Long id,
            @RequestBody LeadPortalSimpleFormStyleGenerationResultRequest request) {
        return toDto(service.saveGenerationResult(id, request));
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
        dto.setGenerationCostUsd(style.getGenerationCostUsd());
        dto.setGenerationStatus(style.getGenerationStatus());
        dto.setGenerationError(style.getGenerationError());
        dto.setCreatedAt(style.getCreatedAt());
        dto.setUpdatedAt(style.getUpdatedAt());
        return dto;
    }
}
