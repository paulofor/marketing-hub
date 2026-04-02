package com.marketinghub.experiment.pipeline.ads;

import java.util.List;

/**
 * Structured view of the image briefing returned by the experiment pipeline.
 */
public record PipelineImageBriefing(
        String mustMatchAdVariant,
        String visualAngle,
        String assetType,
        Integer imageTextMaxWords,
        String visualBriefing,
        String hierarchy,
        String formatByPlacement,
        String safeMargins,
        String complianceNotes,
        String messageMatchNotes,
        List<String> supportingKeywords) {
}
