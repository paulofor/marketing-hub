package com.marketinghub.opsmonitor.pipeline.healthcheck;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita as propriedades da etapa de health check. */
@Configuration
@EnableConfigurationProperties(HealthCheckProperties.class)
public class HealthCheckConfiguration {}
