package com.marketinghub.leadportal.dto;

/**
 * Payload enviado pelo frontend para informar que o formulário foi renderizado completamente.
 */
public record RegisterRenderCompleteRequest(String visitorId) {
}
