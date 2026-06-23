package com.marketinghub.opsmonitor.pipeline.availability;

/** Resultado consolidado de disponibilidade operacional do módulo. */
public record AvailabilityOutput(String moduleCode, String availabilityStatus, String reason) {}
