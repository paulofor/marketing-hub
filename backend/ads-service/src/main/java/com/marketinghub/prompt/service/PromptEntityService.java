package com.marketinghub.prompt.service;

import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.repository.PromptEntityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptEntityService {
    private final PromptEntityRepository repository;

    public PromptEntityService(PromptEntityRepository repository) {
        this.repository = repository;
    }

    public List<String> listNames() {
        return repository.findAll().stream()
                .map(PromptEntity::getName)
                .toList();
    }
}
