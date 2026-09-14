package com.marketinghub.worker.creative.pipeline;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: montar payloads visuais finais a partir dos contratos de campanha. */
@Component
public class AdImagePayloadBuilder {
    private static final String DEFAULT_MODEL = "gpt-image-2.5-sunburst";

    /** Monta um payload de imagem coerente para cada variante publicitária. */
    public ImageBuildPayloadsOutput buildAdImagePayloads(BuildAdImagePayloadsInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Input de build de imagem é obrigatório");
        }
        CampaignAngle angle = requireCampaignAngle(input.campaignAngle());
        validateCampaignConsistency(angle);
        if (input.adImageBriefing() == null || input.adImageBriefing().variants() == null
                || input.adImageBriefing().variants().isEmpty()) {
            throw new IllegalArgumentException("adImageBriefing.variants é obrigatório");
        }

        List<ImageBuildPayload> payloads = new ArrayList<>();
        for (VisualVariant visualVariant : input.adImageBriefing().variants()) {
            String matchVariant = requireText(visualVariant.mustMatchAdVariant(),
                    "mustMatchAdVariant é obrigatório em cada variante visual");
            AdCopyVariant copyVariant = chooseCopyForVariant(input.adCopy(), matchVariant);
            if (copyVariant == null) {
                throw new IllegalArgumentException("Variante visual sem copy correspondente: " + matchVariant);
            }
            String placement = choosePlacement(visualVariant);
            OverlayCopy overlay = limitOverlayText(visualVariant.onImageCopy(), angle.primaryCTA());
            String variantId = chooseVariantId(input.experimentMetadata(), visualVariant);
            String label = StringUtils.hasText(visualVariant.label()) ? visualVariant.label().trim() : matchVariant;
            String prompt = toFinalImagePrompt(angle, visualVariant, overlay, placement, input.adImageBriefing().globalDesignSystem());
            validatePromptSpecificity(prompt, angle.audienceFilterLine());
            validateSingleVisualFocus(prompt);
            validatePromptReadability(prompt, overlay);

            ImageBuildPayload payload = new ImageBuildPayload(
                    buildAssetId(input.experimentMetadata(), variantId, placement),
                    variantId,
                    placement,
                    label,
                    prompt,
                    imageParamsForPlacement(placement),
                    overlay,
                    new Consistency(
                            angle.singleMindedPromise(),
                            angle.audienceFilterLine(),
                            normalizeCta(angle.primaryCTA()),
                            angle.landingMatchLine()),
                    mergeExperimentMetadata(input.experimentMetadata(), variantId));
            payloads.add(payload);
        }
        return new ImageBuildPayloadsOutput(payloads);
    }

    /** Exige o ângulo de campanha necessário para construir as peças. */
    CampaignAngle requireCampaignAngle(CampaignAngle campaignAngle) {
        if (campaignAngle == null) {
            throw new IllegalArgumentException("campaignAngle é obrigatório");
        }
        return campaignAngle;
    }

    /** Valida que promessa, CTA e público estejam presentes no ângulo. */
    void validateCampaignConsistency(CampaignAngle campaignAngle) {
        requireText(campaignAngle.singleMindedPromise(), "singleMindedPromise é obrigatório");
        requireText(campaignAngle.primaryCTA(), "primaryCTA é obrigatório");
        requireText(campaignAngle.audienceFilterLine(), "audienceFilterLine é obrigatório");
    }

    /** Converte o placement textual no formato operacional de feed ou stories. */
    String choosePlacement(VisualVariant variant) {
        String raw = normalize(variant.placement());
        if (raw.contains("story") || raw.contains("stories") || raw.contains("reel")) {
            return "stories";
        }
        return "feed";
    }

    /** Localiza a copy vinculada à variante visual. */
    AdCopyVariant chooseCopyForVariant(AdCopy adCopy, String mustMatchAdVariant) {
        if (adCopy == null || adCopy.variants() == null) {
            return null;
        }
        String expected = normalize(mustMatchAdVariant);
        return adCopy.variants().stream()
                .filter(v -> expected.equals(normalize(v.label())))
                .findFirst()
                .orElse(null);
    }

    /** Limita o CTA ao tamanho aceito na composição visual. */
    String normalizeCta(String cta) {
        String normalized = StringUtils.hasText(cta) ? cta.trim() : "";
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("CTA é obrigatório");
        }
        return normalized.length() > 28 ? normalized.substring(0, 28).trim() : normalized;
    }

    /** Reduz o texto sobreposto aos limites de legibilidade comercial. */
    OverlayCopy limitOverlayText(OnImageCopy onImageCopy, String primaryCta) {
        OnImageCopy safeCopy = onImageCopy != null ? onImageCopy : new OnImageCopy(null, null, null, null);
        String headline = limitWords(safeCopy.headline(), 8);
        String subhead = limitWords(safeCopy.subhead(), 14);
        String badge = limitWords(safeCopy.badge(), 4);
        String cta = limitWords(StringUtils.hasText(safeCopy.cta()) ? safeCopy.cta() : primaryCta, 4);
        return new OverlayCopy(headline, subhead, badge, cta);
    }

    /** Constrói o prompt visual final preservando promessa, público e correspondência da landing. */
    String toFinalImagePrompt(CampaignAngle angle,
                              VisualVariant variant,
                              OverlayCopy overlay,
                              String placement,
                              GlobalDesignSystem globalDesignSystem) {
        String idea = variant.concept() != null ? variant.concept().idea() : null;
        String focus = firstNonBlank(variant.visualMetaphor(), variant.primaryPainToVisualize(), idea);
        if (!StringUtils.hasText(focus)) {
            throw new IllegalArgumentException("Variante visual sem foco principal definido");
        }
        String style = StringUtils.hasText(globalDesignSystem != null ? globalDesignSystem.style() : null)
                ? globalDesignSystem.style().trim()
                : "fotografia publicitária realista";
        String direction = String.join(", ", (variant.visualDirections() == null ? List.<String>of() : variant.visualDirections())
                .stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .limit(3)
                .toList());
        String placementLabel = "stories".equals(placement) ? "Stories/Reels" : "Feed";

        return "Crie um anúncio vertical para Instagram/Meta Ads voltado ao nicho " + angle.audienceFilterLine() + ". "
                + "Mostrar uma cena de anúncio de feed com foco visual único em " + focus + ", "
                + "preservando a promessa central '" + angle.singleMindedPromise() + "'. "
                + "Use o contexto do nicho para ficar claro em até 2 segundos no mobile. "
                + "Estilo visual: " + style + ". "
                + (StringUtils.hasText(direction) ? "Direções visuais: " + direction + ". " : "")
                + (StringUtils.hasText(idea) ? "Ideia-base: " + idea + ". " : "")
                + "composição simples e forte, sem colunas, sem múltiplos cards e sem mini-textos. "
                + "Texto sobreposto curto e legível: headline '" + safe(overlay.headline()) + "', subhead '"
                + safe(overlay.subhead()) + "', badge '" + safe(overlay.badge()) + "', CTA '"
                + safe(overlay.cta()) + "'. "
                + "Manter CTA principal alinhado com landing: '" + normalizeCta(angle.primaryCTA()) + "' e com a linha '"
                + safe(angle.landingMatchLine()) + "'. "
                + "Formato " + placementLabel + ". "
                + "Evitar aparência de dashboard, software genérico, apresentação corporativa ou infográfico confuso.";
    }

    /** Confirma que o prompt contém contexto reconhecível do público. */
    void validatePromptSpecificity(String prompt, String audienceFilterLine) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("imagePrompt final não pode ser vazio");
        }
        String nicheToken = firstMeaningfulToken(audienceFilterLine);
        if (!StringUtils.hasText(nicheToken) || !normalize(prompt).contains(normalize(nicheToken))) {
            throw new IllegalArgumentException("Prompt final genérico demais para o nicho");
        }
    }

    /** Bloqueia direções que introduzam múltiplos focos visuais. */
    void validateSingleVisualFocus(String prompt) {
        String normalized = normalize(prompt);
        if (mentionsForbiddenStyleWithoutNegation(normalized, "infografico")
                || mentionsForbiddenStyleWithoutNegation(normalized, "dashboard")
                || mentionsForbiddenStyleWithoutNegation(normalized, "multiplos cards")) {
            throw new IllegalArgumentException("Prompt final parece infográfico/confuso");
        }
    }

    /** Distingue uma proibição explícita de um pedido acidental do estilo vetado. */
    private boolean mentionsForbiddenStyleWithoutNegation(String normalizedPrompt, String token) {
        int index = normalizedPrompt.indexOf(token);
        while (index >= 0) {
            int sentenceStart = Math.max(normalizedPrompt.lastIndexOf('.', index),
                    Math.max(normalizedPrompt.lastIndexOf(';', index), normalizedPrompt.lastIndexOf(':', index)));
            String leftContext = normalizedPrompt.substring(sentenceStart + 1, index).trim();
            boolean negated = leftContext.contains(" sem ")
                    || leftContext.endsWith(" sem")
                    || leftContext.contains(" evitar ")
                    || leftContext.startsWith("evitar ")
                    || leftContext.contains(" evite ")
                    || leftContext.startsWith("evite ")
                    || leftContext.contains(" evitando ")
                    || leftContext.startsWith("evitando ");
            if (!negated) {
                return true;
            }
            index = normalizedPrompt.indexOf(token, index + token.length());
        }
        return false;
    }

    /** Valida a quantidade de texto e a orientação de leitura em telas móveis. */
    void validatePromptReadability(String prompt, OverlayCopy overlay) {
        int overlayWords = countWords(overlay.headline()) + countWords(overlay.subhead())
                + countWords(overlay.badge()) + countWords(overlay.cta());
        if (overlayWords > 28) {
            throw new IllegalArgumentException("Prompt final com texto sobreposto excessivo");
        }
        if (!normalize(prompt).contains("legivel") && !normalize(prompt).contains("mobile")) {
            throw new IllegalArgumentException("Prompt final precisa reforçar legibilidade mobile");
        }
    }

    /** Resolve tamanho, modelo e qualidade canônicos para o placement. */
    ImageParams imageParamsForPlacement(String placement) {
        String size = "stories".equals(placement) ? "1024x1792" : "1024x1536";
        return new ImageParams("image_api", DEFAULT_MODEL, size, "high", "opaque", "png");
    }

    /** Combina os metadados originais com a identidade da variante materializada. */
    private ExperimentMetadata mergeExperimentMetadata(ExperimentMetadata metadata, String variantId) {
        ExperimentMetadata source = metadata != null
                ? metadata
                : new ExperimentMetadata(null, null, "AD", null, null);
        return new ExperimentMetadata(
                source.primaryVariable(),
                StringUtils.hasText(source.variantId()) ? source.variantId() : variantId,
                StringUtils.hasText(source.stage()) ? source.stage() : "AD",
                source.controlOrTreatment(),
                "ad-image-build");
    }

    /** Escolhe a identidade explícita da variante ou aplica a versão inicial. */
    private String chooseVariantId(ExperimentMetadata metadata, VisualVariant variant) {
        if (StringUtils.hasText(variant.variantId())) {
            return variant.variantId().trim();
        }
        if (metadata != null && StringUtils.hasText(metadata.variantId())) {
            return metadata.variantId().trim();
        }
        return "V1";
    }

    /** Gera um identificador estável para o asset do experimento. */
    private String buildAssetId(ExperimentMetadata metadata, String variantId, String placement) {
        String variable = metadata != null && StringUtils.hasText(metadata.primaryVariable())
                ? metadata.primaryVariable().trim()
                : "X";
        return "AD-" + variable + "-" + variantId + "-" + placement;
    }

    /** Limita um texto pelo número máximo de palavras permitido. */
    private String limitWords(String text, int maxWords) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) {
            return text.trim();
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(words[i]);
        }
        return builder.toString();
    }

    /** Extrai um token útil para validar a presença do nicho no prompt. */
    private String firstMeaningfulToken(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String[] words = text.trim().split("\\s+");
        for (String word : words) {
            if (word.length() >= 4) {
                return word;
            }
        }
        return words.length > 0 ? words[0] : null;
    }

    /** Conta palavras de um trecho opcional. */
    private int countWords(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /** Exige conteúdo textual e devolve o valor normalizado. */
    private String requireText(String text, String errorMessage) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return text.trim();
    }

    /** Normaliza texto para comparações sem acentos e sem diferença de caixa. */
    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /** Retorna o primeiro candidato textual preenchido. */
    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    /** Converte texto ausente em valor seguro para o prompt. */
    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /** Responsabilidade: transportar os insumos estruturados da montagem visual. */
    public record BuildAdImagePayloadsInput(
            ExperimentMetadata experimentMetadata,
            CampaignAngle campaignAngle,
            AdCopy adCopy,
            AdImageBriefing adImageBriefing) {
    }

    /** Responsabilidade: preservar a linhagem da variante dentro do experimento. */
    public record ExperimentMetadata(
            String primaryVariable,
            String variantId,
            String stage,
            String controlOrTreatment,
            String assetRole) {
    }

    /** Responsabilidade: transportar a decisão estratégica do anúncio. */
    public record CampaignAngle(
            String singleMindedPromise,
            String primaryCTA,
            String landingMatchLine,
            String audienceFilterLine) {
    }

    /** Responsabilidade: agrupar as variantes de copy disponíveis. */
    public record AdCopy(List<AdCopyVariant> variants) {
    }

    /** Responsabilidade: representar uma variante textual do anúncio. */
    public record AdCopyVariant(
            String label,
            String headline,
            String primaryText,
            String ctaText) {
    }

    /** Responsabilidade: transportar briefing e variantes visuais aprovadas. */
    public record AdImageBriefing(
            GlobalDesignSystem globalDesignSystem,
            List<VisualVariant> variants) {
    }

    /** Responsabilidade: representar a direção estética compartilhada pelas peças. */
    public record GlobalDesignSystem(String style) {
    }

    /** Responsabilidade: representar a direção visual de uma variante. */
    public record VisualVariant(
            String variantId,
            String mustMatchAdVariant,
            String placement,
            String label,
            Concept concept,
            String primaryPainToVisualize,
            String visualMetaphor,
            OnImageCopy onImageCopy,
            List<String> visualDirections) {
    }

    /** Responsabilidade: transportar a ideia central de uma peça. */
    public record Concept(String idea) {
    }

    /** Responsabilidade: representar a copy sobreposta na imagem. */
    public record OnImageCopy(
            String headline,
            String subhead,
            String badge,
            String cta) {
    }

    /** Responsabilidade: agrupar os payloads visuais prontos para geração. */
    public record ImageBuildPayloadsOutput(List<ImageBuildPayload> imageBuildPayloads) {
    }

    /** Responsabilidade: transportar o contrato completo de uma imagem publicitária. */
    public record ImageBuildPayload(
            String assetId,
            String variantId,
            String placement,
            String label,
            String imagePrompt,
            ImageParams imageParams,
            OverlayCopy overlayCopy,
            Consistency consistency,
            ExperimentMetadata experimentMetadata) {
    }

    /** Responsabilidade: representar os parâmetros técnicos da geração. */
    public record ImageParams(
            String apiMode,
            String model,
            String size,
            String quality,
            String background,
            String format) {
    }

    /** Responsabilidade: transportar o texto sobreposto já limitado. */
    public record OverlayCopy(
            String headline,
            String subhead,
            String badge,
            String cta) {
    }

    /** Responsabilidade: preservar a coerência da peça com campanha e landing. */
    public record Consistency(
            String singleMindedPromise,
            String audienceFilterLine,
            String ctaMatch,
            String landingMatchLine) {
    }
}
