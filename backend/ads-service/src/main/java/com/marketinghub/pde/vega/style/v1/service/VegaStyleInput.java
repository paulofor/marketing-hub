package com.marketinghub.pde.vega.style.v1.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Valida os três campos da jornada de estilo sem ampliar a coleta de informações. */
public final class VegaStyleInput {
  public static final List<String> PREFERENCES =
      List.of(
          "Conforto e praticidade",
          "Cor e criatividade",
          "Discrição",
          "Delicadeza",
          "Elegância",
          "Ainda estou descobrindo",
          "Quero manter como está");
  private static final Set<String> REQUIRED =
      Set.of("occasion", "existingSelection", "stylePreference");
  private static final Set<String> ALLOWED =
      Set.of("occasion", "existingSelection", "stylePreference", "reflection");

  /** Mantém a validação independente do estado e dos endpoints. */
  private VegaStyleInput() {}

  /** Recusa omissões, opções desconhecidas e campos fora do contrato antes de gerar ou salvar. */
  public static void validate(Map<String, String> answers) {
    if (answers == null
        || !answers.keySet().containsAll(REQUIRED)
        || !ALLOWED.containsAll(answers.keySet())) throw invalid();
    for (var entry : answers.entrySet()) {
      if (entry.getValue() == null
          || entry.getValue().length() > (entry.getKey().equals("occasion") ? 160 : 300)
          || (REQUIRED.contains(entry.getKey()) && entry.getValue().isBlank())) throw invalid();
    }
    if (!PREFERENCES.contains(answers.get("stylePreference"))) throw invalid();
  }

  /** Expõe uma orientação curta sem refletir o conteúdo recebido. */
  private static ResponseStatusException invalid() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Confira ocasião, peças disponíveis e preferência. Os demais dados foram preservados.");
  }
}
