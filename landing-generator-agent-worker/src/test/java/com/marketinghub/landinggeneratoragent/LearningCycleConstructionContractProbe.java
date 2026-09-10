package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Responsabilidade: validar o contrato produzido pelo backend com o consumidor real de Dédalo. */
public class LearningCycleConstructionContractProbe {
  /** Confirma as três entradas de construção e recusa mistura de versões antes de chamar IA. */
  public static void main(String[] args) throws Exception {
    var mapper = new ObjectMapper();
    Map<String, Object> task = mapper.readValue(Files.readString(Path.of(args[0])), Map.class);
    for (String activity : java.util.List.of("journey", "deliverables", "access")) {
      var contract =
          new PdeConstructionBpmTaskConsumer.BpmContract(
              "pde-construction-approval", activity, "prompt", "schema", "v2", "READY");
      PdeConstructionBpmTaskConsumer.validateTaskContext(task, contract, mapper);
      Map<String, Object> invalid = mapper.readValue(mapper.writeValueAsString(task), Map.class);
      ((Map<String, Object>) invalid.get("taskTarget")).put("experienceVersion", "historical-v7");
      boolean rejected = false;
      try {
        PdeConstructionBpmTaskConsumer.validateTaskContext(invalid, contract, mapper);
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      if (!rejected) throw new AssertionError("Versão incompatível foi aceita: " + activity);
    }
    System.out.println(
        "PASS contrato backend → Dédalo: três atividades e três recusas de versão; sem chamada ao modelo");
  }
}
