package com.marketinghub.appidea.web;

import com.marketinghub.appidea.dto.AppIdeaDto;
import com.marketinghub.appidea.dto.CreateAppIdeaRequest;
import com.marketinghub.appidea.mapper.AppIdeaMapper;
import com.marketinghub.appidea.service.AppIdeaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nested routes for managing application ideas under a specific market niche.
 */
@RestController
@RequestMapping("/api/niches/{nicheId}/app-ideas")
public class NicheAppIdeaController {
    private final AppIdeaService service;
    private final AppIdeaMapper mapper;

    public NicheAppIdeaController(AppIdeaService service, AppIdeaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<AppIdeaDto> list(@PathVariable Long nicheId) {
        return service.listAppIdeasByNiche(nicheId).stream().map(mapper::toDto).toList();
    }

    @PostMapping
    public AppIdeaDto create(@PathVariable Long nicheId, @RequestBody CreateAppIdeaRequest request) {
        request.setMarketNicheId(nicheId);
        return mapper.toDto(service.createAppIdea(request));
    }
}
