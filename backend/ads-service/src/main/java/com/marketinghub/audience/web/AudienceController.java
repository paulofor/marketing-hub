package com.marketinghub.audience.web;

import com.marketinghub.audience.dto.AudienceDto;
import com.marketinghub.audience.dto.CreateAudienceRequest;
import com.marketinghub.audience.dto.UpdateAudienceApprovalRequest;
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

    @GetMapping("/audiences/{id}")
    public AudienceDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @GetMapping("/audiences")
    public List<AudienceDto> list() {
        return StreamSupport.stream(service.list().spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }

    @GetMapping("/niches/{nicheId}/audiences")
    public List<AudienceDto> listByNiche(@PathVariable Long nicheId) {
        return StreamSupport.stream(service.listByMarketNiche(nicheId).spliterator(), false)
                .map(mapper::toDto)
                .toList();
    }
}
