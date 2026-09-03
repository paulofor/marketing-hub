package com.marketinghub.product.service.valuechainposition;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Responsabilidade: traduzir estados comerciais em códigos dos macroprocessos PDE. */
@Component
public class PdeProcessCodeResolver {
  private static final Map<String, String> LEGACY_STATUS_PROCESS_CODES =
      Map.ofEntries(
          Map.entry("OPORTUNIDADE_EM_DESCOBERTA", "pde-opportunity-discovery"),
          Map.entry("DESCOBERTA_E_PRIORIZACAO", "pde-opportunity-discovery"),
          Map.entry("IDEIA_PRIORIZADA_PARA_TESTE", "pde-commercial-plan-offer"),
          Map.entry("PLANO_COMERCIAL", "pde-commercial-plan-offer"),
          Map.entry("PLANO_COMERCIAL_E_OFERTA", "pde-commercial-plan-offer"),
          Map.entry("PLANNED", "pde-construction-approval"),
          Map.entry("CONSTRUCAO_E_APROVACAO", "pde-construction-approval"),
          Map.entry("COMUNICACAO_E_JORNADA", "pde-communication-sales-journey"),
          Map.entry("COMUNICACAO_E_JORNADA_DE_VENDA", "pde-communication-sales-journey"),
          Map.entry("VALIDACAO_COMERCIAL", "pde-commercial-homologation-activation"),
          Map.entry("HOMOLOGACAO_E_ATIVACAO", "pde-commercial-homologation-activation"),
          Map.entry("HOMOLOGACAO_COMERCIAL_E_ATIVACAO", "pde-commercial-homologation-activation"),
          Map.entry("ATIVO", "pde-sales-delivery-learning"),
          Map.entry("ACTIVE", "pde-sales-delivery-learning"),
          Map.entry("RUNNING", "pde-sales-delivery-learning"),
          Map.entry("ESCALA", "pde-sales-delivery-learning"),
          Map.entry("ESCALANDO", "pde-sales-delivery-learning"),
          Map.entry("VENDA_ENTREGA_E_APRENDIZADO", "pde-sales-delivery-learning"));

  /** Resolve o código canônico aceitando estados legados e o próprio código publicado. */
  public String resolve(String commercialStatus, Set<String> publishedProcessCodes) {
    String normalizedStatus = normalize(commercialStatus);
    if (normalizedStatus == null) {
      return null;
    }
    return publishedProcessCodes.stream()
        .filter(processCode -> normalize(processCode).equals(normalizedStatus))
        .findFirst()
        .orElse(LEGACY_STATUS_PROCESS_CODES.get(normalizedStatus));
  }

  /** Normaliza acentos e separadores usados por estados comerciais antigos. */
  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .trim()
        .toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
  }
}
