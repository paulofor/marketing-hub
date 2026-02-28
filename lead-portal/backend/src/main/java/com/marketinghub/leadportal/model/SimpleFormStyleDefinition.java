package com.marketinghub.leadportal.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimpleFormStyleDefinition(
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
