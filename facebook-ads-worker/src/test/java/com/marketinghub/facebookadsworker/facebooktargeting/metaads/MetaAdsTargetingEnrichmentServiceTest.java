package com.marketinghub.facebookadsworker.facebooktargeting.metaads;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do enriquecimento de elementos de targeting com dados oficiais da Meta Ads.
 */
class MetaAdsTargetingEnrichmentServiceTest {
    /**
     * Deve reportar ausência de ID oficial ao backend quando a Meta não retorna nenhum resultado útil.
     */
    @Test
    void processPendingElementsMarksUnavailableWhenMetaReturnsNoMatch() {
        TargetingBackendClient backendClient = mock(TargetingBackendClient.class);
        FacebookAdsService facebookAdsService = mock(FacebookAdsService.class);
        when(backendClient.listMetaAdsPendingElements(100)).thenReturn(List.of(
                new TargetingBackendClient.MetaAdsPendingElementPayload(31L, 8L, "BEHAVIOR", "Small business owners")
        ));
        when(facebookAdsService.searchGlobalTargetingOptions(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Collections.emptyList());
        MetaAdsTargetingEnrichmentService service = new MetaAdsTargetingEnrichmentService(
                backendClient,
                facebookAdsService,
                "act_123",
                100
        );

        service.processPendingElements();

        verify(backendClient).markMetaAdsIdUnavailable(eq(31L), contains("Small business owners"));
    }
}
