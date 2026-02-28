package com.marketinghub.leadportal;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Declarative style tokens applied to the public lead portal quando renderizamos um formulário simples.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeadPortalSimpleFormStyleDefinition(
        String backgroundColor,
        String backgroundGradient,
        String backgroundPatternUrl,
        String cardBackground,
        String cardBorderColor,
        String cardShadow,
        String headingColor,
        String textColor,
        String mutedTextColor,
        String primaryColor,
        String accentColor,
        String buttonBackground,
        String buttonTextColor,
        String buttonShadow,
        String buttonBorderRadius,
        String highlightBackground,
        String inputBackground,
        String inputBorderColor,
        String heroLayout,
        String heroImageUrl,
        String heroImageBlendColor) {
}
