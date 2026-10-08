package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;

/** Responsabilidade: simular somente o modelo para os testes locais de diferentes PDEs. */
final class BpmScenarioModelFixture {
  /** Produz resposta segregada do contexto fornecido, sem usar credenciais ou chamar IA. */
  static Path create(
      Path directory,
      ObjectMapper json,
      JsonNode target,
      String sourceReference,
      String scenario,
      long evidenceId)
      throws Exception {
    var result = json.createObjectNode();
    result.put("contractVersion", "PDE_PSIQUE_AGENT_SCENARIO_V1");
    result.put("decision", "APPROVED");
    result.put("scenarioCode", scenario);
    result.put("sourceReference", sourceReference);
    result.put("productId", target.path("productId").asLong());
    result.put("productSlug", target.path("productSlug").asText());
    result.put("prototypeVersion", target.path("experienceVersion").asText());
    result.put("trafficClass", "AGENT_VALIDATION");
    result.put("internalMarker", "mh_internal_test");
    result.put("syntheticEvaluation", true);
    result.put("humanEvidenceClaimed", false);
    result.put("commercialEvidenceClaimed", false);
    var assessment = result.putObject("experienceAssessment");
    for (String field :
        List.of("comprehension", "effort", "utility", "trust", "pleasure", "nextStepClarity"))
      assessment.put(
          field,
          "Avaliação simulada do percurso e dos pixels locais, sem comportamento humano observado.");
    assessment.putArray("objections");
    assessment.put(
        "evidenceBoundary",
        "Modelo simulado apenas na sandbox; não representa aprovação de produção.");
    var checks = result.putObject("checks");
    for (String check :
        List.of(
            "sameProductAndVersion",
            "isolatedFreshSession",
            "functionalOutcomeMatchesScenario",
            "lowEffortNoPrompting",
            "accessibilityAndResponsive",
            "privacyPreserved",
            "internalTrafficSegregated",
            "safeLimits",
            "noExternalSideEffects")) checks.put(check, true);
    result.set(
        "sideEffects",
        json.valueToTree(
            Map.of(
                "paymentEnabled",
                false,
                "published",
                false,
                "campaignCreated",
                false,
                "mediaSpendBrl",
                0)));
    var visual = result.putObject("visualAudit");
    visual.putArray("evidenceIds").add(evidenceId);
    for (String field : List.of("visualHierarchy", "legibility", "affectiveResponse", "trustCues"))
      visual.put(field, "Verificação sintética dos pixels gerados pelo navegador local.");
    result.putArray("evidence").add("Captura local " + evidenceId);
    result.putArray("requiredChanges");
    result.put(
        "rootCause", "Percurso local confirmado pelo harness; interpretação do modelo simulada.");
    Path fixture = directory.resolve("result.json");
    Files.writeString(fixture, json.writeValueAsString(result));
    Path implementation = directory.resolve("fixture-model.py");
    Files.writeString(
        implementation,
        """
        import json,pathlib,sys
        root=pathlib.Path(__file__).parent
        prompt=sys.stdin.read()
        (root/'prompt.txt').write_text(prompt)
        context=json.loads(next(line for line in prompt.splitlines() if line.startswith('{') and '"agentScenarioExecution"' in line))
        result=json.loads((root/'result.json').read_text())
        result['visualAudit']['captureSessionId']=context['visualCapture']['captureSessionId']
        schema=json.loads(pathlib.Path(sys.argv[sys.argv.index('--output-schema')+1]).read_text())
        assert set(result)==set(schema['properties'])
        for field in ('sideEffects','experienceAssessment','checks','visualAudit'):
            assert set(result[field])==set(schema['properties'][field]['required'])
        pathlib.Path(sys.argv[sys.argv.index('--output-last-message')+1]).write_text(json.dumps(result))
        """);
    Path executable = directory.resolve("fake-model.sh");
    Files.writeString(executable, "#!/bin/sh\nexec python3 " + quote(implementation) + " \"$@\"\n");
    assertThat(executable.toFile().setExecutable(true)).isTrue();
    return executable;
  }

  /** Protege caminhos locais, inclusive espaços e apóstrofos, sem interpolação do shell. */
  private static String quote(Path file) {
    return "'" + file.toString().replace("'", "'\\''") + "'";
  }
}
