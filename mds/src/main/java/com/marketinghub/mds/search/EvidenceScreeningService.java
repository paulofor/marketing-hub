package com.marketinghub.mds.search;

import com.marketinghub.mds.dto.BackendMdsRequestDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EvidenceScreeningService {
    private static final List<String> LIMITATION_HINTS = List.of(
            "mouse", "mice", "rat", "animal model", "in vitro", "pilot", "case report", "small sample"
    );
    private static final List<String> STRENGTH_HINTS = List.of(
            "randomized", "meta-analysis", "systematic review", "double blind", "placebo", "cohort"
    );

    public List<ScreenedEvidence> screen(BackendMdsRequestDto request, List<SourceSearchHit> hits) {
        String problemAndOutcome = safe(request.problem()) + " " + safe(request.desiredOutcome());
        Set<String> problemTokens = tokenize(problemAndOutcome);
        Set<String> nicheTokens = tokenize(request.market());

        List<ScreenedEvidence> screened = new ArrayList<>();
        for (SourceSearchHit hit : hits) {
            String content = (safe(hit.title()) + " " + safe(hit.abstractText())).toLowerCase(Locale.ROOT);
            double relevance = overlap(problemTokens, tokenize(content));
            double applicability = overlap(nicheTokens, tokenize(content));
            List<String> limitations = hintsFound(content, LIMITATION_HINTS);
            List<String> strengthSignals = hintsFound(content, STRENGTH_HINTS);

            String proximity = relevance >= 0.45 ? "alta" : relevance >= 0.2 ? "moderada" : "baixa";
            String nicheApplicability = applicability >= 0.3 ? "alta" : applicability >= 0.1 ? "moderada" : "baixa";
            double priority = (relevance * 0.6) + (applicability * 0.3) + (strengthSignals.isEmpty() ? 0.0 : 0.1);

            if (relevance < 0.1) {
                continue;
            }

            screened.add(new ScreenedEvidence(
                    hit.source(),
                    hit.sourceDocumentId(),
                    hit.title(),
                    hit.abstractText(),
                    hit.doi(),
                    hit.url(),
                    hit.publicationYear(),
                    limitations,
                    proximity,
                    nicheApplicability,
                    strengthSignals,
                    relevance,
                    applicability,
                    priority
            ));
        }

        return screened;
    }

    public List<ScreenedEvidence> prioritize(List<ScreenedEvidence> screened, int maxItems) {
        return screened.stream()
                .sorted(Comparator.comparingDouble(ScreenedEvidence::priorityScore).reversed())
                .limit(maxItems)
                .toList();
    }

    private Set<String> tokenize(String text) {
        return List.of(text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .stream()
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
    }

    private double overlap(Set<String> expected, Set<String> actual) {
        if (expected.isEmpty() || actual.isEmpty()) {
            return 0;
        }
        long matches = expected.stream().filter(actual::contains).count();
        return (double) matches / expected.size();
    }

    private List<String> hintsFound(String content, List<String> hints) {
        return hints.stream().filter(content::contains).toList();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
