package com.marketinghub.mds.search;

import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PracticalKnowledgePackBuilder {

    public ArtifactPayloadDto build(Long requestId,
                                    MechanismCandidateBuilder.MechanismSpecDraft mechanismSpecDraft,
                                    List<Long> parentArtifactIds) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("requestId", requestId);
        content.put("recommendedMechanismCandidateKey", mechanismSpecDraft.recommendedCandidateKey());
        content.put("confidenceLevel", mechanismSpecDraft.confidenceLevel());
        content.put("activeComponents", mechanismSpecDraft.essentialComponents());
        content.put("supportingEvidenceThemes", mechanismSpecDraft.riskOrLimitations());
        content.put("technicalVersion", buildTechnical(mechanismSpecDraft));
        content.put("executiveVersion", buildExecutive(mechanismSpecDraft));
        content.put("productDesignVersion", buildProductDesign(mechanismSpecDraft));
        content.put("consumerVersion", buildConsumer(mechanismSpecDraft));

        return new ArtifactPayloadDto(
                "practicalKnowledgePack",
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

    private Map<String, Object> buildTechnical(MechanismCandidateBuilder.MechanismSpecDraft mechanismSpecDraft) {
        return Map.of(
                "summary", mechanismSpecDraft.selectionJustification(),
                "behavioralLevers", mechanismSpecDraft.optionalComponents(),
                "activeComponents", mechanismSpecDraft.essentialComponents()
        );
    }

    private Map<String, Object> buildExecutive(MechanismCandidateBuilder.MechanismSpecDraft mechanismSpecDraft) {
        return Map.of(
                "thesis", "Mecanismo recomendado com base em evidências priorizadas.",
                "confidenceLevel", mechanismSpecDraft.confidenceLevel(),
                "recommendedMechanismCandidateKey", mechanismSpecDraft.recommendedCandidateKey()
        );
    }

    private Map<String, Object> buildProductDesign(MechanismCandidateBuilder.MechanismSpecDraft mechanismSpecDraft) {
        return Map.of(
                "designGuidelines", List.of(
                        "Priorizar fricção baixa para adesão inicial.",
                        "Explícitar gatilhos comportamentais no onboarding.",
                        "Ancorar mensagens nos componentes ativos identificados."
                ),
                "behavioralLevers", mechanismSpecDraft.optionalComponents()
        );
    }

    private Map<String, Object> buildConsumer(MechanismCandidateBuilder.MechanismSpecDraft mechanismSpecDraft) {
        return Map.of(
                "message", "Este método combina hábitos-chave para aumentar consistência e resultado.",
                "focusComponents", mechanismSpecDraft.essentialComponents()
        );
    }
}
