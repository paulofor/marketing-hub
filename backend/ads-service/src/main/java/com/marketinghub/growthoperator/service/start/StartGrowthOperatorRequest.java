package com.marketinghub.growthoperator.service.start;

/** Responsabilidade: receber a semana e o objetivo de um diagnostico autonomo. */
public record StartGrowthOperatorRequest(Integer weekNumber, String objective) {}
