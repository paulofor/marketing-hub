package com.marketinghub.experiment.pipeline;

import org.springframework.util.StringUtils;

public enum ExperimentPipelineSection {
    CAMPAIGN_ANGLE("campaign-angle", null),
    AD_COPY("ad-copy", CAMPAIGN_ANGLE),
    AD_IMAGE_BRIEFING("ad-image-briefing", AD_COPY),
    LANDING_PAGE_COPY("landing-page-copy", AD_IMAGE_BRIEFING),
    LANDING_PAGE_WIREFRAME("landing-page-wireframe", LANDING_PAGE_COPY),
    LANDING_PAGE_IMAGE_PLANNING("landing-page-image-planning", LANDING_PAGE_WIREFRAME),
    LANDING_PAGE_HTML("landing-page-html", LANDING_PAGE_IMAGE_PLANNING);

    private final String path;
    private final ExperimentPipelineSection predecessor;

    ExperimentPipelineSection(String path, ExperimentPipelineSection predecessor) {
        this.path = path;
        this.predecessor = predecessor;
    }

    public String path() {
        return path;
    }

    public ExperimentPipelineSection predecessor() {
        return predecessor;
    }

    public static ExperimentPipelineSection fromPath(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("Section is required");
        }
        String normalized = raw.trim().toLowerCase();
        for (ExperimentPipelineSection section : values()) {
            if (section.path.equals(normalized)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Unknown experiment pipeline section: " + raw);
    }
}
