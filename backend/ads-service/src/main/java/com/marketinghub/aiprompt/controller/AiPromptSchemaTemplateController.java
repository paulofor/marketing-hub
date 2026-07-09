package com.marketinghub.aiprompt.controller;

import com.marketinghub.aiprompt.service.AiPromptSchemaTemplateService;
import com.marketinghub.aiprompt.service.alterar.UpdateAiPromptSchemaTemplateRequest;
import com.marketinghub.aiprompt.service.listar.AiPromptSchemaTemplateResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor administração dos prompts e schemas operacionais dos pipelines de IA. */
@RestController
@RequestMapping("/api/ai-prompt-schema-templates")
public class AiPromptSchemaTemplateController {
    private final AiPromptSchemaTemplateService service;

    /** Inicializa o controller com o service de templates operacionais. */
    public AiPromptSchemaTemplateController(AiPromptSchemaTemplateService service) {
        this.service = service;
    }

    /** Lista templates com filtros opcionais por pipeline e etapa. */
    @GetMapping
    public List<AiPromptSchemaTemplateResponse> list(
            @RequestParam(value = "pipelineCode", required = false) String pipelineCode,
            @RequestParam(value = "stageCode", required = false) String stageCode) {
        return service.list(pipelineCode, stageCode);
    }

    /** Busca um template operacional pelo identificador técnico. */
    @GetMapping("/{templateKey}")
    public AiPromptSchemaTemplateResponse get(@PathVariable String templateKey) {
        return service.get(templateKey);
    }

    /** Atualiza prompt, schema, modelo e estado ativo do template. */
    @PutMapping("/{templateKey}")
    public AiPromptSchemaTemplateResponse update(
            @PathVariable String templateKey,
            @RequestBody UpdateAiPromptSchemaTemplateRequest request) {
        return service.update(templateKey, request);
    }

    /** Ativa o template como versão canônica da etapa. */
    @PostMapping("/{templateKey}/activate")
    public AiPromptSchemaTemplateResponse activate(@PathVariable String templateKey) {
        return service.activate(templateKey);
    }
}
