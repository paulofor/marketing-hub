package com.marketinghub.businessprocess.independent.service.catalog;

import java.util.List;

/** Contrato de um campo de entrada declarado pelo backend para uma execução independente. */
public record IndependentBusinessProcessInputFieldResponse(
    String key,
    String label,
    String controlType,
    boolean required,
    Integer maxLength,
    String defaultValue,
    String helpText,
    List<IndependentBusinessProcessInputOptionResponse> options) {

  /** Mantém compatibilidade com campos textuais publicados antes dos controles seletivos. */
  public IndependentBusinessProcessInputFieldResponse(
      String key,
      String label,
      String controlType,
      boolean required,
      Integer maxLength,
      String defaultValue,
      String helpText) {
    this(key, label, controlType, required, maxLength, defaultValue, helpText, List.of());
  }
}
