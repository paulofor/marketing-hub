package com.marketinghub.pde.mira.privateprototype.v1.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Responsabilidade: ordenar somente usos documentados sem produzir orientação clínica. */
public final class MiraRoutinePolicy {
  private static final List<String> CLINICAL_TERMS =
      List.of("diagnost", "trat", "cur", "prescre", "doenca");

  /** Impede instanciação de uma política determinística sem estado. */
  private MiraRoutinePolicy() {}

  /** Organiza os produtos ou devolve um bloqueio seguro e explicável. */
  public static RoutineDecision organize(String objective, List<ProductInput> products) {
    if (products == null || products.isEmpty()) {
      throw new IllegalArgumentException(
          "Informe ao menos um produto e a orientação documentada do rótulo.");
    }
    String normalizedObjective = normalize(objective);
    if (CLINICAL_TERMS.stream().anyMatch(normalizedObjective::contains)) {
      return RoutineDecision.blocked(
          "O objetivo pede conclusão clínica. Reformule como organização de autocuidado ou procure avaliação profissional.");
    }
    List<RoutineCard> cards = new ArrayList<>();
    for (ProductInput product : products) {
      Integer order = documentedOrder(product.labelDirections());
      if (order == null) {
        return RoutineDecision.blocked(
            "Falta orientação documental suficiente para ordenar "
                + product.name()
                + ". Informe o rótulo ou fabricante.");
      }
      cards.add(
          new RoutineCard(
              product.name(),
              order,
              product.labelDirections(),
              "Ordem limitada ao texto documentado informado; não é prescrição."));
    }
    cards.sort(Comparator.comparingInt(RoutineCard::order));
    return RoutineDecision.ready(List.copyOf(cards));
  }

  /** Classifica somente usos reconhecíveis no texto informado pela cliente. */
  private static Integer documentedOrder(String directions) {
    String value = normalize(directions);
    if (value.contains("apos a limpeza") || value.contains("hidrat")) return 20;
    if (value.contains("limpar") || value.contains("enxagu")) return 10;
    if (value.contains("protetor") || value.contains("protecao solar")) return 30;
    return null;
  }

  /** Normaliza texto para comparar limites sem depender de caixa ou acentos. */
  private static String normalize(String value) {
    String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
  }

  /** Representa uma entrada de produto e sua orientação documental. */
  public record ProductInput(String name, String labelDirections) {}

  /** Representa um item seguro e ordenado da rotina. */
  public record RoutineCard(
      String productName, int order, String documentedDirection, String safetyNote) {}

  /** Representa resultado pronto ou bloqueio sem misturar os dois estados. */
  public record RoutineDecision(List<RoutineCard> routine, String blocker) {
    /** Cria uma decisão pronta para exibição. */
    private static RoutineDecision ready(List<RoutineCard> routine) {
      return new RoutineDecision(routine, null);
    }

    /** Cria uma decisão bloqueada sem conservar resultado parcial. */
    private static RoutineDecision blocked(String blocker) {
      return new RoutineDecision(List.of(), blocker);
    }

    /** Informa se a política recusou a organização solicitada. */
    public boolean blocked() {
      return blocker != null;
    }
  }
}
