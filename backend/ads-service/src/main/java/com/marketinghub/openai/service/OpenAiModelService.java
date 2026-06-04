package com.marketinghub.openai.service;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.dto.CreateOpenAiModelRequest;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: manter o catálogo administrativo de modelos OpenAI e suas capacidades. */
@Service
public class OpenAiModelService {

    private final OpenAiModelRepository repository;

    /** Inicializa o serviço com o repositório centralizado do catálogo de modelos OpenAI. */
    public OpenAiModelService(OpenAiModelRepository repository) {
        this.repository = repository;
    }

    /** Cria um modelo OpenAI com preços e capacidades declaradas para uso operacional. */
    @Transactional
    public OpenAiModel create(CreateOpenAiModelRequest request) {
        OpenAiModel model = new OpenAiModel();
        apply(model, request);
        return repository.save(model);
    }

    /** Busca um modelo OpenAI pelo identificador para edição ou detalhe. */
    @Transactional(readOnly = true)
    public OpenAiModel get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    /** Lista modelos OpenAI cadastrados ordenados por nome para seleção nas telas. */
    @Transactional(readOnly = true)
    public List<OpenAiModel> list() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    /** Atualiza preços e capacidades de um modelo OpenAI existente. */
    @Transactional
    public OpenAiModel update(Long id, CreateOpenAiModelRequest request) {
        OpenAiModel model = repository.findById(id).orElseThrow();
        apply(model, request);
        return repository.save(model);
    }

    /** Aplica os campos editáveis do request na entidade persistida. */
    private void apply(OpenAiModel model, CreateOpenAiModelRequest request) {
        model.setName(request.getName());
        model.setCode(request.getCode());
        model.setPriceInputStandard(request.getPriceInputStandard());
        model.setPriceInputCachedStandard(request.getPriceInputCachedStandard());
        model.setPriceOutputStandard(request.getPriceOutputStandard());
        model.setPriceInputBatch(request.getPriceInputBatch());
        model.setPriceInputCachedBatch(request.getPriceInputCachedBatch());
        model.setPriceOutputBatch(request.getPriceOutputBatch());
        model.setAcceptsImageInput(request.isAcceptsImageInput());
    }
}
