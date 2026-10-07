package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.prepareAdjustment;

/** Explica se o ajuste pré-mercado permite cadastrar um sucessor sem executar ou gastar. */
public record AdjustmentPreparationAvailability(boolean available, String reason) {}
