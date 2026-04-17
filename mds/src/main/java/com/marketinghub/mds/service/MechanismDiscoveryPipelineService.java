package com.marketinghub.mds.service;

import com.marketinghub.mds.client.BackendMdsClient;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto;
import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto;
import com.marketinghub.mds.dto.BackendMdsRequestDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MechanismDiscoveryPipelineService {
    private final BackendMdsClient backendMdsClient;

    public MechanismDiscoveryPipelineService(BackendMdsClient backendMdsClient) {
        this.backendMdsClient = backendMdsClient;
    }

    public void execute(BackendMdsRequestDto request) {
        ArtifactPayloadDto mechanismSpec = new ArtifactPayloadDto(
                "mechanismSpec",
                "v1",
                "v1",
                "DRAFT",
                "mds",
                "mds",
                Map.of(
                        "requestId", request.id(),
                        "market", request.market(),
                        "problem", request.problem(),
                        "desiredOutcome", request.desiredOutcome(),
                        "summary", "Stub inicial do mecanismo recomendado para Sprint inicial do modulo independente"
                ),
                null,
                List.of()
        );

        ArtifactPayloadDto knowledgePack = new ArtifactPayloadDto(
                "practicalKnowledgePack",
                "v1",
                "v1",
                "DRAFT",
                "mds",
                "mds",
                Map.of(
                        "requestId", request.id(),
                        "notes", List.of("Pipeline inicial sem acesso direto ao banco.", "Persistencia via backend interno.")
                ),
                null,
                List.of()
        );

        backendMdsClient.publishBatch(new BackendArtifactPublishBatchRequestDto(
                request.id(),
                List.of(mechanismSpec, knowledgePack)
        ));
    }
}
