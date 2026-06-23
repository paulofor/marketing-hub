package com.marketinghub.opsmonitor.pipeline.availability;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita propriedades da etapa de disponibilidade. */
@Configuration
@EnableConfigurationProperties(AvailabilityProperties.class)
public class AvailabilityConfiguration {}
