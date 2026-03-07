package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.catalog.SimpleFlowCatalog;
import com.marketinghub.leadportal.entity.FlowAccessEntity;
import com.marketinghub.leadportal.entity.FlowEntity;
import com.marketinghub.leadportal.exception.FlowNotFoundException;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowAccessMetadata;
import com.marketinghub.leadportal.model.SimpleFormStyle;
import com.marketinghub.leadportal.repository.FlowAccessRepository;
import com.marketinghub.leadportal.repository.FlowRepository;
import com.marketinghub.leadportal.style.SimpleFormStyleDefaults;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlowService {

    private final FlowRepository repository;
    private final FlowAccessRepository accessRepository;
    private final MeterRegistry meterRegistry;
    private final SimpleFlowCatalog simpleFlowCatalog;
    private final FlowAssetService flowAssetService;
    private final SimpleFormStyleDefaults simpleFormStyleDefaults;

    public FlowService(
            FlowRepository repository,
            FlowAccessRepository accessRepository,
            MeterRegistry meterRegistry,
            SimpleFlowCatalog simpleFlowCatalog,
            FlowAssetService flowAssetService,
            SimpleFormStyleDefaults simpleFormStyleDefaults) {
        this.repository = repository;
        this.accessRepository = accessRepository;
        this.meterRegistry = meterRegistry;
        this.simpleFlowCatalog = simpleFlowCatalog;
        this.flowAssetService = flowAssetService;
        this.simpleFormStyleDefaults = simpleFormStyleDefaults;
    }

    @Transactional
    public Flow save(Flow flow) {
        if (simpleFlowCatalog.supports(flow.slug())) {
            throw new IllegalArgumentException(
                    "Fluxos simples são gerenciados automaticamente e não podem ser editados.");
        }

        Flow flowWithDefaults = applyStyleDefaults(flow);
        Flow processedFlow = flowAssetService.optimizeAssets(flowWithDefaults);
        Flow flowToPersist = processedFlow != null ? processedFlow : flowWithDefaults;

        FlowEntity entityToSave = FlowEntity.fromModel(flowToPersist);
        repository
                .findById(flow.slug())
                .ifPresent(existing -> entityToSave.setAccessCount(existing.getAccessCount()));

        FlowEntity saved = repository.save(entityToSave);
        return applyStyleDefaults(saved.toModel());
    }

    @Transactional(readOnly = true)
    public Flow get(String slug) {
        Flow flow = simpleFlowCatalog.find(slug).orElseGet(() -> repository
                .findById(slug)
                .map(FlowEntity::toModel)
                .orElseThrow(() -> new FlowNotFoundException(slug)));
        return applyStyleDefaults(flow);
    }

    @Transactional
    public Flow getAndTrackAccess(String slug, FlowAccessMetadata accessMetadata) {
        return simpleFlowCatalog.find(slug)
                .map(flow -> {
                    Flow preparedFlow = applyStyleDefaults(flow);
                    recordAccessMetric(slug);
                    registerAccess(slug, accessMetadata);
                    return preparedFlow;
                })
                .orElseGet(() -> fetchAndTrackPersistedFlow(slug, accessMetadata));
    }

    @Transactional
    public void delete(String slug) {
        if (simpleFlowCatalog.supports(slug)) {
            throw new IllegalArgumentException(
                    "Fluxos simples são gerenciados automaticamente e não podem ser excluídos.");
        }

        if (repository.existsById(slug)) {
            repository.deleteById(slug);
        }
    }

    @Transactional(readOnly = true)
    public Collection<Flow> list() {
        return repository.findAll().stream()
                .map(FlowEntity::toModel)
                .map(this::applyStyleDefaults)
                .toList();
    }

    private Flow fetchAndTrackPersistedFlow(String slug, FlowAccessMetadata accessMetadata) {
        FlowEntity entity = repository
                .findById(slug)
                .orElseThrow(() -> new FlowNotFoundException(slug));

        Flow originalFlow = applyStyleDefaults(entity.toModel());
        Flow optimizedFlow = flowAssetService.optimizeAssets(originalFlow);

        Flow flowToReturn = originalFlow;
        if (optimizedFlow != null) {
            Flow optimizedWithDefaults = applyStyleDefaults(optimizedFlow);
            if (!optimizedWithDefaults.equals(originalFlow)) {
                FlowEntity updatedEntity = FlowEntity.fromModel(optimizedWithDefaults);
                updatedEntity.setAccessCount(entity.getAccessCount());
                repository.save(updatedEntity);
            }
            flowToReturn = optimizedWithDefaults;
        }

        repository.incrementAccessCount(slug);
        recordAccessMetric(slug);
        registerAccess(slug, accessMetadata);

        return flowToReturn;
    }

    private Flow applyStyleDefaults(Flow flow) {
        if (flow == null) {
            return null;
        }

        SimpleFormStyle enrichedStyle = simpleFormStyleDefaults.applyDefaults(flow.simpleFormStyle());
        if (enrichedStyle == null || enrichedStyle.equals(flow.simpleFormStyle())) {
            return flow;
        }

        return new Flow(
                flow.slug(),
                flow.name(),
                flow.description(),
                flow.model(),
                flow.prompt(),
                flow.questions(),
                enrichedStyle);
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
        access.setCampaignCode(metadata.campaignCode());

        accessRepository.save(access);
    }

    private void recordAccessMetric(String slug) {
        meterRegistry.counter("lead_portal_flow_access_total", "slug", slug).increment();
    }
}
