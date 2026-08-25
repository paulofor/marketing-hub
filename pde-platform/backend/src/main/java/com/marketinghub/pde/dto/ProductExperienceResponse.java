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
        String completionOffer,
        ServiceScopeDto serviceScope,
        List<PublicProofDto> publicProofs,
        List<CommercialProcessStepDto> commercialProcess,
        CommercialBindingDto commercialBinding
) {

    /** Mantém compatibilidade com os contratos anteriores à experiência comercial assistida v2. */
    public ProductExperienceResponse(
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
            String completionOffer) {
        this(
                slug,
                experienceVersion,
                layoutKey,
                funnelVersion,
                name,
                promise,
                audience,
                priceLabel,
                theme,
                diagnostic,
                missions,
                supportMaterials,
                heroVideos,
                publicDiagnosticQuestions,
                publicFirstFold,
                scientificEvidencePack,
                completionOffer,
                null,
                List.of(),
                List.of(),
                null);
    }

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
            String visualCue,
            String completionRole,
            DeliveryContractDto deliveryContract,
            MissionInteractionContractDto interaction
    ) {
        /** Mantém compatibilidade com contratos que já declaram uma entrega estruturada. */
        public MissionDto(
                String id,
                int day,
                String title,
                String principle,
                String action,
                String evidence,
                String visualCue,
                String completionRole,
                DeliveryContractDto deliveryContract) {
            this(id, day, title, principle, action, evidence, visualCue, completionRole, deliveryContract, null);
        }

        /** Mantém compatibilidade com contratos que já declaram somente o papel de conclusão. */
        public MissionDto(
                String id,
                int day,
                String title,
                String principle,
                String action,
                String evidence,
                String visualCue,
                String completionRole) {
            this(id, day, title, principle, action, evidence, visualCue, completionRole, null, null);
        }

        /** Mantém compatibilidade com contratos anteriores em que toda missão pertence à cliente. */
        public MissionDto(
                String id,
                int day,
                String title,
                String principle,
                String action,
                String evidence,
                String visualCue) {
            this(id, day, title, principle, action, evidence, visualCue, null, null, null);
        }
    }

    /** Define o formulário e a orientação que materializam semanticamente uma missão. */
    public record MissionInteractionContractDto(
            String guidanceType,
            String kicker,
            String title,
            String helperText,
            String buttonLabel,
            String loadingLabel,
            String pendingLabel,
            String failedLabel,
            String completedKicker,
            String nextStepTitle,
            String nextStepText,
            List<MissionInteractionFieldDto> fields
    ) {}

    /** Define uma escolha categorial autorizada dentro do formulário da missão. */
    public record MissionInteractionFieldDto(
            String key,
            String label,
            String placeholder,
            List<String> options
    ) {}

    /** Define as seções materiais obrigatórias de uma entrega operacional estruturada. */
    public record DeliveryContractDto(List<DeliverySectionDto> sections) {}

    /** Define quantidade e identidade de uma seção verificável da entrega. */
    public record DeliverySectionDto(String id, String title, int minItems, int maxItems) {}

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

    /** Define o escopo pago que precisa aparecer integralmente antes do checkout. */
    public record ServiceScopeDto(
            List<String> includedItems,
            List<String> excludedItems,
            String deadlineStartsWhen
    ) {}

    /** Transporta uma prova fiel do produto real sem criar depoimento ou resultado fictício. */
    public record PublicProofDto(
            String id,
            String type,
            String title,
            String content,
            List<String> items,
            String evidenceLabel,
            String source
    ) {}

    /** Explica uma etapa observável entre a compra e a primeira aplicação do produto. */
    public record CommercialProcessStepDto(
            int order,
            String title,
            String description,
            String timing
    ) {}

    /** Congela a identidade comercial que deve coincidir com a oferta canônica do experimento. */
    public record CommercialBindingDto(
            Long experimentId,
            String primaryCta,
            java.math.BigDecimal priceBrl,
            String billingModel
    ) {}
}
