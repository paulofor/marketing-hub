package com.marketinghub.opsmonitor.pipeline.availability;

/** Entrada usada para consolidar a disponibilidade de um módulo. */
public record AvailabilityInput(String moduleCode, boolean lastCheckSuccessful, int consecutiveFailures, long responseTimeMs, int offlineThresholdFailures) {}
