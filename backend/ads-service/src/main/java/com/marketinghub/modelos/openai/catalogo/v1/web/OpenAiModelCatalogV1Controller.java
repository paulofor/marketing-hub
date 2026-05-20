package com.marketinghub.modelos.openai.catalogo.v1.web;

import com.marketinghub.modelos.openai.catalogo.v1.dto.OpenAiModelCatalogResponse;
import com.marketinghub.modelos.openai.catalogo.v1.service.OpenAiModelCatalogV1Service;
import com.marketinghub.openai.dto.CreateOpenAiModelRequest;
import com.marketinghub.openai.dto.OpenAiModelDto;
import com.marketinghub.openai.mapper.OpenAiModelMapper;
import com.marketinghub.openai.service.OpenAiModelService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/modelos/openai/catalogo/v1")
public class OpenAiModelCatalogV1Controller {
    private final OpenAiModelService service;
    private final OpenAiModelMapper mapper;
    private final OpenAiModelCatalogV1Service catalogService;

    public OpenAiModelCatalogV1Controller(OpenAiModelService service, OpenAiModelMapper mapper, OpenAiModelCatalogV1Service catalogService) {
        this.service = service;
        this.mapper = mapper;
        this.catalogService = catalogService;
    }

    @PostMapping("/modelos")
    public OpenAiModelDto create(@RequestBody CreateOpenAiModelRequest request) { return mapper.toDto(service.create(request)); }
    @GetMapping("/modelos/{id}")
    public OpenAiModelDto get(@PathVariable Long id) { return mapper.toDto(service.get(id)); }
    @PutMapping("/modelos/{id}")
    public OpenAiModelDto update(@PathVariable Long id, @RequestBody CreateOpenAiModelRequest request) { return mapper.toDto(service.update(id, request)); }
    @GetMapping("/modelos")
    public List<OpenAiModelDto> list() { return service.list().stream().map(mapper::toDto).toList(); }
    @GetMapping
    public OpenAiModelCatalogResponse catalog() { return catalogService.fetchAndPersistCatalog(); }
}
