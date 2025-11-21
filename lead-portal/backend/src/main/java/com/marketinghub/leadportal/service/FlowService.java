package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.exception.FlowNotFoundException;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.entity.FlowEntity;
import com.marketinghub.leadportal.repository.FlowRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FlowService {

    private final FlowRepository repository;

    public FlowService(FlowRepository repository) {
        this.repository = repository;
    }

    public Flow save(Flow flow) {
        FlowEntity saved = repository.save(FlowEntity.fromModel(flow));
        return saved.toModel();
    }

    public Flow get(String slug) {
        return repository
                .findById(slug)
                .map(FlowEntity::toModel)
                .orElseThrow(() -> new FlowNotFoundException(slug));
    }

    public void delete(String slug) {
        if (repository.existsById(slug)) {
            repository.deleteById(slug);
        }
    }

    public Collection<Flow> list() {
        return repository.findAll().stream().map(FlowEntity::toModel).toList();
    }
}
