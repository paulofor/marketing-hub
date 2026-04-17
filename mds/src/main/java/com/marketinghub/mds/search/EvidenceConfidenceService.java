package com.marketinghub.mds.search;

import org.springframework.stereotype.Service;

@Service
public class EvidenceConfidenceService {

    public String classify(ScreenedEvidence evidence) {
        boolean hasStrongSignals = !evidence.evidenceStrengthSignals().isEmpty();
        boolean hasManyLimitations = evidence.limitations().size() >= 2;

        if (evidence.priorityScore() >= 0.65 && hasStrongSignals && !hasManyLimitations) {
            return "alta";
        }
        if (evidence.priorityScore() >= 0.45 && !hasManyLimitations) {
            return "moderada";
        }
        if (evidence.priorityScore() >= 0.25) {
            return "baixa";
        }
        return "muito_baixa";
    }
}
