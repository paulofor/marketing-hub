package com.marketinghub.opala.commercial.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Responsabilidade: comparar semanticamente os ativos comerciais revisados pelo Opala. */
final class OpalaCommercialAssetComparator {
  /** Impede instanciação porque o contrato de comparação não mantém estado. */
  private OpalaCommercialAssetComparator() {}

  /**
   * Compara os ativos efetivamente revisados sem confundir ordenação técnica de coleções com uma
   * mudança comercial.
   */
  static boolean sameReviewableAssets(JsonNode previous, JsonNode current) {
    return sameRequiredNumber(previous, current, "productId")
        && sameRequiredNumber(previous, current, "experimentId")
        && sameRequiredNumber(previous, current, "cycleId")
        && sameRequiredText(previous, current, "contractVersion")
        && sameRequiredText(previous, current, "productVersion")
        && sameOptionalText(previous, current, "destinationUrl")
        && sameRequiredText(previous, current, "destinationSource")
        && sameScopedDecimal(previous.path("priceBrl"), current.path("priceBrl"))
        && sameOptionalText(previous, current, "checkoutUrl")
        && sameScopedDecimal(previous.path("budgetLimitBrl"), current.path("budgetLimitBrl"))
        && sameRequiredText(previous, current, "windowEnd")
        && sameScopedJson(previous.path("productContract"), current.path("productContract"))
        && sameFinancialPlanReviewState(
            previous.path("financialPlan"), current.path("financialPlan"))
        && sameUnorderedAssetArray(previous.path("savedAudience"), current.path("savedAudience"))
        && sameUnorderedAssetArray(previous.path("slots"), current.path("slots"))
        && sameUnorderedAssetArray(previous.path("creatives"), current.path("creatives"))
        && sameUnorderedAssetArray(previous.path("approvedVideos"), current.path("approvedVideos"))
        && sameUnorderedAssetArray(
            previous.path("approvedAudienceElements"), current.path("approvedAudienceElements"))
        && sameRequiredBoolean(previous, current, "publicationAuthorized")
        && sameRequiredBoolean(previous, current, "mediaSpendAuthorized");
  }

  /** Compara um identificador numérico obrigatório da mesma ocorrência comercial. */
  private static boolean sameRequiredNumber(JsonNode previous, JsonNode current, String field) {
    JsonNode previousValue = previous.path(field);
    if (previousValue.isMissingNode() || previousValue.isNull()) return true;
    return previousValue.isNumber()
        && current.path(field).isNumber()
        && previousValue.decimalValue().compareTo(current.path(field).decimalValue()) == 0;
  }

  /** Compara um texto obrigatório que identifica o contrato comercial avaliado. */
  private static boolean sameRequiredText(JsonNode previous, JsonNode current, String field) {
    JsonNode previousValue = previous.path(field);
    if (previousValue.isMissingNode() || previousValue.isNull()) return true;
    return previousValue.isTextual()
        && current.path(field).isTextual()
        && Objects.equals(previousValue.textValue(), current.path(field).textValue());
  }

  /** Compara uma URL ou outro texto opcional sem transformar ausência em ativo válido. */
  private static boolean sameOptionalText(JsonNode previous, JsonNode current, String field) {
    JsonNode previousValue = previous.path(field);
    JsonNode currentValue = current.path(field);
    if (previousValue.isMissingNode()) return true;
    if (previousValue.isNull()) return currentValue.isMissingNode() || currentValue.isNull();
    return previousValue.isTextual()
        && currentValue.isTextual()
        && Objects.equals(previousValue.textValue(), currentValue.textValue());
  }

  /** Compara um marcador de autorização que também modifica o risco da revisão comercial. */
  private static boolean sameRequiredBoolean(JsonNode previous, JsonNode current, String field) {
    JsonNode previousValue = previous.path(field);
    if (previousValue.isMissingNode() || previousValue.isNull()) return true;
    return previousValue.isBoolean()
        && current.path(field).isBoolean()
        && previousValue.booleanValue() == current.path(field).booleanValue();
  }

  /**
   * Compara uma coleção de ativos por conteúdo, preservando mudanças reais e ignorando apenas a
   * ordem semântica inexistente retornada pelo banco.
   */
  private static boolean sameUnorderedAssetArray(JsonNode previous, JsonNode current) {
    if (previous.isMissingNode()) return true;
    if (previous.isNull()) return current.isMissingNode() || current.isNull();
    if (!previous.isArray() || !current.isArray()) return false;
    return canonicalAssetEntries(previous).equals(canonicalAssetEntries(current));
  }

  /**
   * Serializa cada ativo de forma canônica para comparar coleções sem depender da ordenação SQL.
   */
  private static List<String> canonicalAssetEntries(JsonNode values) {
    List<String> entries = new ArrayList<>();
    values.forEach(value -> entries.add(canonicalJson(value)));
    entries.sort(Comparator.naturalOrder());
    return entries;
  }

  /** Normaliza chaves de objetos, números e subestruturas antes da comparação de um ativo. */
  private static String canonicalJson(JsonNode value) {
    if (value.isNumber()) return value.decimalValue().stripTrailingZeros().toPlainString();
    if (value.isArray()) {
      List<String> items = new ArrayList<>();
      value.forEach(item -> items.add(canonicalJson(item)));
      return "[" + String.join(",", items) + "]";
    }
    if (!value.isObject()) return value.toString();
    List<String> fields = new ArrayList<>();
    value.fieldNames().forEachRemaining(fields::add);
    fields.sort(Comparator.naturalOrder());
    List<String> entries = new ArrayList<>();
    for (String field : fields)
      entries.add(new TextNode(field) + ":" + canonicalJson(value.path(field)));
    return "{" + String.join(",", entries) + "}";
  }

  /** Compara um número somente quando ele já fazia parte do escopo recebido pelo revisor. */
  private static boolean sameScopedDecimal(JsonNode previous, JsonNode current) {
    return previous.isMissingNode() || sameDecimal(previous, current);
  }

  /** Compara uma estrutura somente quando ela já fazia parte do escopo recebido pelo revisor. */
  private static boolean sameScopedJson(JsonNode previous, JsonNode current) {
    return previous.isMissingNode() || previous.equals(current);
  }

  /** Compara a revisão financeira e o estado de validade que limitam a aprovação comercial. */
  private static boolean sameFinancialPlanReviewState(JsonNode previous, JsonNode current) {
    return sameFinancialPlanRevision(previous, current)
        && sameRequiredText(previous, current, "status");
  }

  /**
   * Compara a identidade imutável do plano sem invalidar números JSON equivalentes após leitura.
   */
  static boolean sameFinancialPlanRevision(JsonNode previous, JsonNode current) {
    return previous.path("id").isIntegralNumber()
        && current.path("id").isIntegralNumber()
        && previous.path("revision").isIntegralNumber()
        && current.path("revision").isIntegralNumber()
        && previous.path("id").longValue() == current.path("id").longValue()
        && previous.path("revision").intValue() == current.path("revision").intValue();
  }

  /** Compara valores financeiros pelo valor decimal, sem depender do tipo numérico do JSON. */
  static boolean sameDecimal(JsonNode previous, JsonNode current) {
    return previous.isNumber()
        && current.isNumber()
        && previous.decimalValue().compareTo(current.decimalValue()) == 0;
  }
}
