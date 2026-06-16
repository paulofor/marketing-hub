package com.marketinghub.facebookadsworker.facebooktargeting.metaads;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

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
        when(facebookAdsService.searchGlobalTargetingOptions(any()))
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

    /**
     * Deve consultar comportamentos pela categoria oficial de targeting para localizar Small business owners.
     */
    @Test
    void processPendingElementsSearchesBehaviorsAsTargetingCategory() {
        TargetingBackendClient backendClient = mock(TargetingBackendClient.class);
        FacebookAdsService facebookAdsService = mock(FacebookAdsService.class);
        when(backendClient.listMetaAdsPendingElements(100)).thenReturn(List.of(
                new TargetingBackendClient.MetaAdsPendingElementPayload(31L, 8L, "BEHAVIOR", "Small business owners")
        ));
        when(facebookAdsService.searchGlobalTargetingOptions(any()))
                .thenReturn(List.of(new FacebookAdsService.FacebookTargetingSearchResult(
                        "6002714898572",
                        "Small business owners",
                        "behaviors",
                        "People who list themselves as small business owners",
                        48708955L,
                        57281732L,
                        List.of("Digital activities", "Small business owners"))));
        MetaAdsTargetingEnrichmentService service = new MetaAdsTargetingEnrichmentService(
                backendClient,
                facebookAdsService,
                "act_123",
                100
        );

        service.processPendingElements();

        org.mockito.ArgumentCaptor<FacebookAdsService.TargetingSearchRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(FacebookAdsService.TargetingSearchRequest.class);
        verify(facebookAdsService).searchGlobalTargetingOptions(requestCaptor.capture());
        assertThat(requestCaptor.getValue().type())
                .isEqualTo(FacebookAdsService.TargetingSearchType.AD_TARGETING_CATEGORY_BEHAVIOR);
        verify(backendClient).updateMetaAdsData(
                eq(31L),
                eq(new TargetingBackendClient.MetaAdsUpdatePayload(
                        "6002714898572",
                        "Small business owners",
                        48708955L,
                        57281732L)));
    }
}
