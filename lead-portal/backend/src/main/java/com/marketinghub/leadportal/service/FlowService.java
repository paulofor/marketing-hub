package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.entity.FlowAccessEntity;
import com.marketinghub.leadportal.entity.FlowEntity;
import com.marketinghub.leadportal.exception.FlowNotFoundException;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowAccessMetadata;
import com.marketinghub.leadportal.repository.FlowAccessRepository;
import com.marketinghub.leadportal.repository.FlowRepository;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlowService {

    private final FlowRepository repository;
    private final FlowAccessRepository accessRepository;

    public FlowService(FlowRepository repository, FlowAccessRepository accessRepository) {
        this.repository = repository;
        this.accessRepository = accessRepository;
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
    public Flow getAndTrackAccess(String slug, FlowAccessMetadata accessMetadata) {
        FlowEntity entity = repository
                .findById(slug)
                .orElseThrow(() -> new FlowNotFoundException(slug));

        repository.incrementAccessCount(slug);
        registerAccess(slug, accessMetadata);

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

    private void registerAccess(String slug, FlowAccessMetadata metadata) {
        if (metadata == null) {
            return;
        }

        FlowAccessEntity access = new FlowAccessEntity();
        access.setFlowSlug(slug);
        access.setClientIp(metadata.clientIp());
        access.setUserAgent(metadata.userAgent());
        access.setReferer(metadata.referer());
        access.setVisitorId(metadata.visitorId());

        accessRepository.save(access);
    }
}
