package com.marketinghub.oprm.infra.enrichment;

import com.marketinghub.oprm.domain.OccupationSourcePolicyProfile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OccupationSourcePolicyRegistry {

    private static final List<String> DEFAULT_ALLOWLIST = List.of(
            "wikipedia.org",
            "bls.gov",
            "coursera.org",
            "senac.br",
            "sebrae.com.br",
            "indeed.com"
    );

    private static final List<String> DEFAULT_BLOCKLIST = List.of(
            "facebook.com",
            "instagram.com",
            "tiktok.com",
            "linkedin.com"
    );

    public OccupationSourcePolicyProfile policyFor(String occupationName) {
        return new OccupationSourcePolicyProfile(
                DEFAULT_ALLOWLIST,
                DEFAULT_BLOCKLIST,
                "max-5-requests-per-minute",
                "MEDIUM",
                false,
                "phase-2 allowlist policy for public occupation enrichment: " + occupationName
        );
    }
}
