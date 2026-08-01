package com.marketinghub.salesvideo.dto;

/** Responsabilidade: representar um preset visual de legenda do estudio de video. */
public record SalesVideoStudioCaptionPresetDto(
    String key, String label, String style, String description, String planText) {}
