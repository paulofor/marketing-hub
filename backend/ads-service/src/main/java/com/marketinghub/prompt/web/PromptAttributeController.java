package com.marketinghub.prompt.web;

import com.marketinghub.prompt.dto.CreatePromptAttributeRequest;
import com.marketinghub.prompt.dto.PromptAttributeDto;
import com.marketinghub.prompt.dto.UpdatePromptAttributeRequest;
import com.marketinghub.prompt.service.PromptAttributeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompt-entities/{entityName}/attributes")
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

    @GetMapping("/{attrName}")
    public PromptAttributeDto get(@PathVariable String entityName, @PathVariable String attrName) {
        return service.getLatest(entityName, attrName);
    }

    @PutMapping("/{attrName}")
    public PromptAttributeDto update(@PathVariable String entityName, @PathVariable String attrName,
                                     @RequestBody UpdatePromptAttributeRequest req) {
        return service.update(entityName, attrName, req);
    }

    @DeleteMapping("/{attrName}")
    public void delete(@PathVariable String entityName, @PathVariable String attrName) {
        service.delete(entityName, attrName);
    }
}
