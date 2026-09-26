package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Responsabilidade: identificar semanticamente a entrada de Íris sem depender da ordem interna de
 * mapas Java ou dos artefatos gerados pela própria comunicação.
 */
final class IrisCommunicationInputFingerprint {
  private static final List<String> VOLATILE_ROOT_FIELDS =
      List.of("communicationInputHash", "communicationArtifacts", "landingInstrumentationContract");
  private static final List<String> DOWNSTREAM_EXPERIMENT_FIELDS =
      List.of("checkoutUrl", "currentLandingHtml");

  /** Impede instanciação; a utilidade mantém somente operações determinísticas. */
  private IrisCommunicationInputFingerprint() {}

  /** Calcula um hash estável após ordenar recursivamente as propriedades dos objetos JSON. */
  static String hash(ObjectMapper json, Object input) {
    try {
      String canonical = canonical(json, input).toString();
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível para a entrada de Íris.", ex);
    }
  }

  /**
   * Compara o conteúdo funcional atual com o snapshot auditado, ignorando campos autorreferentes.
   */
  static boolean equivalent(ObjectMapper json, Object current, Object audited) {
    return canonical(json, current).equals(canonical(json, audited));
  }

  /** Remove campos voláteis da raiz antes de ordenar o documento completo. */
  private static JsonNode canonical(ObjectMapper json, Object input) {
    JsonNode root = input instanceof JsonNode node ? node.deepCopy() : json.valueToTree(input);
    if (root instanceof ObjectNode object) {
      object.remove(VOLATILE_ROOT_FIELDS);
      if (object.path("experiment") instanceof ObjectNode experiment) {
        experiment.remove(DOWNSTREAM_EXPERIMENT_FIELDS);
      }
    }
    return sorted(json, root);
  }

  /** Ordena objetos por chave e preserva a ordem funcional dos arrays. */
  private static JsonNode sorted(ObjectMapper json, JsonNode value) {
    if (value == null || value.isNull()) return json.nullNode();
    if (value.isObject()) {
      ObjectNode result = json.createObjectNode();
      java.util.stream.StreamSupport.stream(
              java.util.Spliterators.spliteratorUnknownSize(
                  value.fieldNames(), java.util.Spliterator.ORDERED),
              false)
          .sorted()
          .forEach(name -> result.set(name, sorted(json, value.get(name))));
      return result;
    }
    if (value.isArray()) {
      ArrayNode result = json.createArrayNode();
      value.forEach(item -> result.add(sorted(json, item)));
      return result;
    }
    if (value.isNumber()) {
      return com.fasterxml.jackson.databind.node.DecimalNode.valueOf(
          value.decimalValue().stripTrailingZeros());
    }
    return value.deepCopy();
  }
}
