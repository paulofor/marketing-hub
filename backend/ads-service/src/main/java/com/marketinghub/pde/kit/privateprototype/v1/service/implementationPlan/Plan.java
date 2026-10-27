package com.marketinghub.pde.kit.privateprototype.v1.service.implementationPlan;

import java.util.List;

/** Responsabilidade: apresentar a seleção atual e a identidade do desenho imutável. */
public record Plan(
    long revision,
    Long architectureTaskId,
    String architectureSha256,
    String boundProfileCode,
    List<String> installedProfiles) {}
