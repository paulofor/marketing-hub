package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Coordena leitura, escrita e contratos do backend para descoberta de produtos PDE. */
@Service
public class ProductDiscoveryService {

    private static final String PIPELINE_CODE = "productdiscovery.v1";
    private static final String STAGE_CODE = "research";
    private final ProductDiscoveryCycleRepository cycleRepository;
    private final ProductDiscoveryOpportunityRepository opportunityRepository;

    /** Inicializa o serviço com repositórios canônicos do módulo. */
    public ProductDiscoveryService(
            ProductDiscoveryCycleRepository cycleRepository,
            ProductDiscoveryOpportunityRepository opportunityRepository) {
        this.cycleRepository = cycleRepository;
        this.opportunityRepository = opportunityRepository;
    }

    /** Cria um ciclo pronto para o worker pesquisar dores e lacunas na internet. */
    @Transactional
    public ProductDiscoveryCycleResponse createCycle(CreateProductDiscoveryCycleRequest request) {
        ProductDiscoveryCycle cycle = new ProductDiscoveryCycle();
        cycle.setTheme(requiredText(request.theme(), "theme"));
        cycle.setTargetAudience(optionalText(request.targetAudience()));
        cycle.setCountry(defaultText(request.country(), "BR"));
        cycle.setLanguage(defaultText(request.language(), "pt-BR"));
        cycle.setAcquisitionChannel(optionalText(request.acquisitionChannel()));
        cycle.setCommercialConstraints(optionalText(request.commercialConstraints()));
        cycle.setForbiddenCategories(optionalText(request.forbiddenCategories()));
        cycle.setObjective(optionalText(request.objective()));
        cycle.setStatus(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
        cycle.setStageCode(STAGE_CODE);
        return toCycleResponse(cycleRepository.save(cycle));
    }

    /** Lista ciclos recentes para acompanhamento administrativo. */
    @Transactional(readOnly = true)
    public List<ProductDiscoveryCycleResponse> listCycles() {
        return cycleRepository.findTop50ByOrderByUpdatedAtDesc().stream()
                .map(this::toCycleResponse)
                .toList();
    }

    /** Busca um ciclo com oportunidades e evidências para a tela de decisão. */
    @Transactional(readOnly = true)
    public ProductDiscoveryCycleDetailResponse getCycle(Long cycleId) {
        ProductDiscoveryCycle cycle = findCycle(cycleId);
        List<ProductDiscoveryOpportunityResponse> opportunities =
                opportunityRepository.findAllByCycleIdOrderByScoreDesc(cycleId).stream()
                        .map(this::toOpportunityResponse)
                        .toList();
        return new ProductDiscoveryCycleDetailResponse(toCycleResponse(cycle), opportunities);
    }

    /** Entrega pendências ao worker e marca ciclos como em pesquisa para evitar consumo duplicado. */
    @Transactional
    public List<ProductDiscoveryPendingResponse> pending() {
        return cycleRepository
                .findTop5ByStatusInOrderByUpdatedAtAsc(List.of(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH))
                .stream()
                .map(cycle -> {
                    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
                    cycle.setStageCode(STAGE_CODE);
                    cycle.setErrorMessage(null);
                    return toPendingResponse(cycleRepository.save(cycle));
                })
                .toList();
    }

    /** Registra resultado funcional do worker e conclui o ciclo com ranking auditável. */
    @Transactional
    public ProductDiscoveryCycleDetailResponse complete(Long cycleId, ProductDiscoveryResultRequest request) {
        ProductDiscoveryCycle cycle = findCycle(cycleId);
        opportunityRepository.deleteAllByCycleId(cycleId);
        for (ProductDiscoveryOpportunityResultRequest item : request.opportunities()) {
            ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
            opportunity.setCycle(cycle);
            opportunity.setName(requiredText(item.name(), "name"));
            opportunity.setPrimaryAudience(requiredText(item.primaryAudience(), "primaryAudience"));
            opportunity.setRootPain(requiredText(item.rootPain(), "rootPain"));
            opportunity.setPracticalPain(optionalText(item.practicalPain()));
            opportunity.setEmotionalPain(optionalText(item.emotionalPain()));
            opportunity.setScaleEvidence(optionalText(item.scaleEvidence()));
            opportunity.setUnmetnessEvidence(optionalText(item.unmetnessEvidence()));
            opportunity.setPdeExperience(optionalText(item.pdeExperience()));
            opportunity.setFirstCampaignAngle(optionalText(item.firstCampaignAngle()));
            opportunity.setCommercialRisk(optionalText(item.commercialRisk()));
            opportunity.setEvidenceJson(optionalText(item.evidenceJson()));
            opportunity.setScore(item.score());
            opportunity.setDecision(item.decision());
            opportunityRepository.save(opportunity);
        }
        cycle.setDecisionSummary(requiredText(request.decisionSummary(), "decisionSummary"));
        cycle.setStatus(ProductDiscoveryCycleStatus.COMPLETED);
        cycle.setStageCode("opportunity-gate");
        cycle.setErrorMessage(null);
        cycleRepository.save(cycle);
        return getCycle(cycleId);
    }

    /** Registra falha operacional do worker preservando a causa para o usuário. */
    @Transactional
    public ProductDiscoveryCycleResponse fail(Long cycleId, ProductDiscoveryFailureRequest request) {
        ProductDiscoveryCycle cycle = findCycle(cycleId);
        cycle.setStatus(ProductDiscoveryCycleStatus.FAILED);
        cycle.setErrorMessage(requiredText(request.errorMessage(), "errorMessage"));
        return toCycleResponse(cycleRepository.save(cycle));
    }

    /** Converte entidade de ciclo para resposta. */
    private ProductDiscoveryCycleResponse toCycleResponse(ProductDiscoveryCycle cycle) {
        return new ProductDiscoveryCycleResponse(
                cycle.getId(),
                cycle.getTheme(),
                cycle.getTargetAudience(),
                cycle.getCountry(),
                cycle.getLanguage(),
                cycle.getAcquisitionChannel(),
                cycle.getStatus(),
                cycle.getStageCode(),
                cycle.getDecisionSummary(),
                cycle.getErrorMessage(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt());
    }

    /** Converte entidade de oportunidade para resposta. */
    private ProductDiscoveryOpportunityResponse toOpportunityResponse(ProductDiscoveryOpportunity opportunity) {
        return new ProductDiscoveryOpportunityResponse(
                opportunity.getId(),
                opportunity.getCycle().getId(),
                opportunity.getName(),
                opportunity.getPrimaryAudience(),
                opportunity.getRootPain(),
                opportunity.getPracticalPain(),
                opportunity.getEmotionalPain(),
                opportunity.getScaleEvidence(),
                opportunity.getUnmetnessEvidence(),
                opportunity.getPdeExperience(),
                opportunity.getFirstCampaignAngle(),
                opportunity.getCommercialRisk(),
                opportunity.getEvidenceJson(),
                opportunity.getScore(),
                opportunity.getDecision(),
                opportunity.getCreatedAt(),
                opportunity.getUpdatedAt());
    }

    /** Converte ciclo para contrato de pendência do worker. */
    private ProductDiscoveryPendingResponse toPendingResponse(ProductDiscoveryCycle cycle) {
        return new ProductDiscoveryPendingResponse(
                cycle.getId(),
                PIPELINE_CODE,
                STAGE_CODE,
                cycle.getTheme(),
                cycle.getTargetAudience(),
                cycle.getCountry(),
                cycle.getLanguage(),
                cycle.getAcquisitionChannel(),
                cycle.getCommercialConstraints(),
                cycle.getForbiddenCategories(),
                cycle.getObjective());
    }

    /** Busca ciclo por id ou responde 404. */
    private ProductDiscoveryCycle findCycle(Long cycleId) {
        return cycleRepository
                .findById(cycleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciclo de descoberta não encontrado"));
    }

    /** Normaliza texto obrigatório. */
    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " é obrigatório");
        }
        return value.trim();
    }

    /** Normaliza texto opcional. */
    private String optionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Aplica valor padrão para texto opcional ausente. */
    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
