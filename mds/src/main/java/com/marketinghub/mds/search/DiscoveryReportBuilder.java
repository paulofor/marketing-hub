package com.marketinghub.mds.search;

import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DiscoveryReportBuilder {

    public ArtifactPayloadDto build(Long requestId,
                                    String question,
                                    int rawCount,
                                    int dedupedCount,
                                    int screenedCount,
                                    int evidenceItemCount,
                                    int mechanismCandidateCount,
                                    Long mechanismSpecArtifactId,
                                    Long practicalKnowledgePackArtifactId,
                                    List<Long> parentArtifactIds) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("requestId", requestId);
        content.put("question", question);
        content.put("status", "SUCCESS");
        content.put("summary", "Fluxo completo da descoberta executado com publicação final de artefatos.");
        content.put("metrics", Map.of(
                "rawResultCount", rawCount,
                "dedupedResultCount", dedupedCount,
                "screenedResultCount", screenedCount,
                "evidenceItemCount", evidenceItemCount,
                "mechanismCandidateCount", mechanismCandidateCount
        ));
        content.put("output", Map.of(
                "mechanismSpecArtifactId", mechanismSpecArtifactId,
                "practicalKnowledgePackArtifactId", practicalKnowledgePackArtifactId
        ));

        return new ArtifactPayloadDto(
                "mechanismDiscoveryReport",
                "v1",
                "v1",
                "DRAFT",
                "mds",
                "mds",
                content,
                null,
                parentArtifactIds
        );
    }
}
