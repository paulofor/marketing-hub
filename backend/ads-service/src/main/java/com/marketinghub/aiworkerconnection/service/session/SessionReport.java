package com.marketinghub.aiworkerconnection.service.session;

import jakarta.validation.constraints.NotNull;

/** Transporta exclusivamente a confirmação booleana obtida com account/read. */
public record SessionReport(@NotNull Boolean authenticated) {}
