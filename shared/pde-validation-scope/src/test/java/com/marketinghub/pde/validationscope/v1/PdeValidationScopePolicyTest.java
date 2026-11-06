package com.marketinghub.pde.validationscope.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: executar a mesma regressão de escopo nos dois módulos consumidores. */
class PdeValidationScopePolicyTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Cobre negações, obrigações, contradições, condicionais e as matrizes dos registros antigos. */
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "inputComparisonApplicable=false: nenhuma condição REFERENCE/REDUCED e nenhuma matriz de 18"
            + " ou 27 casos.|false",
        "Nenhum caso REFERENCE/REDUCED.|false",
        "Nenhum dos casos REFERENCE/REDUCED.|false",
        "Nenhuma das condições REFERENCE/REDUCED.|false",
        "Nenhuma comparação REFERENCE/REDUCED.|false",
        "Nenhuma condição REFERENCE/REDUCED.|false",
        "Sem REFERENCE/REDUCED.|false",
        "Não exigir REFERENCE/REDUCED.|false",
        "Nenhum caso REFERENCE e nenhum caso REDUCED.|false",
        "Nenhuma condição REFERENCE/REDUCED. Exigir REDUCED.|true",
        "Nenhuma condição REFERENCE/REDUCED, mas executar REDUCED.|true",
        "Nenhuma condição REFERENCE/REDUCED é opcional.|true",
        "Não deixar de exigir REFERENCE/REDUCED.|true",
        "Não homologar sem REFERENCE/REDUCED.|true",
        "Não exigir REFERENCE/REDUCED se faltar orçamento.|true",
        "Comparar REFERENCE/REDUCED somente se houver orçamento.|true",
        "Nenhum caso REFERENCE/REDUCED se não houver orçamento.|true",
        "18 combinações somente se implementada a comparação REFERENCE/REDUCED.|false",
        "Se houver comparação documental implementada na mesma candidata, executar 18 combinações"
            + " REFERENCE/REDUCED.|false",
        "Se houver comparação documental implementada, executar REFERENCE/REDUCED; exigir REDUCED"
            + " mesmo sem implementação.|true",
        "Se houver comparação documental implementada, executar REFERENCE/REDUCED, mas exigir"
            + " REDUCED.|true",
        "Executar nove combinações: três cenários por três dispositivos.|false",
        "Executar 9 casos.|false",
        "Executar 18 casos.|true",
        "Executar 27 combinações.|true",
        "27 combinações somente se implementada a comparação REFERENCE/REDUCED.|true",
        "Se houver comparação documental implementada, executar 27 combinações"
            + " REFERENCE/REDUCED.|true",
        "Nenhuma matriz de 18 ou 27 casos.|false",
        "Sem 18 combinações.|false",
        "Comparar REFERENCE e REDUCED em 18 combinações.|true",
        "Comparar as ofertas de Whering e Indyx como fontes públicas de mercado.|false"
      })
  void checksLegacyClauses(String text, boolean blocked) {
    var result = result();
    result.withObject("/marketStrategicContract/agentValidationPlan").put("continueCriteria", text);
    assertThat(PdeValidationScopePolicy.violation(result, protocol(false)).isPresent())
        .isEqualTo(blocked);
  }

  /** Preserva a separação entre campos, listas e flags explícitas de condições desabilitadas. */
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"REFERENCE\":false,\"REDUCED\":false}|false",
        "{\"REFERENCE\":true,\"REDUCED\":false}|true",
        "{\"conditions\":[\"REFERENCE\",\"REDUCED\"]}|true",
        "{\"conditions\":[]}|false",
        "{\"technicalCombinationCount\":18}|true",
        "{\"technicalCombinationCount\":9}|false",
        "{\"technicalCombinationCount\":27}|true",
        "{\"inputComparisonRequired\":true}|true",
        "{\"inputComparisonRequired\":false}|false",
        "{\"nested\":[{\"criterion\":\"Nenhuma condição"
            + " REFERENCE/REDUCED\"},{\"criterion\":\"Exigir REDUCED\"}]}|true",
        "{\"nested\":[{\"criterion\":\"Nenhum caso REFERENCE/REDUCED\"},{\"criterion\":\"9"
            + " casos\"}]}|false"
      })
  void traversesStructuredValues(String value, boolean blocked) throws Exception {
    var result = result();
    result.withObject("/marketStrategicContract").set("agentValidationPlan", json.readTree(value));
    assertThat(PdeValidationScopePolicy.violation(result, protocol(false)).isPresent())
        .isEqualTo(blocked);
  }

  /** Vincula a saída nova ao protocolo exato e conserva obrigações contraditórias na prosa. */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void validatesStructuredScopeAndContradictions(boolean comparison) {
    var result = result();
    var protocol = protocol(comparison);
    var scope = protocol.deepCopy();
    scope.put("contractVersion", "PDE_VALIDATION_REQUIREMENTS_V1");
    scope.remove("scopeContractVersion");
    scope.remove("inputComparisonApplicable");
    scope.put("inputComparisonRequired", comparison);
    scope.put("technicalMatrixRuns", 1);
    scope.put("technicalCombinationCount", comparison ? 18 : 9);
    scope.put("independentExperienceReviewCount", 3);
    result.withObject("/marketStrategicContract").set("validationExecutionScope", scope);
    assertThat(PdeValidationScopePolicy.violation(result, protocol)).isEmpty();
    for (String field : new String[] {"productId", "cycleId", "experimentId", "prototypeVersion"}) {
      var original = scope.get(field);
      scope.put(field, "outra-identidade");
      assertThat(PdeValidationScopePolicy.violation(result, protocol).orElseThrow().code())
          .isEqualTo("SCOPE_IDENTITY_MISMATCH");
      scope.set(field, original);
    }
    scope.put("technicalCombinationCount", 27);
    assertThat(PdeValidationScopePolicy.violation(result, protocol)).isPresent();
    scope.put("technicalCombinationCount", comparison ? 18 : 9);
    result
        .withObject("/marketStrategicContract/agentValidationPlan")
        .put("continueCriteria", comparison ? "Executar 27 casos" : "Executar REFERENCE/REDUCED");
    assertThat(PdeValidationScopePolicy.violation(result, protocol)).isPresent();
  }

  /** Mantém comparação implementada, mas não permite substituir sua matriz por 27 casos. */
  @Test
  void preservesImplementedComparisonAndHistoricalAbsence() {
    var result = result();
    result
        .withObject("/marketStrategicContract")
        .put("causalHypothesis", "Comparar REFERENCE e REDUCED em 18 combinações.");
    assertThat(PdeValidationScopePolicy.violation(result, protocol(true))).isEmpty();
    assertThat(PdeValidationScopePolicy.violation(result, json.createObjectNode())).isEmpty();
    result.withObject("/marketStrategicContract").put("continueCriteria", "Executar 27 casos.");
    assertThat(PdeValidationScopePolicy.violation(result, protocol(true))).isPresent();
  }

  /** Cria estratégia independente de nomes de produto e identificadores produtivos. */
  private ObjectNode result() {
    var result = json.createObjectNode().put("decision", "APPROVE");
    result.putObject("marketStrategicContract");
    return result;
  }

  /** Cria o protocolo oficial com identidade sintética e capacidade declarada pelo backend. */
  private ObjectNode protocol(boolean comparison) {
    return json.createObjectNode()
        .put("scopeContractVersion", "PDE_VALIDATION_SCOPE_V1")
        .put("productId", 981L)
        .put("cycleId", 982L)
        .put("experimentId", 983L)
        .put("prototypeVersion", "produto-sintetico-v4")
        .put("inputComparisonApplicable", comparison);
  }
}
