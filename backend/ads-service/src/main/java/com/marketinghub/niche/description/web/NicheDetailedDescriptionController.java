package com.marketinghub.niche.description.web;

import com.marketinghub.niche.description.dto.CreateNicheDetailedDescriptionRequest;
import com.marketinghub.niche.description.dto.NicheDetailedDescriptionDto;
import com.marketinghub.niche.description.dto.UpdateNicheDetailedDescriptionStatusRequest;
import com.marketinghub.niche.description.mapper.NicheDetailedDescriptionMapper;
import com.marketinghub.niche.description.service.NicheDetailedDescriptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/niches/{nicheId}/descriptions")
public class NicheDetailedDescriptionController {

    private final NicheDetailedDescriptionService service;
    private final NicheDetailedDescriptionMapper mapper;

    public NicheDetailedDescriptionController(NicheDetailedDescriptionService service,
                                              NicheDetailedDescriptionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<NicheDetailedDescriptionDto> list(@PathVariable Long nicheId) {
        return service.listByNiche(nicheId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    public NicheDetailedDescriptionDto create(@PathVariable Long nicheId,
                                              @RequestBody CreateNicheDetailedDescriptionRequest request) {
        request.setMarketNicheId(nicheId);
        return mapper.toDto(service.create(request));
    }

    @PatchMapping("/{descriptionId}/active")
    public NicheDetailedDescriptionDto updateActive(@PathVariable Long nicheId,
                                                    @PathVariable Long descriptionId,
                                                    @RequestBody UpdateNicheDetailedDescriptionStatusRequest request) {
        return mapper.toDto(service.updateActive(nicheId, descriptionId, request.isActive()));
    }
}
