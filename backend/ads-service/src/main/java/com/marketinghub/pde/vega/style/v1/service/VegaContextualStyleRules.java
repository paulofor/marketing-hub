package com.marketinghub.pde.vega.style.v1.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Escolhe uma ação verificável somente entre recursos e formas de uso declarados pela cliente. */
public final class VegaContextualStyleRules {
  public static final String VERSION = "musa-pde-entry-v16-modelagem-internacional";
  public static final String MODEL = "MUSA_CONTEXTUAL_STYLE_RULES_V1";

  /** Impede instanciação do catálogo determinístico e independente de persistência. */
  private VegaContextualStyleRules() {}

  /** Reutiliza os três campos e as preferências, sem exigir dados adicionais para a v16. */
  public static void validate(Map<String, String> answers) {
    VegaStyleInput.validate(answers);
  }

  /** Seleciona uma regra aplicável ou explica a insuficiência sem fabricar um resultado útil. */
  public static Decision decide(
      String missionId, Map<String, String> answers, Map<String, String> previous) {
    validate(answers);
    String pieces = normalize(answers.get("existingSelection"));
    String occasion = normalize(answers.get("occasion"));
    String preference = answers.get("stylePreference");
    String all = pieces + " " + occasion;
    if (all.matches(".*\\b(comprar|adquirir|emagrecer|diagnosticar|curar)\\b.*")) {
      return blocked(
          "Esta orientação usa apenas as peças disponíveis. Não recomenda compras nem avalia saúde ou corpo.");
    }
    if (all.matches(".*\\b(nao|nunca|talvez|sem|falta|faltam|emprestado|emprestada)\\b.*")) {
      return blocked(
          "Preciso entender quais peças e formas de usar estão disponíveis. Confira essa informação nos mesmos campos.");
    }
    if ("Quero manter como está".equals(preference)
        && pieces.matches(".*\\b(camisa|camiseta|calca|saia|vestido|macacao|jeans|blusa)\\b.*")) {
      return result(
          "KEEP_DECLARED_CHOICE",
          missionId,
          answers,
          previous,
          "Mantenha a combinação que você informou para "
              + answers.get("occasion")
              + ". Não há correção obrigatória.",
          "Você escolheu manter como está. Sua preferência é suficiente para preservar essa escolha, sem impor um estilo.",
          List.of("stylePreference", "existingSelection", "occasion"));
    }
    boolean scarfChoices =
        pieces.contains("lenco")
            && pieces.contains("bolsa")
            && pieces.contains("pescoco")
            && pieces.contains("alca")
            && pieces.matches(".*\\b(posso|pode|uso|usar)\\b.*");
    if (scarfChoices && occasion.matches(".*\\b(almoco|jantar|refeicao|cozinhar)\\b.*")) {
      return result(
          "SCARF_MEAL_STRAP",
          missionId,
          answers,
          previous,
          "Use o lenço na alça da bolsa, uma das formas que você informou, e mantenha o pescoço livre durante a refeição.",
          "Você tem o lenço e a bolsa, declarou as duas formas de uso e vai fazer uma refeição. Essa escolha mantém o detalhe sem deixá-lo junto ao prato.",
          List.of("existingSelection", "occasion"));
    }
    if (scarfChoices
        && occasion.matches(".*\\b(videochamada|videoconferencia|reuniao online)\\b.*")) {
      boolean discreet =
          "Discrição".equals(preference) || "Conforto e praticidade".equals(preference);
      return result(
          discreet ? "SCARF_VIDEO_STRAP" : "SCARF_VIDEO_NECK",
          missionId,
          answers,
          previous,
          discreet
              ? "Use o lenço na alça da bolsa e mantenha o pescoço livre na videochamada."
              : "Experimente o lenço no pescoço, como você informou que pode usar, antes de entrar na videochamada.",
          discreet
              ? "Entre as duas formas disponíveis, essa atende à sua preferência por "
                  + preference.toLowerCase(Locale.ROOT)
                  + " sem acrescentar peças."
              : "Na videochamada, você pode conferir esse detalhe no enquadramento antes de começar. O lenço e essa forma de usar já fazem parte das suas opções.",
          List.of("existingSelection", "occasion", "stylePreference"));
    }
    if (pieces.contains("camisa")
        && (pieces.contains("mangas dobraveis")
            || pieces.contains("posso dobrar as mangas")
            || pieces.contains("posso dobrar mangas"))
        && occasion.matches(".*\\b(cozinhar|lavar as maos|pintar|mexer com agua)\\b.*")) {
      return result(
          "DECLARED_FOLD_FOR_HANDS",
          missionId,
          answers,
          previous,
          "Dobre as mangas da camisa antes de começar a atividade com as mãos; você informou que elas permitem essa dobra.",
          "A forma de uso foi declarada por você e a ocasião envolve as mãos. Experimente a dobra sem mudar as peças escolhidas.",
          List.of("existingSelection", "occasion"));
    }
    if ((pieces.contains("cardiga") || pieces.contains("jaqueta") || pieces.contains("casaco"))
        && (pieces.contains("posso tirar")
            || pieces.contains("posso remover")
            || pieces.contains("removivel"))
        && occasion.matches(
            ".*(temperatura varia|ambientes com temperaturas diferentes|entrar e sair).*")) {
      return result(
          "DECLARED_REMOVABLE_LAYER",
          missionId,
          answers,
          previous,
          "Mantenha a camada que você disse poder tirar ao alcance. Use ou retire conforme sentir necessidade ao mudar de ambiente.",
          "Você informou uma camada removível e mudanças de ambiente. A ação aproveita essa possibilidade; não pressupõe clima ou temperatura.",
          List.of("existingSelection", "occasion"));
    }
    if (pieces.contains("tenis confortavel")
        && (pieces.contains("bota") || pieces.contains("sapato"))
        && occasion.matches(".*\\b(caminhar|caminhada|andar bastante|passeio a pe)\\b.*")) {
      return result(
          "DECLARED_COMFORT_FOR_WALK",
          missionId,
          answers,
          previous,
          "Escolha o tênis que você descreveu como confortável para o percurso a pé, entre as opções que já tem.",
          "O conforto do tênis foi informado por você e a ocasião inclui caminhar. Não atribuí essa característica aos outros sapatos.",
          List.of("existingSelection", "occasion"));
    }
    return blocked(
        "Ainda não consigo escolher uma ação útil para esta combinação. Conte, nos mesmos campos, uma forma de usar que suas peças permitem e o que você fará na ocasião. Não é preciso comprar nada.");
  }

  /**
   * Acrescenta a missão e os registros da jornada sem trocar a ação contextual por uma orientação
   * genérica.
   */
  private static Decision result(
      String rule,
      String missionId,
      Map<String, String> answers,
      Map<String, String> previous,
      String action,
      String reason,
      List<String> facts) {
    var signals =
        new ArrayList<String>(
            List.of(
                "Sua preferência: " + answers.get("stylePreference"),
                "Sua ocasião: " + answers.get("occasion"),
                "Suas peças: " + answers.get("existingSelection")));
    previous.forEach(
        (key, value) -> {
          if (key.endsWith(".reflection") && !value.isBlank())
            signals.add("Seu registro anterior: " + value);
        });
    var actions = new ArrayList<String>();
    actions.add(action);
    actions.add(
        switch (missionId) {
          case "estilo-dia-2" -> "Relembre o primeiro teste e registre o que vale repetir.";
          case "estilo-dia-3" ->
              "Compare esta forma de usar com seu registro anterior. Você escolhe o que manter.";
          case "estilo-dia-4" ->
              "Observe como a escolha funciona na sua rotina e registre o que quer adaptar.";
          case "estilo-dia-5" ->
              "Observe as cores que você já escolheu e anote uma relação que gostaria de repetir.";
          case "estilo-dia-6" ->
              "Registre sua fórmula: peças escolhidas, forma de usar e ocasião em que funciona.";
          case "estilo-dia-7" ->
              "Revise seus registros e escolha uma fórmula para repetir na próxima semana.";
          default ->
              "Experimente e observe se isso combina com o que você gosta e precisa fazer hoje.";
        });
    return new Decision(
        "COMPLETED",
        rule,
        "Um ajuste para a sua situação",
        reason,
        List.copyOf(signals),
        List.copyOf(actions),
        facts,
        "");
  }

  /** Explica o limite funcional sem título, ação ou sinal de resultado pronto. */
  private static Decision blocked(String reason) {
    return new Decision(
        "BLOCKED", "INPUT_UNSUPPORTED", "", "", List.of(), List.of(), List.of(), reason);
  }

  /** Normaliza somente para comparação; a auditoria preserva a entrada original. */
  private static String normalize(String value) {
    return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("\\s+", " ")
        .trim();
  }

  /**
   * Registra a decisão, os campos que a sustentam e uma limitação explícita quando não há
   * cobertura.
   */
  public record Decision(
      String status,
      String ruleId,
      String headline,
      String summary,
      List<String> signals,
      List<String> actions,
      List<String> supportingFields,
      String limitation) {}
}
