package com.marketinghub.prompt.web;

import com.marketinghub.prompt.dto.PromptEntityDescriptionDto;
import com.marketinghub.prompt.dto.UpdatePromptEntityDescriptionRequest;
import com.marketinghub.prompt.service.PromptEntityDescriptionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prompt-entities/{entityName}/description")
public class PromptEntityDescriptionController {
    private final PromptEntityDescriptionService service;

    public PromptEntityDescriptionController(PromptEntityDescriptionService service) {
        this.service = service;
    }

    @GetMapping
    public PromptEntityDescriptionDto get(@PathVariable String entityName) {
        return service.getLatest(entityName);
    }

    @PutMapping
    public PromptEntityDescriptionDto update(@PathVariable String entityName,
                                             @RequestBody UpdatePromptEntityDescriptionRequest req) {
        return service.update(entityName, req);
    }
}
