package com.marketinghub.agent.web;

import com.marketinghub.agent.dto.AgentDto;
import com.marketinghub.agent.dto.SaveAgentRequest;
import com.marketinghub.agent.mapper.AgentMapper;
import com.marketinghub.agent.service.AgentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService service;
    private final AgentMapper mapper;

    public AgentController(AgentService service, AgentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public AgentDto create(@RequestBody SaveAgentRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public AgentDto update(@PathVariable Long id, @RequestBody SaveAgentRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @GetMapping
    public List<AgentDto> list() {
        return service.list().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public AgentDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }
}
