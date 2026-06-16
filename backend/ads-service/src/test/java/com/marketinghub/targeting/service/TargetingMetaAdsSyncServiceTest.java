package com.marketinghub.targeting.service;

import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.dto.UpdateTargetingMetaAdsDataRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes da sincronização de dados oficiais da Meta Ads em elementos de segmentação.
 */
@ExtendWith(MockitoExtension.class)
class TargetingMetaAdsSyncServiceTest {
    @Mock
    private TargetingElementRepository repository;

    /**
     * Deve marcar elemento sem ID oficial da Meta e limpar dados parciais para impedir nova tentativa automática.
     */
    @Test
    void updateMetaAdsDataMarksMetaIdUnavailableAndClearsPartialData() {
        TargetingElement element = TargetingElement.builder()
                .metaId("123")
                .metaKey("Antigo")
                .metaAudienceSizeLowerBound(10L)
                .metaAudienceSizeUpperBound(20L)
                .build();
        when(repository.findById(7L)).thenReturn(Optional.of(element));
        TargetingMetaAdsSyncService service = new TargetingMetaAdsSyncService(repository);

        service.updateMetaAdsData(7L, new UpdateTargetingMetaAdsDataRequest(null, null, null, null, true, "sem match"));

        assertThat(element.getMetaId()).isNull();
        assertThat(element.getMetaKey()).isNull();
        assertThat(element.getMetaAudienceSizeLowerBound()).isNull();
        assertThat(element.getMetaAudienceSizeUpperBound()).isNull();
        assertThat(element.getMetaIdUnavailable()).isTrue();
        assertThat(element.getMetaIdUnavailableReason()).isEqualTo("sem match");
    }

    /**
     * Deve liberar o bloqueio de ausência de ID quando um ID oficial da Meta for persistido.
     */
    @Test
    void updateMetaAdsDataClearsUnavailableFlagWhenMetaIdIsSaved() {
        TargetingElement element = TargetingElement.builder()
                .metaIdUnavailable(true)
                .metaIdUnavailableReason("sem match")
                .build();
        when(repository.findById(9L)).thenReturn(Optional.of(element));
        TargetingMetaAdsSyncService service = new TargetingMetaAdsSyncService(repository);

        service.updateMetaAdsData(9L, new UpdateTargetingMetaAdsDataRequest("456", "Interesse", 100L, 200L, null, null));

        assertThat(element.getMetaId()).isEqualTo("456");
        assertThat(element.getMetaKey()).isEqualTo("Interesse");
        assertThat(element.getMetaAudienceSizeLowerBound()).isEqualTo(100L);
        assertThat(element.getMetaAudienceSizeUpperBound()).isEqualTo(200L);
        assertThat(element.getMetaIdUnavailable()).isFalse();
        assertThat(element.getMetaIdUnavailableReason()).isNull();
    }
}
