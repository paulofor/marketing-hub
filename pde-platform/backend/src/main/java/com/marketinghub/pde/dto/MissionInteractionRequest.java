package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/** Recebe respostas da cliente para personalizar uma missão da experiência PDE. */
public record MissionInteractionRequest(
        @NotEmpty Map<String, String> answers
) {}
