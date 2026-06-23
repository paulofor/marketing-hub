package com.marketinghub.opsmonitor.pipeline.logscan;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita propriedades da etapa de leitura de logs. */
@Configuration
@EnableConfigurationProperties(LogScanProperties.class)
public class LogScanConfiguration {}
