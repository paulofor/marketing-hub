package com.marketinghub.openai.service;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.dto.CreateOpenAiModelRequest;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenAiModelService {

    private final OpenAiModelRepository repository;

    public OpenAiModelService(OpenAiModelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OpenAiModel create(CreateOpenAiModelRequest request) {
        OpenAiModel model = new OpenAiModel();
        apply(model, request);
        return repository.save(model);
    }

    @Transactional(readOnly = true)
    public OpenAiModel get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<OpenAiModel> list() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional
    public OpenAiModel update(Long id, CreateOpenAiModelRequest request) {
        OpenAiModel model = repository.findById(id).orElseThrow();
        apply(model, request);
        return repository.save(model);
    }

    private void apply(OpenAiModel model, CreateOpenAiModelRequest request) {
        model.setName(request.getName());
        model.setCode(request.getCode());
        model.setPriceInputStandard(request.getPriceInputStandard());
        model.setPriceInputCachedStandard(request.getPriceInputCachedStandard());
        model.setPriceOutputStandard(request.getPriceOutputStandard());
        model.setPriceInputBatch(request.getPriceInputBatch());
        model.setPriceInputCachedBatch(request.getPriceInputCachedBatch());
        model.setPriceOutputBatch(request.getPriceOutputBatch());
    }
}
