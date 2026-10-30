package com.marketinghub.pde.kit.privateprototype.v1.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: limitar a fila aos perfis explicitamente instalados no compositor. */
public final class KitCompositorCapabilities {
  public static final String HEADER = "X-Kit-Supported-Profiles";

  /** Impede instanciação da política compartilhada da fila e da reserva. */
  private KitCompositorCapabilities() {}

  /** Mantém executores antigos sem trabalho e recusa declarações de capacidade inválidas. */
  public static Set<String> parse(String declaration) {
    if (declaration == null || declaration.isBlank()) return Set.of();
    if (declaration.length() > 512) throw invalid();
    Set<String> profiles =
        Arrays.stream(declaration.split(",", -1))
            .map(String::trim)
            .collect(Collectors.toUnmodifiableSet());
    if (profiles.isEmpty()
        || profiles.stream()
            .anyMatch(
                code -> KitSpecialties.isFamily(code) || KitSpecialties.profiles(code).isEmpty()))
      throw invalid();
    return profiles;
  }

  /** Explica a incompatibilidade sem consumir uma reserva nem expor credenciais. */
  private static ResponseStatusException invalid() {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "O compositor precisa declarar somente os perfis instalados e compatíveis.");
  }
}
