package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.dto.BackendMdsRequestDto;
import com.marketinghub.mds.search.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MechanismDiscoveryPipelineServiceTest {
    @Mock
    private BackendMdsClient backendMdsClient;

    @Mock
    private MechanismQuestionBuilder mechanismQuestionBuilder;

    @Mock
    private SearchQueryPlanBuilder searchQueryPlanBuilder;

    @Mock
    private SearchExecutionService searchExecutionService;

    @InjectMocks
    private MechanismDiscoveryPipelineService service;

    @Test
    void shouldPublishEvidenceSearchAndSourceDocuments() {
        BackendMdsRequestDto request = new BackendMdsRequestDto(
                10L,
                "IN_PROGRESS",
                "weight-loss",
                "plateau",
                "consistent fat loss",
                "corr-123"
        );
        when(mechanismQuestionBuilder.build(request)).thenReturn(new MechanismQuestion("q1"));
        when(searchQueryPlanBuilder.buildPlans(new MechanismQuestion("q1")))
                .thenReturn(List.of(new SearchQueryPlan("pubmed", "q1", 10)));
        when(searchExecutionService.execute(List.of(new SearchQueryPlan("pubmed", "q1", 10))))
                .thenReturn(List.of(new SourceSearchHit(
                        "pubmed",
                        "pubmed:1",
                        null,
                        "title",
                        "abstract",
                        "https://pubmed.ncbi.nlm.nih.gov/1/",
                        "2024",
                        "CC-BY",
                        true,
                        true,
                        false,
                        List.of("A Author")
                )));

        service.execute(request);

        ArgumentCaptor<com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto> artifactCaptor =
                ArgumentCaptor.forClass(com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.class);
        verify(backendMdsClient).publishBatch(artifactCaptor.capture());

        assertThat(artifactCaptor.getValue().requestId()).isEqualTo(10L);
        assertThat(artifactCaptor.getValue().artifacts()).hasSize(2);
        assertThat(artifactCaptor.getValue().artifacts().get(0).artifactType()).isEqualTo("mechanismEvidenceSearch");
        assertThat(artifactCaptor.getValue().artifacts().get(1).artifactType()).isEqualTo("sourceDocument");

        ArgumentCaptor<com.marketinghub.mds.dto.BackendSourceAccessPublishBatchRequestDto> sourceAccessCaptor =
                ArgumentCaptor.forClass(com.marketinghub.mds.dto.BackendSourceAccessPublishBatchRequestDto.class);
        verify(backendMdsClient).publishSourceAccessBatch(sourceAccessCaptor.capture());
        assertThat(sourceAccessCaptor.getValue().records()).hasSize(1);
    }
}
