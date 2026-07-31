package com.marketinghub.experiment.service.cockpit;

import java.util.List;

/** Saúde operacional e comercial antes de interpretar o resultado de mercado. */
public record ExperimentCockpitHealthDto(
    String status, String headline, String description, List<String> blockers) {}
