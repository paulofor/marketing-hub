package com.marketinghub.prompt.web;

import com.marketinghub.prompt.dto.CreatePromptAttributeRequest;
import com.marketinghub.prompt.dto.PromptAttributeDto;
import com.marketinghub.prompt.service.PromptAttributeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prompt-entities/{entityName}/attributes")
public class PromptAttributeController {
    private final PromptAttributeService service;

    public PromptAttributeController(PromptAttributeService service) {
        this.service = service;
    }

    @GetMapping
    public List<PromptAttributeDto> list(@PathVariable String entityName) {
        return service.listLatest(entityName);
    }

    @PostMapping
    public PromptAttributeDto create(@PathVariable String entityName, @RequestBody CreatePromptAttributeRequest req) {
        return service.create(entityName, req);
    }
}
