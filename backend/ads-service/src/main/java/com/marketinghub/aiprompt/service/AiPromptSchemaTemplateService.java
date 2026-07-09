package com.marketinghub.aiprompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import com.marketinghub.aiprompt.service.alterar.UpdateAiPromptSchemaTemplateRequest;
import com.marketinghub.aiprompt.service.listar.AiPromptSchemaTemplateResponse;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar templates operacionais de prompt/schema usados pelos pipelines de IA. */
@Service
public class AiPromptSchemaTemplateService {
    private final AiPromptSchemaTemplateRepository repository;
    private final ObjectMapper objectMapper;

    /** Inicializa o service com persistência e validador JSON. */
    public AiPromptSchemaTemplateService(AiPromptSchemaTemplateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Lista templates com filtros opcionais por pipeline e etapa. */
    @Transactional(readOnly = true)
    public List<AiPromptSchemaTemplateResponse> list(String pipelineCode, String stageCode) {
        if (StringUtils.hasText(pipelineCode) && StringUtils.hasText(stageCode)) {
            return repository.findByPipelineCodeAndStageCodeOrderByVersionDesc(
                            normalize(pipelineCode), normalize(stageCode))
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (StringUtils.hasText(pipelineCode)) {
            return repository.findByPipelineCodeOrderByStageCodeAscVersionDesc(normalize(pipelineCode))
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        return repository.findAll().stream()
                .sorted((left, right) -> compareTemplates(left, right))
                .map(this::toResponse)
                .toList();
    }

    /** Busca um template pelo identificador técnico. */
    @Transactional(readOnly = true)
    public AiPromptSchemaTemplateResponse get(String templateKey) {
        return toResponse(load(templateKey));
    }

    /** Atualiza prompt, schema e metadados de execução de um template existente. */
    @Transactional
    public AiPromptSchemaTemplateResponse update(String templateKey, UpdateAiPromptSchemaTemplateRequest request) {
        AiPromptSchemaTemplate template = load(templateKey);
        validate(request);
        template.setVersion(request.version().trim());
        template.setOpenAiModel(request.openAiModel().trim());
        template.setSchemaName(request.schemaName().trim());
        template.setPromptMarkdownContent(request.promptMarkdownContent());
        template.setSchemaJson(request.schemaJson());
        template.setActive(Boolean.TRUE.equals(request.active()));
        template.setUpdatedAt(Instant.now());
        AiPromptSchemaTemplate saved = repository.save(template);
        if (saved.isActive()) {
            repository.deactivateOthersForStage(
                    saved.getPipelineCode(), saved.getStageCode(), saved.getTemplateKey(), Instant.now());
        }
        return toResponse(saved);
    }

    /** Ativa um template para a etapa e desativa os demais da mesma etapa. */
    @Transactional
    public AiPromptSchemaTemplateResponse activate(String templateKey) {
        AiPromptSchemaTemplate template = load(templateKey);
        template.setActive(true);
        template.setUpdatedAt(Instant.now());
        AiPromptSchemaTemplate saved = repository.save(template);
        repository.deactivateOthersForStage(
                saved.getPipelineCode(), saved.getStageCode(), saved.getTemplateKey(), Instant.now());
        return toResponse(saved);
    }

    /** Carrega um template ou falha quando o identificador não existe. */
    private AiPromptSchemaTemplate load(String templateKey) {
        return repository.findById(templateKey)
                .orElseThrow(() -> new EntityNotFoundException("AI prompt schema template not found: " + templateKey));
    }

    /** Valida campos obrigatórios e o JSON schema antes de persistir. */
    private void validate(UpdateAiPromptSchemaTemplateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request is required");
        }
        if (!StringUtils.hasText(request.version())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "version is required");
        }
        if (!StringUtils.hasText(request.openAiModel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "openAiModel is required");
        }
        if (!StringUtils.hasText(request.schemaName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schemaName is required");
        }
        if (!StringUtils.hasText(request.promptMarkdownContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promptMarkdownContent is required");
        }
        if (!StringUtils.hasText(request.schemaJson())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schemaJson is required");
        }
        try {
            objectMapper.readTree(request.schemaJson());
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schemaJson must be valid JSON", ex);
        }
    }

    /** Converte entidade persistida para contrato da API. */
    private AiPromptSchemaTemplateResponse toResponse(AiPromptSchemaTemplate template) {
        return new AiPromptSchemaTemplateResponse(
                template.getTemplateKey(),
                template.getPipelineCode(),
                template.getStageCode(),
                template.getVersion(),
                template.getOpenAiModel(),
                template.getSchemaName(),
                template.getPromptMarkdownContent(),
                template.getSchemaJson(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }

    /** Ordena templates por pipeline, etapa e versão para listagem administrativa estável. */
    private int compareTemplates(AiPromptSchemaTemplate left, AiPromptSchemaTemplate right) {
        int pipeline = left.getPipelineCode().compareToIgnoreCase(right.getPipelineCode());
        if (pipeline != 0) {
            return pipeline;
        }
        int stage = left.getStageCode().compareToIgnoreCase(right.getStageCode());
        if (stage != 0) {
            return stage;
        }
        return right.getVersion().compareToIgnoreCase(left.getVersion());
    }

    /** Normaliza filtros preservando o padrão técnico dos códigos. */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
