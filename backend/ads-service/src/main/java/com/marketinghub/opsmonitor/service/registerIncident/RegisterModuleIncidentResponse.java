package com.marketinghub.opsmonitor.service.registerIncident;

/** Confirma o incidente operacional persistido pelo backend. */
public record RegisterModuleIncidentResponse(Long incidentId, String moduleCode, String status, String severity) {}
