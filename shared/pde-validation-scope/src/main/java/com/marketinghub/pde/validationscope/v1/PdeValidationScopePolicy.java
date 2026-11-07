package com.marketinghub.pde.validationscope.v1;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Pattern;

/** Responsabilidade: conferir o escopo de homologação pela mesma política no backend e em Atena. */
public final class PdeValidationScopePolicy {
  private static final Pattern COMPARISON =
      Pattern.compile("\\b(?:REFERENCE|REDUCED|PDE_DOCUMENTED_INPUT_COMPARISON_V[1-3])\\b");
  private static final String CONDITION =
      "(?:reference\\s*(?:/|e|ou|versus|vs\\.?)\\s*reduced|reference|reduced|pde_documented_input_comparison_v[1-3])";
  private static final String MATRIX =
      "(?:(?:uma?\\s+)?matriz(?:es)?\\s+(?:de\\s+)?)?(?:18|27|dezoito|vinte e"
          + " sete)(?:\\s+(?:ou|e)\\s+(?:18|27))?\\s+(?:casos|combinacoes|cenarios|execucoes)";
  private static final Pattern MATRIX_COUNT =
      Pattern.compile(
          "\\b(9|18|27|nove|dezoito|vinte e sete)\\s+(?:casos|combinacoes|cenarios|execucoes)\\b");
  private static final String NEGATED_ITEM =
      "(?:sem\\s+(?:(?:a|as|o|os)\\s+)?|nenhum(?:a|as|s)?\\s+(?:(?:dos|das)\\s+)?)(?:(?:condicao|condicoes|comparacao(?:"
          + " documental)?(?: de"
          + " entradas)?|matriz(?:es)?|casos?|testes?|combinacoes)\\s+(?:de\\s+)?)?(?:"
          + CONDITION
          + "|"
          + MATRIX
          + ")";
  private static final String PROHIBITED_ITEM =
      "nao\\s+(?:impor|exigir|executar|realizar)\\s+(?:(?:a|as)\\s+)?(?:(?:comparacao(?:"
          + " documental)?(?: de entradas)?|condicao|condicoes|matriz(?:es)?)\\s*,?\\s*)?(?:"
          + CONDITION
          + "|"
          + MATRIX
          + ")";
  private static final Pattern EXCLUSION =
      Pattern.compile(
          "(?:^|[:,])\\s*(?:inputcomparisonapplicable\\s*=\\s*false\\s*:\\s*)?"
              + "(?:"
              + NEGATED_ITEM
              + "|"
              + PROHIBITED_ITEM
              + ")"
              + "(?:\\s+(?:e|ou|nem)\\s+(?:"
              + NEGATED_ITEM
              + "|"
              + CONDITION
              + "|matrizes adicionais))*\\s*$");
  private static final Pattern IMPLEMENTED_CONDITION =
      Pattern.compile(
          "(?:se houver comparacao documental implementada(?: na mesma candidata)?, executar .*"
              + "|(?:(?:18|27) combinacoes\\s+)?(?:somente|apenas) se implementada a comparacao\\s+"
              + CONDITION
              + ")");
  private static final Pattern CONTRAST =
      Pattern.compile(
          "\\b(?:mas|porem|contudo|entretanto|independentemente|sempre|obrigatoriamente)\\b|mesmo"
              + " sem");

  /** Impede instâncias de uma política determinística sem estado. */
  private PdeValidationScopePolicy() {}

  /** Compara a identidade tipada, aceitando representações inteiras equivalentes de IDs JSON. */
  public static boolean sameCandidate(JsonNode input, JsonNode expected) {
    for (String field : new String[] {"productId", "cycleId", "experimentId"}) {
      if (!sameId(input.path(field), expected.path(field))) return false;
    }
    return input.path("prototypeVersion").isTextual()
        && !input.path("prototypeVersion").asText().isBlank()
        && input.path("prototypeVersion").equals(expected.path("prototypeVersion"));
  }

  /** Exige IDs inteiros positivos sem aceitar ausência, arredondamento ou coerção de texto. */
  private static boolean sameId(JsonNode input, JsonNode expected) {
    return input.isIntegralNumber()
        && expected.isIntegralNumber()
        && input.canConvertToLong()
        && expected.canConvertToLong()
        && input.asLong() > 0
        && input.asLong() == expected.asLong();
  }

  /**
   * Confere saída estruturada quando presente e mantém leitura delimitada das respostas antigas.
   */
  public static Optional<Violation> violation(JsonNode result, JsonNode protocol) {
    if (protocol == null
        || !"PDE_VALIDATION_SCOPE_V1".equals(protocol.path("scopeContractVersion").asText()))
      return Optional.empty();
    if (!protocol.path("inputComparisonApplicable").isBoolean())
      return issue(
          "INVALID_SCOPE",
          "agentValidationExecution.inputComparisonApplicable",
          "O protocolo da candidata está incompleto. Confira os critérios antes de retomar.");
    JsonNode strategy = result.path("marketStrategicContract");
    boolean applicable = protocol.path("inputComparisonApplicable").asBoolean(false);
    JsonNode declared = strategy.path("validationExecutionScope");
    if (!declared.isMissingNode() && !declared.isNull()) {
      var issue = structured(declared, protocol, applicable);
      if (issue.isPresent()) return issue;
    }
    for (String field :
        new String[] {"causalHypothesis", "continueCriteria", "agentValidationPlan"}) {
      var issue = inspect(strategy.path(field), "marketStrategicContract." + field, applicable);
      if (issue.isPresent()) return issue;
    }
    return Optional.empty();
  }

  /**
   * Vincula contagens e condições declaradas à capacidade e à identidade fornecidas pelo backend.
   */
  private static Optional<Violation> structured(
      JsonNode scope, JsonNode protocol, boolean applicable) {
    String path = "marketStrategicContract.validationExecutionScope";
    if (!scope.isObject()
        || !"PDE_VALIDATION_REQUIREMENTS_V1".equals(scope.path("contractVersion").asText()))
      return issue(
          "INVALID_SCOPE",
          path,
          "O plano de homologação está incompleto. Revise os critérios da candidata.");
    for (String field : new String[] {"productId", "cycleId", "experimentId", "prototypeVersion"}) {
      if (!protocol.hasNonNull(field)
          || !(field.equals("prototypeVersion")
              ? scope.path(field).isTextual() && scope.path(field).equals(protocol.path(field))
              : sameId(scope.path(field), protocol.path(field))))
        return issue(
            "SCOPE_IDENTITY_MISMATCH",
            path + "." + field,
            "O plano de homologação pertence a outro contexto. Preserve o produto, ciclo e versão"
                + " desta análise.");
    }
    if (!scope.path("inputComparisonRequired").isBoolean()
        || scope.path("inputComparisonRequired").asBoolean() != applicable)
      return issue(
          "COMPARISON_CAPABILITY_MISMATCH",
          path + ".inputComparisonRequired",
          "A estratégia pede uma comparação fora do protocolo da candidata. Revise os critérios sem"
              + " repetir a análise paga.");
    for (String field :
        new String[] {
          "technicalMatrixRuns", "technicalCombinationCount", "independentExperienceReviewCount"
        }) {
      int expected =
          switch (field) {
            case "technicalCombinationCount" -> applicable ? 18 : 9;
            case "independentExperienceReviewCount" -> 3;
            default -> 1;
          };
      if (!scope.path(field).isIntegralNumber() || scope.path(field).asInt(-1) != expected)
        return issue(
            "MATRIX_SCOPE_MISMATCH",
            path + "." + field,
            "O plano multiplica a homologação além do necessário. Use a matriz da candidata e três"
                + " revisões independentes.");
    }
    return Optional.empty();
  }

  /** Percorre campos e listas sem permitir que uma negação dispense outra obrigação. */
  private static Optional<Violation> inspect(JsonNode value, String path, boolean applicable) {
    if (value.isObject()) {
      var fields = value.fields();
      while (fields.hasNext()) {
        var field = fields.next();
        String childPath = path + "." + field.getKey();
        if ((field.getKey().equals("technicalCombinationCount")
                && (!field.getValue().isIntegralNumber()
                    || field.getValue().asInt(-1) != (applicable ? 18 : 9)))
            || (field.getKey().equals("inputComparisonRequired")
                && (!field.getValue().isBoolean() || field.getValue().asBoolean() != applicable)))
          return issue(
              "COMPARISON_CAPABILITY_MISMATCH",
              childPath,
              "Os critérios de homologação divergem da capacidade da candidata. Preserve o"
                  + " protocolo desta versão.");
        if (!applicable
            && COMPARISON.matcher(field.getKey()).find()
            && !(field.getValue().isBoolean() && !field.getValue().asBoolean()))
          return comparisonIssue(childPath);
        var issue = inspect(field.getValue(), childPath, applicable);
        if (issue.isPresent()) return issue;
      }
    } else if (value.isArray()) {
      for (int i = 0; i < value.size(); i++) {
        var issue = inspect(value.get(i), path + "[" + i + "]", applicable);
        if (issue.isPresent()) return issue;
      }
    } else if (value.isTextual()) {
      for (String sentence : value.asText().split("[.!?;\\r\\n]+")) {
        String normalized = normalize(sentence);
        if (!CONTRAST.matcher(normalized).find()
            && IMPLEMENTED_CONDITION.matcher(normalized).matches()
            && !Pattern.compile(
                    "\\b(?:27|vinte e sete)\\s+(?:casos|combinacoes|cenarios|execucoes)\\b")
                .matcher(normalized)
                .find()) continue;
        for (String clause : normalized.split("\\b(?:mas|porem|contudo|entretanto)\\b")) {
          String remaining = EXCLUSION.matcher(clause.strip()).replaceFirst("").strip();
          if (!applicable
              && COMPARISON.matcher(remaining.toUpperCase(java.util.Locale.ROOT)).find())
            return comparisonIssue(path);
          var counts = MATRIX_COUNT.matcher(remaining);
          while (counts.find()) {
            String count = counts.group(1);
            boolean eighteen = count.equals("18") || count.equals("dezoito");
            boolean twentySeven = count.equals("27") || count.equals("vinte e sete");
            if (twentySeven || eighteen && !applicable)
              return issue(
                  "MATRIX_SCOPE_MISMATCH",
                  path,
                  "O plano exige mais casos que a capacidade da candidata. Preserve a matriz de"
                      + " homologação aplicável.");
          }
        }
      }
    }
    return Optional.empty();
  }

  /** Uniformiza somente a leitura das frases, sem alterar os bytes preservados do parecer. */
  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(java.util.Locale.ROOT)
        .strip();
  }

  /** Cria diagnóstico rastreável separado da mensagem de negócio apresentada na tela. */
  private static Optional<Violation> comparisonIssue(String path) {
    return issue(
        "UNSUPPORTED_INPUT_COMPARISON",
        path,
        "A estratégia pede uma comparação fora do protocolo da candidata. Revise os critérios sem"
            + " repetir a análise paga.");
  }

  /** Representa uma recusa sem incluir JSON, segredo ou stack trace na mensagem visível. */
  private static Optional<Violation> issue(String code, String path, String message) {
    return Optional.of(new Violation(code, path, message));
  }

  /** Responsabilidade: transportar localização técnica e orientação de negócio de uma recusa. */
  public record Violation(String code, String path, String message) {}
}
