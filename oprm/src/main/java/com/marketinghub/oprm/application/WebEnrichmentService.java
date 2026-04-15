package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.CapturedWebSource;
import com.marketinghub.oprm.domain.OccupationCatalogItem;
import com.marketinghub.oprm.domain.OccupationSourcePolicyProfile;
import com.marketinghub.oprm.domain.OccupationWebSourceSnapshotPayload;
import com.marketinghub.oprm.infra.StructuredOccupationCatalog;
import com.marketinghub.oprm.infra.enrichment.FetchedWebPage;
import com.marketinghub.oprm.infra.enrichment.OccupationSourcePolicyRegistry;
import com.marketinghub.oprm.infra.enrichment.OccupationWebSeedRegistry;
import com.marketinghub.oprm.infra.enrichment.WebPageFetcher;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class WebEnrichmentService {

    private static final int MAX_CAPTURED_SOURCES = 3;

    private final StructuredOccupationCatalog catalog;
    private final OccupationSourcePolicyRegistry policyRegistry;
    private final OccupationWebSeedRegistry seedRegistry;
    private final WebPageFetcher webPageFetcher;

    public WebEnrichmentService(StructuredOccupationCatalog catalog,
                                OccupationSourcePolicyRegistry policyRegistry,
                                OccupationWebSeedRegistry seedRegistry,
                                WebPageFetcher webPageFetcher) {
        this.catalog = catalog;
        this.policyRegistry = policyRegistry;
        this.seedRegistry = seedRegistry;
        this.webPageFetcher = webPageFetcher;
    }

    public ArtifactEnvelope enrichOccupation(String rawOccupationLabel, String nicheName, String locale, String correlationId) {
        String normalized = OccupationLabelNormalizer.normalize(rawOccupationLabel);
        OccupationCatalogItem item = resolveItem(normalized)
                .orElseThrow(() -> new IllegalArgumentException("occupation_not_supported_for_phase_2: " + rawOccupationLabel));

        OccupationSourcePolicyProfile policyProfile = policyRegistry.policyFor(item.occupationName());
        List<String> seededUrls = seedRegistry.seedsFor(item.occupationName());

        if (seededUrls.isEmpty()) {
            throw new IllegalArgumentException("no_web_seeds_for_occupation: " + item.occupationName());
        }

        List<CapturedWebSource> capturedSources = seededUrls.stream()
                .filter(url -> isAllowed(url, policyProfile.allowedDomains(), policyProfile.blockedDomains()))
                .limit(MAX_CAPTURED_SOURCES)
                .map(this::captureSource)
                .toList();

        List<String> semanticSignals = buildSemanticSignals(item, capturedSources);

        OccupationWebSourceSnapshotPayload payload = new OccupationWebSourceSnapshotPayload(
                item.occupationName(),
                nicheName,
                locale,
                policyProfile,
                capturedSources,
                semanticSignals,
                buildSummary(item.occupationName(), capturedSources)
        );

        double confidenceScore = capturedSources.stream().anyMatch(source -> "CAPTURED".equals(source.captureStatus())) ? 0.87 : 0.45;

        return new ArtifactEnvelope(
                "occupationWebSourceSnapshot",
                "1.0",
                UUID.randomUUID().toString(),
                "oprm",
                "oprm.web-enrichment",
                Instant.now(),
                correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId,
                UUID.randomUUID().toString(),
                capturedSources.stream().map(CapturedWebSource::url).toList(),
                List.of("occupationSeed", "occupationProfileSnapshot"),
                payload,
                "GENERATED",
                confidenceScore,
                Map.of(
                        "phase", "phase-2",
                        "allowlist_domains", policyProfile.allowedDomains().size(),
                        "captured_sources", capturedSources.size()
                )
        );
    }

    private Optional<OccupationCatalogItem> resolveItem(String normalizedLabel) {
        return catalog.listAll().stream()
                .filter(item -> OccupationLabelNormalizer.normalize(item.occupationName()).equals(normalizedLabel)
                        || item.aliases().stream().map(OccupationLabelNormalizer::normalize).anyMatch(normalizedLabel::equals))
                .findFirst();
    }

    private boolean isAllowed(String url, List<String> allowedDomains, List<String> blockedDomains) {
        String normalizedUrl = url.toLowerCase(Locale.ROOT);
        boolean explicitlyBlocked = blockedDomains.stream().anyMatch(normalizedUrl::contains);
        boolean explicitlyAllowed = allowedDomains.stream().anyMatch(normalizedUrl::contains);
        return !explicitlyBlocked && explicitlyAllowed;
    }

    private CapturedWebSource captureSource(String url) {
        FetchedWebPage page = webPageFetcher.fetch(url);
        String excerpt = excerpt(page.content());
        String captureStatus = page.success() ? "CAPTURED" : "FAILED";

        return new CapturedWebSource(
                page.url(),
                classifySourceType(page.url()),
                page.title(),
                Instant.now(),
                page.language(),
                sha256(page.title() + "\n" + excerpt),
                excerpt.isBlank() ? List.of() : List.of(excerpt),
                page.captureNotes(),
                captureStatus
        );
    }

    private List<String> buildSemanticSignals(OccupationCatalogItem item, List<CapturedWebSource> sources) {
        Set<String> signals = new LinkedHashSet<>();
        signals.add("daily-operations");

        sources.stream()
                .map(source -> String.join(" ", source.extractedBlocks()).toLowerCase(Locale.ROOT))
                .forEach(content -> {
                    for (String task : item.taskList()) {
                        if (content.contains(task.toLowerCase(Locale.ROOT))) {
                            signals.add("task:" + task);
                        }
                    }
                    for (String tool : item.toolsList()) {
                        if (content.contains(tool.toLowerCase(Locale.ROOT))) {
                            signals.add("tool:" + tool);
                        }
                    }
                });

        if (signals.size() <= 1) {
            signals.add("insufficient-domain-terms-detected");
        }

        return new ArrayList<>(signals);
    }

    private String buildSummary(String occupationName, List<CapturedWebSource> capturedSources) {
        long capturedCount = capturedSources.stream().filter(source -> "CAPTURED".equals(source.captureStatus())).count();
        return "phase-2 web enrichment completed for " + occupationName
                + " with " + capturedCount + "/" + capturedSources.size() + " successful captures";
    }

    private String classifySourceType(String url) {
        String normalizedUrl = url.toLowerCase(Locale.ROOT);
        if (normalizedUrl.contains("wikipedia.org")) {
            return "ENCYCLOPEDIA";
        }
        if (normalizedUrl.contains("bls.gov")) {
            return "LABOR_STATISTICS";
        }
        if (normalizedUrl.contains("coursera.org") || normalizedUrl.contains("senac")) {
            return "TRAINING_CONTENT";
        }
        if (normalizedUrl.contains("sebrae") || normalizedUrl.contains("indeed")) {
            return "PRACTICAL_GUIDE";
        }
        return "PUBLIC_WEB";
    }

    private String excerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        int limit = Math.min(content.length(), 280);
        return content.substring(0, limit).trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("sha-256-unavailable", exception);
        }
    }
}
