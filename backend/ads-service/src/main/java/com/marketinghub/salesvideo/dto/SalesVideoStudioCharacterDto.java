package com.marketinghub.salesvideo.dto;

/** Responsabilidade: representar uma opcao visual de personagem do estudio de video. */
public record SalesVideoStudioCharacterDto(
    String key,
    String name,
    String status,
    String imageUrl,
    String description,
    String reason,
    String bibleText) {}
