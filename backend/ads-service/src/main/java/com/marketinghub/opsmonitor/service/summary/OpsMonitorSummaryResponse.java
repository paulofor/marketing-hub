package com.marketinghub.opsmonitor.service.summary;

/** Resumo executivo de saúde operacional dos módulos monitorados. */
public record OpsMonitorSummaryResponse(long online, long degraded, long offline, long unknown, long openIncidents) {}
