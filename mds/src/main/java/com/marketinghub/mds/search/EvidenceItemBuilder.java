package com.marketinghub.mds.search;

import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvidenceItemBuilder {

    public ArtifactPayloadDto build(Long requestId,
                                    ScreenedEvidence screenedEvidence,
                                    String confidenceLevel,
                                    List<Long> parentArtifactIds) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("requestId", requestId);
        content.put("source", screenedEvidence.source());
        content.put("sourceDocumentId", screenedEvidence.sourceDocumentId());
        content.put("title", empty(screenedEvidence.title()));
        content.put("abstract", empty(screenedEvidence.abstractText()));
        content.put("doi", empty(screenedEvidence.doi()));
        content.put("url", empty(screenedEvidence.url()));
        content.put("publicationYear", empty(screenedEvidence.publicationYear()));
        content.put("limitation", screenedEvidence.limitations());
        content.put("proximidadeComProblema", screenedEvidence.proximityWithProblem());
        content.put("aplicabilidadeAoNicho", screenedEvidence.applicabilityToNiche());
        content.put("sinaisForcaEvidencia", screenedEvidence.evidenceStrengthSignals());
        content.put("relevanceScore", screenedEvidence.relevanceScore());
        content.put("applicabilityScore", screenedEvidence.applicabilityScore());
        content.put("priorityScore", screenedEvidence.priorityScore());
        content.put("confidenceLevel", confidenceLevel);

        return new ArtifactPayloadDto(
                "evidenceItem",
                "v1",
                "v1",
                "DRAFT",
                "mds",
                "mds",
                content,
                null,
                parentArtifactIds == null ? List.of() : parentArtifactIds
        );
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }
}
