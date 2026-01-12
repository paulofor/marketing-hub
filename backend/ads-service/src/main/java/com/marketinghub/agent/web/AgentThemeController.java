package com.marketinghub.agent.web;

import com.marketinghub.agent.dto.AgentThemeDto;
import com.marketinghub.agent.dto.SaveAgentThemeRequest;
import com.marketinghub.agent.mapper.AgentMapper;
import com.marketinghub.agent.service.AgentThemeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-themes")
public class AgentThemeController {

    private final AgentThemeService service;
    private final AgentMapper mapper;

    public AgentThemeController(AgentThemeService service, AgentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public AgentThemeDto create(@RequestBody SaveAgentThemeRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public AgentThemeDto update(@PathVariable Long id, @RequestBody SaveAgentThemeRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @GetMapping
    public List<AgentThemeDto> list() {
        return service.list().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public AgentThemeDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }
}
