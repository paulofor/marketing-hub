package com.marketinghub.pde.dto;

import java.util.Map;

/** Confirma o tipo, o estado e o instante da ação de privacidade executada. */
public record PrivacyActionResponse(
        String action,
        String status,
        String executedAt,
        Map<String, Object> data
) {}
