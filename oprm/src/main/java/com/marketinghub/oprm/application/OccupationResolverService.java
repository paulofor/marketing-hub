package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.OccupationAliasResolution;
import com.marketinghub.oprm.domain.OccupationCatalogItem;
import com.marketinghub.oprm.domain.OccupationProfileSnapshotPayload;
import com.marketinghub.oprm.infra.StructuredOccupationCatalog;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OccupationResolverService {

    private static final List<String> MVP_OCCUPATIONS = List.of(
            "personal trainer",
            "pastor",
            "agricultor",
            "manicure",
            "cabeleireiro",
            "dono de loja de celulares"
    );

    private final StructuredOccupationCatalog catalog;

    public OccupationResolverService(StructuredOccupationCatalog catalog) {
        this.catalog = catalog;
    }

    public ArtifactEnvelope resolveToProfileSnapshot(String rawOccupationLabel, String nicheName, String locale, String correlationId) {
        String normalized = OccupationLabelNormalizer.normalize(rawOccupationLabel);
        OccupationCatalogItem item = resolveItem(normalized)
                .orElseThrow(() -> new IllegalArgumentException("occupation_not_supported_for_phase_1: " + rawOccupationLabel));

        boolean exactName = OccupationLabelNormalizer.normalize(item.occupationName()).equals(normalized);
        double confidence = exactName ? 0.98 : 0.92;

        OccupationAliasResolution aliasResolution = new OccupationAliasResolution(
                rawOccupationLabel,
                normalized,
                item.occupationName(),
                exactName ? "EXACT" : "ALIAS",
                confidence,
                "phase-1 occupation resolver"
        );

        OccupationProfileSnapshotPayload payload = new OccupationProfileSnapshotPayload(
                item.occupationName(),
                item.occupationSummary(),
                item.taskList(),
                item.skillsList(),
                item.toolsList(),
                item.workContextList(),
                item.sourceSystem(),
                item.sourceRecordIds(),
                aliasResolution,
                nicheName,
                locale
        );

        return new ArtifactEnvelope(
                "occupationProfileSnapshot",
                "1.0",
                UUID.randomUUID().toString(),
                "oprm",
                "oprm.occupation-resolver",
                Instant.now(),
                correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId,
                UUID.randomUUID().toString(),
                item.sourceRecordIds(),
                List.of("occupationSeed"),
                payload,
                "GENERATED",
                confidence,
                Map.of("phase", "phase-1", "supported_mvp_occupations", MVP_OCCUPATIONS.size())
        );
    }

    public List<String> supportedOccupations() {
        return MVP_OCCUPATIONS;
    }

    private Optional<OccupationCatalogItem> resolveItem(String normalizedLabel) {
        return catalog.listAll().stream()
                .filter(item -> OccupationLabelNormalizer.normalize(item.occupationName()).equals(normalizedLabel)
                        || item.aliases().stream().map(OccupationLabelNormalizer::normalize).anyMatch(normalizedLabel::equals))
                .findFirst();
    }

}
