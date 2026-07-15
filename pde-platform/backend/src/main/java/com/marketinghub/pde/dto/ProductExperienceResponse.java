package com.marketinghub.pde.dto;

import java.util.List;

/** Descreve o produto experiencial que o frontend renderiza para a cliente. */
public record ProductExperienceResponse(
        String slug,
        String name,
        String promise,
        String audience,
        String priceLabel,
        ThemeDto theme,
        DiagnosticDto diagnostic,
        List<MissionDto> missions,
        List<SupportMaterialDto> supportMaterials,
        String completionOffer
) {

    /** Define a identidade visual básica do produto. */
    public record ThemeDto(String primary, String accent, String background, String imageUrl) {}

    /** Define o diagnóstico inicial da experiência. */
    public record DiagnosticDto(String title, String intro, List<String> questions) {}

    /** Define uma missão prática dentro da jornada guiada. */
    public record MissionDto(
            String id,
            int day,
            String title,
            String principle,
            String action,
            String evidence,
            String visualCue
    ) {}

    /** Define um material de apoio disponível na biblioteca do produto. */
    public record SupportMaterialDto(String title, String type, String description, String url) {}
}
