package com.marketinghub.pde.vega.style.v1.service.contract;

import jakarta.validation.constraints.*;
import java.util.Map;

/** Define comandos privados da jornada sem permitir concessões comerciais pelo navegador. */
public final class VegaStyleContract {
  /** Impede instanciação do agrupador de contratos. */
  private VegaStyleContract() {}

  /** Solicita uma sessão sintética vinculada à versão exata de um ciclo aberto. */
  public record Session(@NotNull @Positive Long cycleId, @NotBlank String prototypeVersion) {}

  /** Identifica uma operação idempotente e as respostas declaradas para uma missão. */
  public record Generate(
      @NotBlank String experienceVersion,
      @NotBlank String operationKey,
      @NotNull Map<String, String> answers) {}

  /** Registra a reflexão após uma orientação válida sem alterar o resultado anterior. */
  public record Reflection(@NotNull Map<String, String> answers) {}

  /** Vincula uma ação explícita ao resultado realmente consultado nesta sessão. */
  public record Event(@NotBlank String eventType, String requestId) {}
}
