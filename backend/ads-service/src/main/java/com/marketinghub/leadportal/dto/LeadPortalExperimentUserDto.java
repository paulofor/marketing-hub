package com.marketinghub.leadportal.dto;

/**
 * Representa um usuário único que interagiu com o fluxo do portal.
 */
public record LeadPortalExperimentUserDto(
        String displayName, String email, String phone, boolean sentImage) {
}
