package com.marketinghub.pipeline.web;

import com.marketinghub.pipeline.Pipeline;
import com.marketinghub.pipeline.PipelineStage;
import com.marketinghub.pipeline.dto.PipelineDiagnosticsDto;
import com.marketinghub.pipeline.dto.PipelineDto;
import com.marketinghub.pipeline.dto.PipelineMetadataDto;
import com.marketinghub.pipeline.dto.PipelineRequest;
import com.marketinghub.pipeline.dto.PipelineStageDto;
import com.marketinghub.pipeline.dto.PipelineStageRequest;
import com.marketinghub.pipeline.dto.PipelineSyncResultDto;
import com.marketinghub.pipeline.mapper.PipelineMapper;
import com.marketinghub.pipeline.service.PipelineDefinitionSynchronizer;
import com.marketinghub.pipeline.service.PipelineService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para administrar pipelines e etapas configuráveis do Marketing Hub.
 */
@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {
    private final PipelineService service;
    private final PipelineDefinitionSynchronizer synchronizer;
    private final PipelineMapper mapper;

    /**
     * Inicializa o controller com serviço, sincronizador seguro e mapper de contratos.
     */
    public PipelineController(
            PipelineService service, PipelineDefinitionSynchronizer synchronizer, PipelineMapper mapper) {
        this.service = service;
        this.synchronizer = synchronizer;
        this.mapper = mapper;
    }

    /**
     * Lista pipelines com etapas para a tela de CRUD.
     */
    @GetMapping
    public List<PipelineDto> list() {
        return service.list().stream().map(mapper::toDto).toList();
    }

    /**
     * Obtém metadados oficiais que governam campos editáveis na tela.
     */
    @GetMapping("/metadata")
    public PipelineMetadataDto metadata() {
        return service.metadata();
    }

    /**
     * Obtém um pipeline específico com suas etapas ordenadas.
     */
    @GetMapping("/{id}")
    public PipelineDto get(@PathVariable Long id) {
        return mapper.toDto(service.get(id));
    }

    /**
     * Diagnostica divergências entre banco e definição oficial do backend.
     */
    @GetMapping("/{id}/diagnostics")
    public PipelineDiagnosticsDto diagnostics(@PathVariable Long id) {
        return service.diagnostics(id);
    }

    /**
     * Sincroniza com segurança o pipeline informado contra sua definição oficial.
     */
    @PostMapping("/{id}/sync")
    public PipelineSyncResultDto sync(@PathVariable Long id) {
        return synchronizer.sync(id);
    }

    /**
     * Cria ou sincroniza com segurança um pipeline oficial pelo código canônico.
     */
    @PostMapping("/official/{code}/sync")
    public PipelineSyncResultDto syncOfficial(@PathVariable String code) {
        return synchronizer.syncOfficialByCode(code);
    }

    /**
     * Cria um pipeline operacional configurável.
     */
    @PostMapping
    public PipelineDto create(@Valid @RequestBody PipelineRequest request) {
        Pipeline pipeline = service.create(request);
        return mapper.toDto(pipeline);
    }

    /**
     * Atualiza os dados básicos de um pipeline configurável.
     */
    @PutMapping("/{id}")
    public PipelineDto update(@PathVariable Long id, @Valid @RequestBody PipelineRequest request) {
        Pipeline pipeline = service.update(id, request);
        return mapper.toDto(pipeline);
    }

    /**
     * Remove um pipeline configurável e suas etapas.
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /**
     * Cria uma etapa dentro de um pipeline configurável.
     */
    @PostMapping("/{pipelineId}/stages")
    public PipelineStageDto createStage(
            @PathVariable Long pipelineId, @Valid @RequestBody PipelineStageRequest request) {
        PipelineStage stage = service.createStage(pipelineId, request);
        return mapper.toStageDto(stage);
    }

    /**
     * Atualiza uma etapa de um pipeline configurável.
     */
    @PutMapping("/{pipelineId}/stages/{stageId}")
    public PipelineStageDto updateStage(
            @PathVariable Long pipelineId,
            @PathVariable Long stageId,
            @Valid @RequestBody PipelineStageRequest request) {
        PipelineStage stage = service.updateStage(pipelineId, stageId, request);
        return mapper.toStageDto(stage);
    }

    /**
     * Remove uma etapa de um pipeline configurável.
     */
    @DeleteMapping("/{pipelineId}/stages/{stageId}")
    public void deleteStage(@PathVariable Long pipelineId, @PathVariable Long stageId) {
        service.deleteStage(pipelineId, stageId);
    }
}
