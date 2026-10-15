package com.marketinghub.communicationagentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Exige fala pronta no briefing de Íris, separada da direção visual e das provas internas. */
final class AudiovisualBriefContract {
  private static final List<String> SECTIONS =
      List.of("[ABERTURAS]", "[FALA_FINAL]", "[PROVA_E_LIMITES]", "[DIRECAO_VISUAL]");

  /** Impede instanciar o contrato sem estado. */
  private AudiovisualBriefContract() {}

  /** Preserva o fluxo sem vídeo e recusa briefing audiovisual sem conteúdo em cada seção. */
  static void validate(JsonNode value) {
    if (value.isNull() || value.isMissingNode()) return;
    if (!value.isTextual() || value.asText().isBlank())
      throw new IllegalArgumentException(
          "Briefing audiovisual vazio; use null quando não há vídeo.");
    String brief = value.asText();
    for (String section : SECTIONS) {
      int start = brief.indexOf(section);
      if (start < 0 || brief.indexOf(section, start + section.length()) >= 0)
        throw new IllegalArgumentException(
            "Briefing audiovisual exige uma seção " + section + " com conteúdo utilizável.");
      start += section.length();
      int end = brief.length();
      for (String next : SECTIONS) {
        int at = brief.indexOf(next, start);
        if (at >= 0) end = Math.min(end, at);
      }
      if (brief.substring(start, end).replaceAll("[\\p{P}\\p{Z}\\s]+", "").isBlank())
        throw new IllegalArgumentException("Seção audiovisual vazia: " + section);
    }
  }
}
