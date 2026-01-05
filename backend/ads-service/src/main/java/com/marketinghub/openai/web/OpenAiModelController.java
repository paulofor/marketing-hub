package com.marketinghub.openai.web;

import com.marketinghub.openai.dto.CreateOpenAiModelRequest;
import com.marketinghub.openai.dto.OpenAiModelDto;
import com.marketinghub.openai.mapper.OpenAiModelMapper;
import com.marketinghub.openai.service.OpenAiModelService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/openai-models")
public class OpenAiModelController {

    private final OpenAiModelService service;
    private final OpenAiModelMapper mapper;

    public OpenAiModelController(OpenAiModelService service, OpenAiModelMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public OpenAiModelDto create(@RequestBody CreateOpenAiModelRequest request) {
        return mapper.toDto(service.create(request));
    }

    @GetMapping("/{id}")
    public OpenAiModelDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    @PutMapping("/{id}")
    public OpenAiModelDto update(@PathVariable Long id, @RequestBody CreateOpenAiModelRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    @GetMapping
    public List<OpenAiModelDto> list() {
        return service.list().stream().map(mapper::toDto).toList();
    }
}
