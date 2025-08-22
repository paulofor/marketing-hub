package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.dto.CreatePromptEntityRequest;
import com.marketinghub.prompt.dto.PromptEntityDto;
import com.marketinghub.prompt.dto.UpdatePromptEntityRequest;
import com.marketinghub.prompt.mapper.PromptEntityMapper;
import com.marketinghub.prompt.repository.PromptEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptEntityService {
    private final PromptEntityRepository repository;
    private final PromptEntityMapper mapper;

    public PromptEntityService(PromptEntityRepository repository, PromptEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<PromptEntityDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public PromptEntityDto get(Long id) {
        PromptEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PromptEntity not found"));
        return mapper.toDto(entity);
    }

    public PromptEntityDto create(CreatePromptEntityRequest req) {
        PromptEntity entity = PromptEntity.builder().name(req.getName()).build();
        repository.save(entity);
        return mapper.toDto(entity);
    }

    public PromptEntityDto update(Long id, UpdatePromptEntityRequest req) {
        PromptEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PromptEntity not found"));
        entity.setName(req.getName());
        repository.save(entity);
        return mapper.toDto(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
