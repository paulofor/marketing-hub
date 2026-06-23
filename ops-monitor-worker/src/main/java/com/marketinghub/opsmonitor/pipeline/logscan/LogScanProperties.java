package com.marketinghub.opsmonitor.pipeline.logscan;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Guarda termos configuráveis de busca em logs. */
@ConfigurationProperties(prefix = "ops-monitor.log-scan")
public record LogScanProperties(List<String> keywords) {}
