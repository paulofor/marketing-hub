package com.marketinghub.pde.dto;

import java.util.List;

/** Descreve o produto experiencial que o frontend renderiza para a cliente. */
public record ProductExperienceResponse(
        String slug,
        String experienceVersion,
        String layoutKey,
        String funnelVersion,
        String name,
        String promise,
        String audience,
        String priceLabel,
        ThemeDto theme,
        DiagnosticDto diagnostic,
        List<MissionDto> missions,
        List<SupportMaterialDto> supportMaterials,
        List<HeroVideoDto> heroVideos,
        List<PublicDiagnosticQuestionDto> publicDiagnosticQuestions,
        PublicFirstFoldDto publicFirstFold,
        ScientificEvidencePackDto scientificEvidencePack,
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

    /** Define uma pergunta editável do diagnóstico público renderizado na primeira experiência. */
    public record PublicDiagnosticQuestionDto(
            String key,
            String stageLabel,
            String question,
            List<String> options,
            String imageUrl,
            String visualTitle,
            String visualText,
            String journeyEventType
    ) {}

    /** Define um vídeo hero aprovado para uma versão pública específica do PDE. */
    public record HeroVideoDto(
            String experienceVersion,
            String placement,
            String playbackUrl,
            String hlsPlaybackUrl,
            String posterUrl,
            Boolean autoplay,
            Boolean muted,
            Boolean controls,
            Boolean loop,
            Boolean playsInline,
            String source,
            Long assetId,
            Long experimentVideoAssetId,
            Long salesVideoProfileId,
            Long salesVideoJobId,
            String reviewStatus,
            String status
    ) {}

    /** Define a copy versionada da primeira dobra pública do produto. */
    public record PublicFirstFoldDto(
            String headline,
            String supportingText,
            String videoKicker,
            String videoHeadline,
            String videoSupportingText,
            String videoExtraText,
            String videoCtaLabel
    ) {}

    /** Define a base científica operacional usada pela IA sem expor artigo bruto à cliente. */
    public record ScientificEvidencePackDto(
            String version,
            List<String> principles,
            List<String> practicalApplications,
            List<String> allowedLanguage,
            List<String> forbiddenClaims,
            List<ScientificReferenceDto> references
    ) {}

    /** Define uma referência científica rastreável usada na criação do produto. */
    public record ScientificReferenceDto(String authors, String year, String title, String source, String doi) {}
}
