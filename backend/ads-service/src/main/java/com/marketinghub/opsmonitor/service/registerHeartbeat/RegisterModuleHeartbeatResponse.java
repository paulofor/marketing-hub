package com.marketinghub.opsmonitor.service.registerHeartbeat;

/** Confirma o registro de heartbeat operacional recebido do worker. */
public record RegisterModuleHeartbeatResponse(Long healthCheckId, String moduleCode, String status) {}
