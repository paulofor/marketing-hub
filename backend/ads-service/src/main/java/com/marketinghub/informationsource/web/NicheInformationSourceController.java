package com.marketinghub.informationsource.web;

import com.marketinghub.informationsource.dto.CreateInformationSourceRequest;
import com.marketinghub.informationsource.dto.InformationSourceDto;
import com.marketinghub.informationsource.mapper.InformationSourceMapper;
import com.marketinghub.informationsource.service.InformationSourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nested routes for information sources under a market niche.
 */
@RestController
@RequestMapping("/api/niches/{nicheId}/information-sources")
public class NicheInformationSourceController {
    private final InformationSourceService service;
    private final InformationSourceMapper mapper;

    public NicheInformationSourceController(InformationSourceService service, InformationSourceMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<InformationSourceDto> list(@PathVariable Long nicheId) {
        return service.listByNiche(nicheId).stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public InformationSourceDto create(@PathVariable Long nicheId, @RequestBody CreateInformationSourceRequest request) {
        request.setMarketNicheId(nicheId);
        return mapper.toDto(service.create(request));
    }
}
