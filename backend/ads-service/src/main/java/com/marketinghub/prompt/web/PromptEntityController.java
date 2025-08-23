package com.marketinghub.prompt.web;

import com.marketinghub.prompt.dto.CreatePromptEntityRequest;
import com.marketinghub.prompt.dto.PromptEntityDto;
import com.marketinghub.prompt.dto.UpdatePromptEntityRequest;
import com.marketinghub.prompt.service.PromptEntityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompt-entities")
public class PromptEntityController {
    private final PromptEntityService service;

    public PromptEntityController(PromptEntityService service) {
        this.service = service;
    }

    @GetMapping
    public List<PromptEntityDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public PromptEntityDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public PromptEntityDto create(@RequestBody CreatePromptEntityRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public PromptEntityDto update(@PathVariable Long id, @RequestBody UpdatePromptEntityRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
