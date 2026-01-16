package com.marketinghub.prompt.web;

import com.marketinghub.prompt.dto.CreatePromptDomainRequest;
import com.marketinghub.prompt.dto.PromptDomainDto;
import com.marketinghub.prompt.dto.PromptDomainObjectDto;
import com.marketinghub.prompt.dto.UpdatePromptDomainRequest;
import com.marketinghub.prompt.service.PromptDomainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompt-domains")
public class PromptDomainController {
    private final PromptDomainService service;

    public PromptDomainController(PromptDomainService service) {
        this.service = service;
    }

    @GetMapping
    public List<PromptDomainDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public PromptDomainDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public PromptDomainDto create(@RequestBody CreatePromptDomainRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PromptDomainDto update(@PathVariable Long id, @RequestBody UpdatePromptDomainRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/available-objects")
    public List<PromptDomainObjectDto> availableObjects() {
        return service.listAvailableObjects();
    }
}
