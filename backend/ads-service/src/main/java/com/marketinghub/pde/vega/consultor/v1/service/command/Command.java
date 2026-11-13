package com.marketinghub.pde.vega.consultor.v1.service.command;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

/** Transporta comandos limitados da homologação sem aceitar direitos comerciais. */
public record Command(@NotBlank @Size(max = 80) String action, @NotNull JsonNode payload) {}
