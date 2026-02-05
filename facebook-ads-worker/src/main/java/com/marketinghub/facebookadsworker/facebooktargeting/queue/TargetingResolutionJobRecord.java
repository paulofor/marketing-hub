package com.marketinghub.facebookadsworker.facebooktargeting.queue;

import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidatePayload;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidatePayload.CandidateConstraints;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidateType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TargetingResolutionJobRecord(
    long jobId,
    UUID requestId,
    String requestLocale,
    String requestCountry,
    Long candidateId,
    String seed,
    List<String> seedVariants,
    TargetingCandidateType type,
    String localeHint,
    String locale,
    String country,
    String origin,
    BigDecimal score,
    String rationale,
    String intentTag
) {
    public TargetingCandidatePayload toPayload() {
        List<String> variants = seedVariants != null ? new ArrayList<>(seedVariants) : new ArrayList<>();
        return new TargetingCandidatePayload(
            candidateId,
            seed,
            seed,
            variants,
            type,
            localeHint,
            locale,
            country,
            origin,
            score,
            rationale,
            intentTag,
            new CandidateConstraints(country, locale)
        );
    }
}
