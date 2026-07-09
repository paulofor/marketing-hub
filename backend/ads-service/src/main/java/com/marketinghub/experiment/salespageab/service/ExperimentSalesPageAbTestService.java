package com.marketinghub.experiment.salespageab.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTest;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTestStatus;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariant;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantStatus;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantType;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbTestDto;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbVariantDto;
import com.marketinghub.experiment.salespageab.dto.UpdateExperimentSalesPageAbVariantRequest;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.salespageab.ExperimentSalesPageAbTestRepository;
import com.marketinghub.repository.jpa.experiment.salespageab.ExperimentSalesPageAbVariantRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: criar e manter testes A/B de pagina de venda ligados ao experimento. */
@Service
public class ExperimentSalesPageAbTestService {
    private static final BigDecimal HALF_TRAFFIC = new BigDecimal("50.00");
    private static final List<ExperimentSalesPageAbTestStatus> ACTIVE_STATUSES = List.of(
            ExperimentSalesPageAbTestStatus.DRAFT,
            ExperimentSalesPageAbTestStatus.READY,
            ExperimentSalesPageAbTestStatus.RUNNING);

    private final ExperimentSalesPageAbTestRepository testRepository;
    private final ExperimentSalesPageAbVariantRepository variantRepository;
    private final ExperimentRepository experimentRepository;
    private final GeraSalesPagePublicationAuditRepository publicationAuditRepository;
    private final ExperimentVideoAssetRepository videoAssetRepository;

    /** Inicializa o servico com as fontes canonicas de experimento, publicacao e video. */
    public ExperimentSalesPageAbTestService(ExperimentSalesPageAbTestRepository testRepository,
                                            ExperimentSalesPageAbVariantRepository variantRepository,
                                            ExperimentRepository experimentRepository,
                                            GeraSalesPagePublicationAuditRepository publicationAuditRepository,
                                            ExperimentVideoAssetRepository videoAssetRepository) {
        this.testRepository = testRepository;
        this.variantRepository = variantRepository;
        this.experimentRepository = experimentRepository;
        this.publicationAuditRepository = publicationAuditRepository;
        this.videoAssetRepository = videoAssetRepository;
    }

    /** Cria o plano padrao recomendado para Meta: pagina tradicional contra pagina com video humano. */
    @Transactional
    public ExperimentSalesPageAbTestDto createMetaVideoVsTraditional(Long experimentId) {
        Experiment experiment = findExperiment(experimentId);
        ExperimentSalesPageAbTest test = ExperimentSalesPageAbTest.builder()
                .experiment(experiment)
                .name("A/B Meta - pagina tradicional vs video humano")
                .status(ExperimentSalesPageAbTestStatus.DRAFT)
                .hypothesis("Video humano curto antes da prova visual aumenta confianca e clique no checkout sem alterar oferta, preco, publico ou criativo.")
                .primaryMetric("checkout_click_rate")
                .secondaryMetrics("purchase_rate,cost_per_checkout_click,cost_per_purchase,video_50_percent")
                .winnerRule("Vencer a variante com melhor custo por checkout_click; confirmar por purchase quando houver volume suficiente.")
                .minimumRuntimeDays(7)
                .minimumSampleSize(100)
                .metaSplitTestRecommended(true)
                .notes("Manter oferta, preco, checkout, publico, criativos e orcamento equivalentes. Testar apenas a presenca do video humano.")
                .build();
        test.getVariants().add(buildVariant(test, "A", "Pagina tradicional", ExperimentSalesPageAbVariantType.TRADITIONAL));
        test.getVariants().add(buildVariant(test, "B", "Pagina com video humano", ExperimentSalesPageAbVariantType.HUMAN_VIDEO));
        return toDto(testRepository.save(test));
    }

    /** Lista os planos A/B cadastrados para um experimento. */
    @Transactional(readOnly = true)
    public List<ExperimentSalesPageAbTestDto> list(Long experimentId) {
        findExperiment(experimentId);
        return testRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Retorna o teste aberto mais recente para validar prontidao e informar workers de campanha. */
    @Transactional(readOnly = true)
    public Optional<ExperimentSalesPageAbTestDto> findActiveForCampaign(Long experimentId) {
        return testRepository.findTopByExperimentIdAndStatusInOrderByUpdatedAtDesc(experimentId, ACTIVE_STATUSES)
                .map(this::toDto);
    }

    /** Atualiza uma variante com URLs, vinculos de auditoria e status de prontidao. */
    @Transactional
    public ExperimentSalesPageAbTestDto updateVariant(
            Long experimentId,
            Long variantId,
            UpdateExperimentSalesPageAbVariantRequest request) {
        findExperiment(experimentId);
        ExperimentSalesPageAbVariant variant = variantRepository.findByIdAndTestExperimentId(variantId, experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ab test variant not found"));
        applyVariantUpdate(variant, request, experimentId);
        normalizeTestStatus(variant.getTest());
        variantRepository.save(variant);
        return toDto(variant.getTest());
    }

    /** Verifica se o teste A/B ativo possui duas variantes prontas e equivalentes para campanha. */
    @Transactional(readOnly = true)
    public boolean hasReadyActiveTest(Long experimentId) {
        return findActiveForCampaign(experimentId)
                .map(test -> test.variants().size() == 2
                        && test.variants().stream().allMatch(this::isVariantReadyForTraffic))
                .orElse(true);
    }

    /** Cria uma variante inicial com divisao de trafego 50/50. */
    private ExperimentSalesPageAbVariant buildVariant(
            ExperimentSalesPageAbTest test,
            String key,
            String name,
            ExperimentSalesPageAbVariantType type) {
        return ExperimentSalesPageAbVariant.builder()
                .test(test)
                .variantKey(key)
                .name(name)
                .variantType(type)
                .status(ExperimentSalesPageAbVariantStatus.DRAFT)
                .trafficWeight(HALF_TRAFFIC)
                .analyticsVariantParam("ab=" + key.toLowerCase())
                .requiredCollectorsPresent(false)
                .build();
    }

    /** Aplica somente os campos enviados na variante. */
    private void applyVariantUpdate(
            ExperimentSalesPageAbVariant variant,
            UpdateExperimentSalesPageAbVariantRequest request,
            Long experimentId) {
        if (request == null) {
            return;
        }
        if (request.status() != null) {
            variant.setStatus(request.status());
        }
        if (request.trafficWeight() != null) {
            variant.setTrafficWeight(request.trafficWeight());
        }
        if (request.salesPageUrl() != null) {
            variant.setSalesPageUrl(request.salesPageUrl());
        }
        if (request.checkoutUrl() != null) {
            variant.setCheckoutUrl(request.checkoutUrl());
        }
        if (request.adDestinationUrl() != null) {
            variant.setAdDestinationUrl(request.adDestinationUrl());
        }
        if (request.analyticsVariantParam() != null) {
            variant.setAnalyticsVariantParam(request.analyticsVariantParam());
        }
        if (request.requiredCollectorsPresent() != null) {
            variant.setRequiredCollectorsPresent(request.requiredCollectorsPresent());
        }
        if (request.publicationAuditId() != null) {
            variant.setPublicationAudit(resolvePublicationAudit(request.publicationAuditId(), experimentId));
        }
        if (request.experimentVideoAssetId() != null) {
            variant.setExperimentVideoAsset(resolveVideoAsset(request.experimentVideoAssetId(), experimentId));
        }
    }

    /** Promove o teste para READY apenas quando as duas variantes estao prontas para trafego. */
    private void normalizeTestStatus(ExperimentSalesPageAbTest test) {
        boolean ready = test.getVariants() != null
                && test.getVariants().size() == 2
                && test.getVariants().stream().allMatch(variant -> isVariantReadyForTraffic(toVariantDto(variant)));
        if (ready && test.getStatus() == ExperimentSalesPageAbTestStatus.DRAFT) {
            test.setStatus(ExperimentSalesPageAbTestStatus.READY);
        }
    }

    /** Busca o experimento alvo ou retorna erro HTTP claro. */
    private Experiment findExperiment(Long experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found"));
    }

    /** Busca a auditoria de publicacao e impede vinculo com outro experimento. */
    private GeraSalesPagePublicationAudit resolvePublicationAudit(Long auditId, Long experimentId) {
        GeraSalesPagePublicationAudit audit = publicationAuditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "publication audit not found"));
        if (!experimentId.equals(audit.getExperimentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "publication audit belongs to another experiment");
        }
        return audit;
    }

    /** Busca o video e impede vinculo com outro experimento. */
    private ExperimentVideoAsset resolveVideoAsset(Long videoAssetId, Long experimentId) {
        ExperimentVideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "video asset not found"));
        Long assetExperimentId = asset.getExperiment() != null ? asset.getExperiment().getId() : null;
        if (!experimentId.equals(assetExperimentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "video asset belongs to another experiment");
        }
        return asset;
    }

    /** Confirma se a variante pode receber trafego pago. */
    private boolean isVariantReadyForTraffic(ExperimentSalesPageAbVariantDto variant) {
        return variant.status() == ExperimentSalesPageAbVariantStatus.READY
                && StringUtils.hasText(variant.salesPageUrl())
                && StringUtils.hasText(variant.adDestinationUrl())
                && variant.salesPageUrl().equals(variant.adDestinationUrl())
                && StringUtils.hasText(variant.checkoutUrl())
                && variant.requiredCollectorsPresent()
                && variant.trafficWeight() != null
                && variant.trafficWeight().signum() > 0;
    }

    /** Converte o teste para contrato de API com variantes ordenadas. */
    private ExperimentSalesPageAbTestDto toDto(ExperimentSalesPageAbTest test) {
        List<ExperimentSalesPageAbVariantDto> variants = test.getVariants() == null
                ? List.of()
                : test.getVariants().stream()
                .sorted(Comparator.comparing(ExperimentSalesPageAbVariant::getVariantKey))
                .map(this::toVariantDto)
                .toList();
        return new ExperimentSalesPageAbTestDto(
                test.getId(),
                test.getExperiment() != null ? test.getExperiment().getId() : null,
                test.getName(),
                test.getStatus(),
                test.getHypothesis(),
                test.getPrimaryMetric(),
                test.getSecondaryMetrics(),
                test.getWinnerRule(),
                test.getMinimumRuntimeDays(),
                test.getMinimumSampleSize(),
                test.isMetaSplitTestRecommended(),
                test.getNotes(),
                variants,
                test.getCreatedAt(),
                test.getUpdatedAt());
    }

    /** Converte a variante para contrato de API. */
    private ExperimentSalesPageAbVariantDto toVariantDto(ExperimentSalesPageAbVariant variant) {
        return new ExperimentSalesPageAbVariantDto(
                variant.getId(),
                variant.getVariantKey(),
                variant.getName(),
                variant.getVariantType(),
                variant.getStatus(),
                variant.getTrafficWeight(),
                variant.getSalesPageUrl(),
                variant.getCheckoutUrl(),
                variant.getAdDestinationUrl(),
                variant.getAnalyticsVariantParam(),
                variant.getPublicationAudit() != null ? variant.getPublicationAudit().getId() : null,
                variant.getExperimentVideoAsset() != null ? variant.getExperimentVideoAsset().getId() : null,
                variant.isRequiredCollectorsPresent(),
                variant.getCreatedAt(),
                variant.getUpdatedAt());
    }
}
