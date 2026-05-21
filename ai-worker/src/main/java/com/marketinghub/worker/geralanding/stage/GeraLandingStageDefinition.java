package com.marketinghub.worker.geralanding.stage;

/**
 * Catálogo canônico das etapas do GeraLanding no Worker AI.
 * Cada definição representa um conjunto exclusivo de etapa.
 */
public enum GeraLandingStageDefinition {
    WIREFRAME("landing-page-wireframe"),
    COPY("landing-page-copy"),
    IMAGE_PLANNING("landing-page-image-planning"),
    DESIGN_PRESET("landing-page-design-preset"),
    DELIVERABLES("landing-page-deliverables");

    private final String code;

    GeraLandingStageDefinition(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static GeraLandingStageDefinition fromCode(String stageCode) {
        for (GeraLandingStageDefinition value : values()) {
            if (value.code.equalsIgnoreCase(stageCode)) {
                return value;
            }
        }
        return null;
    }
}
