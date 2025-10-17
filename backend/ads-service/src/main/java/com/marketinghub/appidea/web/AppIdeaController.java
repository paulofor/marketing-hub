package com.marketinghub.appidea.web;

import com.marketinghub.appidea.dto.AppIdeaDto;
import com.marketinghub.appidea.dto.CreateAppIdeaRequest;
import com.marketinghub.appidea.mapper.AppIdeaMapper;
import com.marketinghub.appidea.service.AppIdeaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing endpoints to manage application ideas.
 */
@RestController
@RequestMapping("/api/app-ideas")
public class AppIdeaController {
    private final AppIdeaService service;
    private final AppIdeaMapper mapper;

    public AppIdeaController(AppIdeaService service, AppIdeaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public AppIdeaDto create(@RequestBody CreateAppIdeaRequest request) {
        return mapper.toDto(service.createAppIdea(request));
    }

    @GetMapping
    public List<AppIdeaDto> list() {
        return service.listAppIdeas().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public AppIdeaDto get(@PathVariable Long id) {
        return mapper.toDto(service.getAppIdea(id));
    }
}
