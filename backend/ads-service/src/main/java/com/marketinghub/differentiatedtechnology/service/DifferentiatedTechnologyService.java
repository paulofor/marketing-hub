package com.marketinghub.differentiatedtechnology.service;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.differentiatedtechnology.dto.CreateDifferentiatedTechnologyRequest;
import com.marketinghub.differentiatedtechnology.dto.UpdateDifferentiatedTechnologyRequest;
import com.marketinghub.differentiatedtechnology.repository.DifferentiatedTechnologyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DifferentiatedTechnologyService {
    private final DifferentiatedTechnologyRepository repository;

    public DifferentiatedTechnologyService(DifferentiatedTechnologyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DifferentiatedTechnology create(CreateDifferentiatedTechnologyRequest request) {
        DifferentiatedTechnology technology = DifferentiatedTechnology.builder()
                .name(request.getName())
                .description(request.getDescription())
                .promptText(request.getPromptText())
                .build();
        return repository.save(technology);
    }

    public List<DifferentiatedTechnology> list() {
        return repository.findAll();
    }

    public DifferentiatedTechnology get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public DifferentiatedTechnology update(Long id, UpdateDifferentiatedTechnologyRequest request) {
        DifferentiatedTechnology technology = repository.findById(id).orElseThrow();
        technology.setName(request.getName());
        technology.setDescription(request.getDescription());
        technology.setPromptText(request.getPromptText());
        return repository.save(technology);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
