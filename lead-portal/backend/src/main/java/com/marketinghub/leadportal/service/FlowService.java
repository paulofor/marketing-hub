package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.exception.FlowNotFoundException;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.entity.FlowEntity;
import com.marketinghub.leadportal.repository.FlowRepository;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlowService {

    private final FlowRepository repository;

    public FlowService(FlowRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Flow save(Flow flow) {
        FlowEntity entityToSave = FlowEntity.fromModel(flow);
        repository
                .findById(flow.slug())
                .ifPresent(existing -> entityToSave.setAccessCount(existing.getAccessCount()));

        FlowEntity saved = repository.save(entityToSave);
        return saved.toModel();
    }

    @Transactional(readOnly = true)
    public Flow get(String slug) {
        return repository
                .findById(slug)
                .map(FlowEntity::toModel)
                .orElseThrow(() -> new FlowNotFoundException(slug));
    }

    @Transactional
    public Flow getAndTrackAccess(String slug) {
        FlowEntity entity = repository
                .findById(slug)
                .orElseThrow(() -> new FlowNotFoundException(slug));

        repository.incrementAccessCount(slug);

        return entity.toModel();
    }

    @Transactional
    public void delete(String slug) {
        if (repository.existsById(slug)) {
            repository.deleteById(slug);
        }
    }

    @Transactional(readOnly = true)
    public Collection<Flow> list() {
        return repository.findAll().stream().map(FlowEntity::toModel).toList();
    }
}
