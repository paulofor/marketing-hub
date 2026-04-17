package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.dto.BackendMdsRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MechanismDiscoveryPipelineServiceTest {
    @Mock
    private BackendMdsClient backendMdsClient;

    @InjectMocks
    private MechanismDiscoveryPipelineService service;

    @Test
    void shouldPublishAtLeastTwoArtifacts() {
        BackendMdsRequestDto request = new BackendMdsRequestDto(
                10L,
                "IN_PROGRESS",
                "weight-loss",
                "plateau",
                "consistent fat loss",
                "corr-123"
        );

        service.execute(request);

        ArgumentCaptor<com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto> captor =
                ArgumentCaptor.forClass(com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.class);
        verify(backendMdsClient).publishBatch(captor.capture());

        assertThat(captor.getValue().requestId()).isEqualTo(10L);
        assertThat(captor.getValue().artifacts()).hasSizeGreaterThanOrEqualTo(2);
    }
}
