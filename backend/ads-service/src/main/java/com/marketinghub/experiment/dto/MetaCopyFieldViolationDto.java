package com.marketinghub.experiment.dto;

/** Expõe a contagem auditável de um campo textual que violou o contrato da Meta. */
public record MetaCopyFieldViolationDto(String field, int actualLength, int maxLength) {}
