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

/** Gerencia persistência, leitura e contabilização de acessos dos fluxos públicos. */
@Service
public class FlowService {

    private final FlowRepository repository;
    private final FlowAccessRepository accessRepository;
    private final MeterRegistry meterRegistry;
    private final SimpleFlowCatalog simpleFlowCatalog;
    private final FlowAssetService flowAssetService;
    private final SimpleFormStyleDefaults simpleFormStyleDefaults;

    /** Inicializa o serviço com persistência, métricas, catálogo e tratamento de ativos. */
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

    /** Persiste o fluxo depois de normalizar estilos e migrar ativos legados. */
    @Transactional
    public Flow save(Flow flow) {
        if (simpleFlowCatalog.supports(flow.slug())) {
            throw new IllegalArgumentException(
                    "Fluxos simples são gerenciados automaticamente e não podem ser editados.");
        }

        Flow normalized = normalizeCustomFormHtml(flow);
        Flow flowWithDefaults = applyStyleDefaults(normalized);
        Flow processedFlow = flowAssetService.optimizeAssets(flowWithDefaults);
        Flow flowToPersist = processedFlow != null ? processedFlow : flowWithDefaults;

        FlowEntity entityToSave = FlowEntity.fromModel(flowToPersist);
        repository
                .findById(flow.slug())
                .ifPresent(existing -> entityToSave.setAccessCount(existing.getAccessCount()));

        FlowEntity saved = repository.save(entityToSave);
        return applyStyleDefaults(normalizeCustomFormHtml(saved.toModel()));
    }

    /** Retorna um fluxo sem registrar acesso ou executar integrações externas. */
    @Transactional(readOnly = true)
    public Flow get(String slug) {
        Flow flow = simpleFlowCatalog.find(slug).orElseGet(() -> repository
                .findById(slug)
                .map(FlowEntity::toModel)
                .orElseThrow(() -> new FlowNotFoundException(slug)));
        return applyStyleDefaults(normalizeCustomFormHtml(flow));
    }

    /** Reprocessa de forma idempotente os ativos de um fluxo persistido antes da publicação. */
    @Transactional
    public Flow optimizeExistingAssets(String slug) {
        if (simpleFlowCatalog.supports(slug)) {
            throw new IllegalArgumentException(
                    "Fluxos simples são gerenciados automaticamente e não possuem ativos persistidos.");
        }

        FlowEntity existing = repository
                .findById(slug)
                .orElseThrow(() -> new FlowNotFoundException(slug));
        Flow current = applyStyleDefaults(normalizeCustomFormHtml(existing.toModel()));
        Flow optimized = flowAssetService.optimizeAssets(current);
        if (optimized == null || optimized.equals(current)) {
            return current;
        }

        FlowEntity optimizedEntity = FlowEntity.fromModel(optimized);
        optimizedEntity.setAccessCount(existing.getAccessCount());
        return applyStyleDefaults(normalizeCustomFormHtml(repository.save(optimizedEntity).toModel()));
    }

    /** Retorna o fluxo e registra somente os dados operacionais do acesso. */
    @Transactional
    public Flow getAndTrackAccess(String slug, FlowAccessMetadata accessMetadata) {
        return simpleFlowCatalog.find(slug)
                .map(flow -> {
                    Flow preparedFlow = applyStyleDefaults(normalizeCustomFormHtml(flow));
                    recordAccessMetric(slug);
                    registerAccess(slug, accessMetadata);
                    return preparedFlow;
                })
                .orElseGet(() -> fetchAndTrackPersistedFlow(slug, accessMetadata));
    }

    /** Remove um fluxo persistido quando ele não pertence ao catálogo automático. */
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

    /** Lista os fluxos persistidos com os estilos padrão aplicados. */
    @Transactional(readOnly = true)
    public Collection<Flow> list() {
        return repository.findAll().stream()
                .map(FlowEntity::toModel)
                .map(this::normalizeCustomFormHtml)
                .map(this::applyStyleDefaults)
                .toList();
    }

    /** Carrega o fluxo persistido e contabiliza o acesso sem migrar ativos na rota pública. */
    private Flow fetchAndTrackPersistedFlow(String slug, FlowAccessMetadata accessMetadata) {
        FlowEntity entity = repository
                .findById(slug)
                .orElseThrow(() -> new FlowNotFoundException(slug));

        repository.incrementAccessCount(slug);
        recordAccessMetric(slug);
        registerAccess(slug, accessMetadata);

        return applyStyleDefaults(normalizeCustomFormHtml(entity.toModel()));
    }

    /** Completa o estilo ausente com os padrões do Lead Portal. */
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
                flow.customFormHtml(),
                flow.model(),
                flow.prompt(),
                flow.imagePromptModel(),
                flow.imagePromptTemplate(),
                flow.imageBatchSize(),
                flow.questions(),
                enrichedStyle,
                flow.facebookPixelId(),
                flow.facebookPixelCode(),
                flow.facebookPixelCreatedAt());
    }

    /** Mantém o ponto canônico de normalização do HTML de fluxos. */
    private Flow normalizeCustomFormHtml(Flow flow) {
        if (flow == null) {
            return null;
        }

        return flow;
    }

    /** Persiste os metadados disponíveis para auditoria do acesso. */
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

    /** Incrementa a métrica operacional de acessos do fluxo. */
    private void recordAccessMetric(String slug) {
        meterRegistry.counter("lead_portal_flow_access_total", "slug", slug).increment();
    }
}
