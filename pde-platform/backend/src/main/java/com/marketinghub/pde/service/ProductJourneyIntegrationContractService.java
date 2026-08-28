package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.ProductJourneyIntegrationContractResponse;
import org.springframework.stereotype.Service;

/** Responsabilidade: montar o contrato público de integração da jornada comercial do PDE. */
@Service
public class ProductJourneyIntegrationContractService {
    private final ProductCatalogService productCatalogService;

    /** Recebe o catálogo que comprova a existência e a versão vigente do produto. */
    public ProductJourneyIntegrationContractService(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    /** Expõe somente rotas e eventos existentes, sem executar pagamento, acesso ou publicação. */
    public ProductJourneyIntegrationContractResponse get(String productSlug) {
        ProductExperienceResponse product = productCatalogService.getProduct(productSlug);
        if (!FunnelEventCatalog.supportsRequiredCommercialJourney()) {
            throw new IllegalStateException("Catálogo PDE não suporta a jornada comercial obrigatória");
        }
        return new ProductJourneyIntegrationContractResponse(
                product.slug(),
                product.experienceVersion(),
                FunnelEventCatalog.CONTRACT_VERSION,
                "/api/pde/access/events",
                "/api/pde/access/analytics/{productSlug}/summary",
                "/api/pde/access/login-link",
                "/api/pde/access/{token}/workspace",
                "/api/pde/access/{token}/missions/{missionId}/complete",
                FunnelEventCatalog.REQUIRED_COMMERCIAL_JOURNEY_EVENTS,
                java.util.List.of(
                        "eventId",
                        "productSlug",
                        "experienceVersion",
                        "sessionId",
                        "visitorId",
                        "accessToken"),
                "pde_funnel_event",
                "trafficQuality=INTERNAL_QA ou mh_test identifica dados de homologação");
    }
}
