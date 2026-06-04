package com.marketinghub.pipeline.service;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.pipeline.Pipeline;
import com.marketinghub.pipeline.PipelineStage;
import com.marketinghub.pipeline.dto.PipelineRequest;
import com.marketinghub.pipeline.dto.PipelineStageRequest;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por manter pipelines e etapas usados pela operação do Marketing Hub.
 */
@Service
public class PipelineService {
    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository stageRepository;
    private final OpenAiModelRepository openAiModelRepository;

    /**
     * Inicializa o serviço com os repositórios centralizados de pipelines, etapas e modelos OpenAI.
     */
    public PipelineService(
            PipelineRepository pipelineRepository,
            PipelineStageRepository stageRepository,
            OpenAiModelRepository openAiModelRepository) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.openAiModelRepository = openAiModelRepository;
    }

    /**
     * Lista todos os pipelines cadastrados com etapas ordenadas para administração.
     */
    public List<Pipeline> list() {
        return pipelineRepository.findAll().stream()
                .peek(this::sortStages)
                .sorted(Comparator.comparing(Pipeline::getModule).thenComparing(Pipeline::getName))
                .toList();
    }

    /**
     * Busca um pipeline pelo identificador, garantindo erro claro quando não existe.
     */
    public Pipeline get(Long id) {
        Pipeline pipeline = findPipeline(id);
        sortStages(pipeline);
        return pipeline;
    }

    /**
     * Cria um novo pipeline operacional.
     */
    @Transactional
    public Pipeline create(PipelineRequest request) {
        Pipeline pipeline = new Pipeline();
        applyPipelineRequest(pipeline, request);
        return pipelineRepository.save(pipeline);
    }

    /**
     * Atualiza os dados básicos de um pipeline existente.
     */
    @Transactional
    public Pipeline update(Long id, PipelineRequest request) {
        Pipeline pipeline = findPipeline(id);
        applyPipelineRequest(pipeline, request);
        return pipelineRepository.save(pipeline);
    }

    /**
     * Remove um pipeline e suas etapas vinculadas.
     */
    @Transactional
    public void delete(Long id) {
        pipelineRepository.delete(findPipeline(id));
    }

    /**
     * Cria uma etapa dentro de um pipeline existente.
     */
    @Transactional
    public PipelineStage createStage(Long pipelineId, PipelineStageRequest request) {
        Pipeline pipeline = findPipeline(pipelineId);
        PipelineStage stage = new PipelineStage();
        stage.setPipeline(pipeline);
        applyStageRequest(stage, request);
        return stageRepository.save(stage);
    }

    /**
     * Atualiza uma etapa, preservando o vínculo com o pipeline informado na rota.
     */
    @Transactional
    public PipelineStage updateStage(Long pipelineId, Long stageId, PipelineStageRequest request) {
        PipelineStage stage = findStageInPipeline(pipelineId, stageId);
        applyStageRequest(stage, request);
        return stageRepository.save(stage);
    }

    /**
     * Remove uma etapa específica de um pipeline.
     */
    @Transactional
    public void deleteStage(Long pipelineId, Long stageId) {
        stageRepository.delete(findStageInPipeline(pipelineId, stageId));
    }

    /**
     * Localiza um pipeline pelo identificador interno.
     */
    private Pipeline findPipeline(Long id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pipeline não encontrado: " + id));
    }

    /**
     * Localiza uma etapa validando que ela pertence ao pipeline da rota.
     */
    private PipelineStage findStageInPipeline(Long pipelineId, Long stageId) {
        PipelineStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Etapa de pipeline não encontrada: " + stageId));
        if (!stage.getPipeline().getId().equals(pipelineId)) {
            throw new EntityNotFoundException("Etapa não pertence ao pipeline informado: " + pipelineId);
        }
        return stage;
    }

    /**
     * Aplica o payload validado nos campos básicos de pipeline.
     */
    private void applyPipelineRequest(Pipeline pipeline, PipelineRequest request) {
        pipeline.setName(request.getName());
        pipeline.setCode(request.getCode());
        pipeline.setModule(request.getModule());
        pipeline.setDescription(request.getDescription());
        pipeline.setActive(request.isActive());
    }

    /**
     * Aplica o payload validado nos campos de configuração da etapa.
     */
    private void applyStageRequest(PipelineStage stage, PipelineStageRequest request) {
        stage.setPosition(request.getPosition());
        stage.setName(request.getName());
        stage.setCode(request.getCode());
        stage.setDescription(request.getDescription());
        stage.setRequired(request.isRequired());
        stage.setActive(request.isActive());
        stage.setOpenAiModel(resolveOpenAiModel(request.getOpenAiModelId()));
    }

    /**
     * Resolve o modelo OpenAI escolhido para a etapa ou remove a escolha quando não informado.
     */
    private OpenAiModel resolveOpenAiModel(Long openAiModelId) {
        if (openAiModelId == null) {
            return null;
        }
        return openAiModelRepository.findById(openAiModelId)
                .orElseThrow(() -> new EntityNotFoundException("Modelo OpenAI não encontrado: " + openAiModelId));
    }

    /**
     * Ordena as etapas carregadas para manter consistência visual e operacional.
     */
    private void sortStages(Pipeline pipeline) {
        pipeline.getStages().sort(Comparator.comparing(PipelineStage::getPosition).thenComparing(PipelineStage::getId));
    }
}
