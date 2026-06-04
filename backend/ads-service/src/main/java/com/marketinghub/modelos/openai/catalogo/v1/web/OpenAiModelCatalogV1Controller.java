package com.marketinghub.modelos.openai.catalogo.v1.web;

import com.marketinghub.modelos.openai.catalogo.v1.dto.OpenAiModelCatalogResponse;
import com.marketinghub.modelos.openai.catalogo.v1.service.OpenAiModelCatalogV1Service;
import com.marketinghub.openai.dto.CreateOpenAiModelRequest;
import com.marketinghub.openai.dto.OpenAiModelDto;
import com.marketinghub.openai.mapper.OpenAiModelMapper;
import com.marketinghub.openai.service.OpenAiModelService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o catálogo administrativo e oficial de modelos OpenAI para o frontend. */
@RestController
@RequestMapping("/api/modelos/openai/catalogo/v1")
public class OpenAiModelCatalogV1Controller {
    private final OpenAiModelService service;
    private final OpenAiModelMapper mapper;
    private final OpenAiModelCatalogV1Service catalogService;

    /** Inicializa o controller com serviços de cadastro local, mapeamento e sincronização do catálogo oficial. */
    public OpenAiModelCatalogV1Controller(
            OpenAiModelService service, OpenAiModelMapper mapper, OpenAiModelCatalogV1Service catalogService) {
        this.service = service;
        this.mapper = mapper;
        this.catalogService = catalogService;
    }

    /** Cria um modelo OpenAI no catálogo administrativo. */
    @PostMapping("/modelos")
    public OpenAiModelDto create(@RequestBody CreateOpenAiModelRequest request) {
        return mapper.toDto(service.create(request));
    }

    /** Retorna um modelo OpenAI específico para edição. */
    @GetMapping("/modelos/{id}")
    public OpenAiModelDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    /** Atualiza um modelo OpenAI existente no catálogo administrativo. */
    @PutMapping("/modelos/{id}")
    public OpenAiModelDto update(@PathVariable Long id, @RequestBody CreateOpenAiModelRequest request) {
        return mapper.toDto(service.update(id, request));
    }

    /** Lista os modelos OpenAI cadastrados para telas e seletores de pipeline. */
    @GetMapping("/modelos")
    public List<OpenAiModelDto> list() {
        return service.list().stream().map(mapper::toDto).toList();
    }

    /** Consulta o catálogo oficial da OpenAI e persiste modelos reconhecidos localmente. */
    @GetMapping
    public OpenAiModelCatalogResponse catalog() {
        return catalogService.fetchAndPersistCatalog();
    }
}
