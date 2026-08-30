package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.dto.ProductExperienceResponse;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o contrato que prepara checkout, acesso, eventos e auditoria PDE. */
class ProductJourneyIntegrationContractServiceTest {

    /** Deriva identidade e versão do catálogo sem executar pagamento nem publicar tráfego. */
    @Test
    void exposesImplementedCommercialJourneyContract() {
        ProductCatalogService catalog = mock(ProductCatalogService.class);
        ProductExperienceResponse product = mock(ProductExperienceResponse.class);
        when(product.slug()).thenReturn("kit-whatsapp-pronto");
        when(product.experienceVersion()).thenReturn("kit-whatsapp-pronto-pde-v2");
        when(catalog.getProduct("kit-whatsapp-pronto")).thenReturn(product);
        ProductJourneyIntegrationContractService service =
                new ProductJourneyIntegrationContractService(catalog);

        var contract = service.get("kit-whatsapp-pronto");

        assertThat(contract.productSlug()).isEqualTo("kit-whatsapp-pronto");
        assertThat(contract.experienceVersion()).isEqualTo("kit-whatsapp-pronto-pde-v2");
        assertThat(contract.contractVersion()).isEqualTo("PDE_COMMERCIAL_JOURNEY_EVENTS_V1");
        assertThat(contract.eventsPath()).isEqualTo("/api/pde/access/events");
        assertThat(contract.requiredEventTypes())
                .contains("PAGE_VIEW", "CHECKOUT_STARTED", "PURCHASE_COMPLETED", "ACCESS_RELEASED", "FIRST_USE");
        assertThat(contract.correlationKeys())
                .contains("eventId", "sessionId", "visitorId", "accessReferenceHash")
                .doesNotContain("accessToken");
        assertThat(contract.workspacePathTemplate()).isEqualTo("/api/pde/access/workspace");
        assertThat(contract.missionCompletionPathTemplate())
                .isEqualTo("/api/pde/access/missions/{missionId}/complete");
        assertThat(contract.sourceOfTruth()).isEqualTo("pde_funnel_event");
        assertThat(contract.testTrafficPolicy()).contains("INTERNAL_QA");
    }
}
