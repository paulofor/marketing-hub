package com.marketinghub.experiment.pipeline;

import org.springframework.util.StringUtils;

/** Define as seções canônicas e a precedência operacional do pipeline de experimento. */
public enum ExperimentPipelineSection {
    CAMPAIGN_ANGLE("campaign-angle", null),
    AD_COPY("ad-copy", CAMPAIGN_ANGLE),
    AD_IMAGE_BRIEFING("ad-image-briefing", AD_COPY),
    LANDING_PAGE_WIREFRAME("landing-page-wireframe", AD_IMAGE_BRIEFING),
    LANDING_PAGE_COPY("landing-page-copy", LANDING_PAGE_WIREFRAME),
    LANDING_PAGE_IMAGE_PLANNING("landing-page-image-planning", LANDING_PAGE_WIREFRAME),
    LANDING_PAGE_DESIGN_PRESET("landing-page-design-preset", LANDING_PAGE_IMAGE_PLANNING),
    LANDING_PAGE_HTML("landing-page-html", LANDING_PAGE_DESIGN_PRESET);

    private final String path;
    private final ExperimentPipelineSection predecessor;

    ExperimentPipelineSection(String path, ExperimentPipelineSection predecessor) {
        this.path = path;
        this.predecessor = predecessor;
    }

    /** Retorna o path canônico usado em rotas, prompts e persistência da seção. */
    public String path() {
        return path;
    }

    /** Retorna a seção que deve estar concluída antes da seção atual. */
    public ExperimentPipelineSection predecessor() {
        return predecessor;
    }

    /** Retorna a primeira seção sucessora direta quando existir. */
    public ExperimentPipelineSection successor() {
        for (ExperimentPipelineSection section : values()) {
            if (section.predecessor == this) {
                return section;
            }
        }
        return null;
    }

    /** Converte o path recebido pela API para a seção canônica correspondente. */
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
