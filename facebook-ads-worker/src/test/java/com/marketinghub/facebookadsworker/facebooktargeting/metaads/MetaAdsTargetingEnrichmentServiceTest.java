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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    /**
     * Deve ignorar marca apenas relacionada no locale inicial e usar a correspondência oficial exata no locale seguinte.
     */
    @Test
    void processPendingElementsUsesExactCrossLocaleMatchInsteadOfFirstRelatedBrand() {
        TargetingBackendClient backendClient = mock(TargetingBackendClient.class);
        FacebookAdsService facebookAdsService = mock(FacebookAdsService.class);
        when(backendClient.listMetaAdsPendingElements(100)).thenReturn(List.of(
                new TargetingBackendClient.MetaAdsPendingElementPayload(397L, 34L, "INTEREST", "Skin care")
        ));
        when(facebookAdsService.searchGlobalTargetingOptions(any()))
                .thenReturn(
                        List.of(new FacebookAdsService.FacebookTargetingSearchResult(
                                "6003657105838",
                                "Mario Badescu Skin Care",
                                "interests",
                                "Fitness and wellness",
                                681784L,
                                801778L,
                                List.of("Interesses", "Mario Badescu Skin Care"))),
                        List.of(new FacebookAdsService.FacebookTargetingSearchResult(
                                "664130153728886",
                                "Skin care",
                                "interests",
                                null,
                                257137942L,
                                302394220L,
                                List.of("Interests", "Skin care"))));
        MetaAdsTargetingEnrichmentService service = new MetaAdsTargetingEnrichmentService(
                backendClient,
                facebookAdsService,
                "act_123",
                100
        );

        service.processPendingElements();

        org.mockito.ArgumentCaptor<FacebookAdsService.TargetingSearchRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(FacebookAdsService.TargetingSearchRequest.class);
        verify(facebookAdsService, times(2)).searchGlobalTargetingOptions(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(FacebookAdsService.TargetingSearchRequest::locale)
                .containsExactly("pt_BR", "en_US");
        verify(backendClient).updateMetaAdsData(
                eq(397L),
                eq(new TargetingBackendClient.MetaAdsUpdatePayload(
                        "664130153728886",
                        "Skin care",
                        257137942L,
                        302394220L)));
    }

    /** Deve aceitar o nome oficial quando a única diferença for um qualificador de categoria entre parênteses. */
    @Test
    void processPendingElementsAcceptsOfficialTrailingCategoryQualifier() {
        TargetingBackendClient backendClient = mock(TargetingBackendClient.class);
        FacebookAdsService facebookAdsService = mock(FacebookAdsService.class);
        when(backendClient.listMetaAdsPendingElements(100)).thenReturn(List.of(
                new TargetingBackendClient.MetaAdsPendingElementPayload(398L, 34L, "INTEREST", "Cuidados com a pele")
        ));
        when(facebookAdsService.searchGlobalTargetingOptions(any()))
                .thenReturn(List.of(new FacebookAdsService.FacebookTargetingSearchResult(
                        "664130153728886",
                        "Cuidados com a pele (cosméticos)",
                        "interests",
                        null,
                        257137942L,
                        302394220L,
                        List.of("Interesses", "Cuidados com a pele (cosméticos)"))));
        MetaAdsTargetingEnrichmentService service = new MetaAdsTargetingEnrichmentService(
                backendClient,
                facebookAdsService,
                "act_123",
                100
        );

        service.processPendingElements();

        verify(facebookAdsService).searchGlobalTargetingOptions(any());
        verify(backendClient).updateMetaAdsData(
                eq(398L),
                eq(new TargetingBackendClient.MetaAdsUpdatePayload(
                        "664130153728886",
                        "Cuidados com a pele (cosméticos)",
                        257137942L,
                        302394220L)));
    }

    /** Deve recusar todos os resultados apenas relacionados quando nenhum nome oficial corresponde ao termo. */
    @Test
    void processPendingElementsMarksUnavailableInsteadOfFallingBackToRelatedResult() {
        TargetingBackendClient backendClient = mock(TargetingBackendClient.class);
        FacebookAdsService facebookAdsService = mock(FacebookAdsService.class);
        when(backendClient.listMetaAdsPendingElements(100)).thenReturn(List.of(
                new TargetingBackendClient.MetaAdsPendingElementPayload(399L, 34L, "INTEREST", "Skin care")
        ));
        when(facebookAdsService.searchGlobalTargetingOptions(any()))
                .thenReturn(List.of(new FacebookAdsService.FacebookTargetingSearchResult(
                        "6003657105838",
                        "Mario Badescu Skin Care",
                        "interests",
                        null,
                        681784L,
                        801778L,
                        List.of("Interests", "Mario Badescu Skin Care"))));
        MetaAdsTargetingEnrichmentService service = new MetaAdsTargetingEnrichmentService(
                backendClient,
                facebookAdsService,
                "act_123",
                100
        );

        service.processPendingElements();

        verify(facebookAdsService, times(3)).searchGlobalTargetingOptions(any());
        verify(backendClient, never()).updateMetaAdsData(eq(399L), any());
        verify(backendClient).markMetaAdsIdUnavailable(eq(399L), contains("Skin care"));
    }
}
