package com.marketinghub.prompt.web;

import com.marketinghub.prompt.service.PromptEntityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/prompt-entities")
public class PromptEntityController {
    private final PromptEntityService service;

    public PromptEntityController(PromptEntityService service) {
        this.service = service;
    }

    @GetMapping
    public List<String> list() {
        return service.listNames();
    }
}
