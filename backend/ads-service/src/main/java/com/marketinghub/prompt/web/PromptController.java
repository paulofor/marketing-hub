package com.marketinghub.prompt.web;

import com.marketinghub.prompt.dto.CreatePromptRequest;
import com.marketinghub.prompt.dto.PromptDto;
import com.marketinghub.prompt.dto.PromptTemplateValidationRequest;
import com.marketinghub.prompt.dto.PromptTemplateValidationResponse;
import com.marketinghub.prompt.dto.UpdatePromptRequest;
import com.marketinghub.prompt.mapper.PromptMapper;
import com.marketinghub.prompt.service.PromptService;
import com.marketinghub.prompt.service.PromptTemplateValidationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {
    private final PromptService service;
    private final PromptMapper mapper;
    private final PromptTemplateValidationService validationService;

    public PromptController(PromptService service, PromptMapper mapper, PromptTemplateValidationService validationService) {
        this.service = service;
        this.mapper = mapper;
        this.validationService = validationService;
    }

    @GetMapping
    public List<PromptDto> list(@RequestParam(value = "domain", required = false) String domain) {
        return service.list(domain).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PromptDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PostMapping
    public PromptDto create(@RequestBody CreatePromptRequest request) {
        return mapper.toDto(service.create(request));
    }

    @PutMapping("/{id}")
    public PromptDto update(@PathVariable Long id, @RequestBody UpdatePromptRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @PostMapping("/{id}/activate")
    public PromptDto activate(@PathVariable Long id) {
        return mapper.toDto(service.activate(id));
    }

    @PostMapping("/validate")
    public PromptTemplateValidationResponse validate(@RequestBody PromptTemplateValidationRequest request) {
        return validationService.validate(request);
    }
}
