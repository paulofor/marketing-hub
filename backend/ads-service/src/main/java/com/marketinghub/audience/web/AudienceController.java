package com.marketinghub.audience.web;

import com.marketinghub.audience.dto.AudienceDto;
import com.marketinghub.audience.dto.CreateAudienceRequest;
import com.marketinghub.audience.dto.UpdateAudienceApprovalRequest;
import com.marketinghub.audience.dto.UpdateAudienceTargetingRequest;
import com.marketinghub.audience.mapper.AudienceMapper;
import com.marketinghub.audience.service.AudienceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * REST controller for audiences.
 */
@RestController
@RequestMapping("/api")
public class AudienceController {
    private final AudienceService service;
    private final AudienceMapper mapper;

    public AudienceController(AudienceService service, AudienceMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/audiences")
    public AudienceDto create(@RequestBody CreateAudienceRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PatchMapping("/audiences/{id}/approval")
    public AudienceDto updateApproval(@PathVariable Long id,
                                      @RequestBody UpdateAudienceApprovalRequest request) {
        return mapper.toDto(service.updateApproval(id, request.isApproved()));
    }

    @PatchMapping("/audiences/{id}/targeting")
    public AudienceDto updateTargeting(@PathVariable Long id,
                                       @RequestBody UpdateAudienceTargetingRequest request,
                                       @RequestParam(value = "includeTargeting", defaultValue = "true") boolean includeTargeting) {
        AudienceDto dto = mapper.toDto(service.updateTargeting(id, request));
        return maybeStripTargeting(dto, includeTargeting);
    }

    @PostMapping("/audiences/{id}/targeting-seeds/reprocess")
    public AudienceDto reprocessSeeds(@PathVariable Long id,
                                      @RequestParam(value = "includeTargeting", defaultValue = "false") boolean includeTargeting) {
        AudienceDto dto = mapper.toDto(service.markSeedsForReprocess(id));
        return maybeStripTargeting(dto, includeTargeting);
    }

    @GetMapping("/audiences/{id}")
    public AudienceDto get(@PathVariable Long id,
                           @RequestParam(value = "includeTargeting", defaultValue = "false") boolean includeTargeting) {
        AudienceDto dto = mapper.toDto(service.get(id));
        return maybeStripTargeting(dto, includeTargeting);
    }

    @GetMapping("/audiences")
    public List<AudienceDto> list(@RequestParam(value = "includeTargeting", defaultValue = "false") boolean includeTargeting) {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .map(dto -> maybeStripTargeting(dto, includeTargeting))
                .toList();
    }

    @GetMapping("/niches/{nicheId}/audiences")
    public List<AudienceDto> listByNiche(@PathVariable Long nicheId,
                                         @RequestParam(value = "includeTargeting", defaultValue = "false") boolean includeTargeting) {
        return StreamSupport.stream(service.listByMarketNiche(nicheId).spliterator(), false)
                .map(mapper::toDto)
                .map(dto -> maybeStripTargeting(dto, includeTargeting))
                .toList();
    }

    private AudienceDto maybeStripTargeting(AudienceDto dto, boolean includeTargeting) {
        if (!includeTargeting && dto != null) {
            dto.setTargetingSpec(null);
            dto.setSeeds(null);
        }
        return dto;
    }
}
