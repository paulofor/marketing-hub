package com.marketinghub.businessprocess.automation.v1.service.continuity;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Responsabilidade: comparar contratos antes de reutilizar uma entrega entre versões do BPM. */
public final class ProcessContractCompatibility {
  /** Impede instanciação de uma política sem estado. */
  private ProcessContractCompatibility() {}

  /** Exige igualdade semântica dos campos; mudança de descrição também exige revalidação. */
  public static List<String> differences(JsonNode source, JsonNode target) {
    if (source == null
        || target == null
        || !source.isObject()
        || !target.isObject()
        || source.isEmpty()
        || target.isEmpty()) return List.of("/");
    var changed = new ArrayList<String>();
    compare("", source, target, changed);
    return List.copyOf(changed);
  }

  /**
   * Identifica campos alterados sem confundir ordenação de propriedades com mudança de contrato.
   */
  private static void compare(String path, JsonNode source, JsonNode target, List<String> changed) {
    if (Objects.equals(source, target)) return;
    if (source != null && target != null && source.isObject() && target.isObject()) {
      var names = new java.util.TreeSet<String>();
      source.fieldNames().forEachRemaining(names::add);
      target.fieldNames().forEachRemaining(names::add);
      for (var name : names)
        compare(path + "/" + name, source.get(name), target.get(name), changed);
    } else changed.add(path.isEmpty() ? "/" : path);
  }
}
