package com.marketinghub.pde.dto;

/** Retorna uma resposta salva da cliente em uma missão da experiência PDE. */
public record MissionInteractionResponse(
        String missionId,
        String questionKey,
        String answerText
) {}
