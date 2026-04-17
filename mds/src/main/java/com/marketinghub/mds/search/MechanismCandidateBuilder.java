package com.marketinghub.mds.search;

import com.marketinghub.mds.dto.BackendArtifactPublishBatchRequestDto.ArtifactPayloadDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MechanismCandidateBuilder {
    private final ActiveComponentExtractor activeComponentExtractor;

    public MechanismCandidateBuilder(ActiveComponentExtractor activeComponentExtractor) {
        this.activeComponentExtractor = activeComponentExtractor;
    }

    public MechanismBuildResult build(Long requestId,
                                      List<ScreenedEvidence> prioritizedEvidence,
                                      Map<String, String> confidenceBySourceDocumentId,
                                      Map<String, Long> evidenceArtifactIdsBySourceDocumentId) {
        if (prioritizedEvidence == null || prioritizedEvidence.isEmpty()) {
            return new MechanismBuildResult(List.of(), null, List.of());
        }

        Map<String, Integer> componentFrequency = new LinkedHashMap<>();
        Set<String> limitations = new LinkedHashSet<>();
        double avgPriority = prioritizedEvidence.stream().mapToDouble(ScreenedEvidence::priorityScore).average().orElse(0.0);
        List<Long> supportingEvidenceIds = new ArrayList<>();
        List<String> evidenceConfidence = new ArrayList<>();

        for (ScreenedEvidence evidence : prioritizedEvidence) {
            List<String> components = activeComponentExtractor.extract(evidence);
            for (String component : components) {
                componentFrequency.merge(component, 1, Integer::sum);
            }
            limitations.addAll(evidence.limitations());
            evidenceConfidence.add(confidenceBySourceDocumentId.getOrDefault(evidence.sourceDocumentId(), "muito_baixa"));
            Long evidenceArtifactId = evidenceArtifactIdsBySourceDocumentId.get(evidence.sourceDocumentId());
            if (evidenceArtifactId != null) {
                supportingEvidenceIds.add(evidenceArtifactId);
            }
        }

        if (componentFrequency.isEmpty()) {
            return new MechanismBuildResult(List.of(), null, supportingEvidenceIds);
        }

        int threshold = Math.max(2, (int) Math.ceil(prioritizedEvidence.size() * 0.4));
        List<String> initialEssentialComponents = componentFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        List<String> optionalComponents = componentFrequency.entrySet().stream()
                .filter(entry -> !initialEssentialComponents.contains(entry.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(4)
                .toList();

        List<String> essentialComponents = initialEssentialComponents;
        if (initialEssentialComponents.isEmpty()) {
            essentialComponents = componentFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .limit(2)
                    .toList();
        }

        String confidenceLevel = aggregateConfidence(avgPriority, evidenceConfidence);
        String justification = buildJustification(essentialComponents, optionalComponents, prioritizedEvidence.size(), confidenceLevel);

        Map<String, Object> candidateContent = new LinkedHashMap<>();
        String candidateKey = "mc-1";
        candidateContent.put("requestId", requestId);
        candidateContent.put("candidateKey", candidateKey);
        candidateContent.put("candidateLabel", "Mecanismo recorrente principal");
        candidateContent.put("essentialComponents", essentialComponents);
        candidateContent.put("optionalComponents", optionalComponents);
        candidateContent.put("riskOrLimitations", new ArrayList<>(limitations));
        candidateContent.put("supportingEvidenceCount", prioritizedEvidence.size());
        candidateContent.put("confidenceLevel", confidenceLevel);
        candidateContent.put("selectionJustification", justification);
        candidateContent.put("recommended", true);

        ArtifactPayloadDto candidateArtifact = new ArtifactPayloadDto(
                "mechanismCandidate",
                "v1",
                "v1",
                "DRAFT",
                "mds",
                "mds",
                candidateContent,
                null,
                supportingEvidenceIds
        );

        MechanismSpecDraft mechanismSpecDraft = new MechanismSpecDraft(
                requestId,
                candidateKey,
                essentialComponents,
                optionalComponents,
                new ArrayList<>(limitations),
                confidenceLevel,
                justification
        );

        return new MechanismBuildResult(List.of(candidateArtifact), mechanismSpecDraft, supportingEvidenceIds);
    }

    public ArtifactPayloadDto buildMechanismSpec(MechanismSpecDraft draft,
                                                 Long selectedCandidateArtifactId,
                                                 List<Long> supportingEvidenceIds) {
        if (draft == null || selectedCandidateArtifactId == null) {
            return null;
        }

        List<Long> parentArtifactIds = new ArrayList<>();
        parentArtifactIds.add(selectedCandidateArtifactId);
        parentArtifactIds.addAll(supportingEvidenceIds.stream().filter(id -> !id.equals(selectedCandidateArtifactId)).toList());

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("requestId", draft.requestId());
        content.put("recommendedMechanismCandidateKey", draft.recommendedCandidateKey());
        content.put("essentialComponents", draft.essentialComponents());
        content.put("optionalComponents", draft.optionalComponents());
        content.put("riskOrLimitations", draft.riskOrLimitations());
        content.put("confidenceLevel", draft.confidenceLevel());
        content.put("selectionJustification", draft.selectionJustification());

        return new ArtifactPayloadDto(
                "mechanismSpec",
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

    private String buildJustification(List<String> essential,
                                      List<String> optional,
                                      int evidenceCount,
                                      String confidenceLevel) {
        return "Seleção baseada em recorrência de componentes (essenciais: "
                + String.join(", ", essential)
                + "), suporte em "
                + evidenceCount
                + " evidências e confiança "
                + confidenceLevel
                + ". Componentes opcionais considerados: "
                + String.join(", ", optional)
                + ".";
    }

    private String aggregateConfidence(double avgPriority, List<String> evidenceConfidence) {
        int high = (int) evidenceConfidence.stream().filter(c -> c.equals("alta")).count();
        int medium = (int) evidenceConfidence.stream().filter(c -> c.equals("moderada")).count();

        if (avgPriority >= 0.65 && high >= 1) {
            return "alta";
        }
        if (avgPriority >= 0.45 && (high + medium) >= 1) {
            return "moderada";
        }
        if (avgPriority >= 0.25) {
            return "baixa";
        }
        return "muito_baixa";
    }

    public record MechanismBuildResult(
            List<ArtifactPayloadDto> candidateArtifacts,
            MechanismSpecDraft mechanismSpecDraft,
            List<Long> supportingEvidenceArtifactIds
    ) {
    }

    public record MechanismSpecDraft(
            Long requestId,
            String recommendedCandidateKey,
            List<String> essentialComponents,
            List<String> optionalComponents,
            List<String> riskOrLimitations,
            String confidenceLevel,
            String selectionJustification
    ) {
    }
}
