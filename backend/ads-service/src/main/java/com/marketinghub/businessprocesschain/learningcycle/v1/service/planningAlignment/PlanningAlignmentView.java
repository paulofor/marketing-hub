package com.marketinghub.businessprocesschain.learningcycle.v1.service.planningAlignment;

import java.math.BigDecimal;

/** Responsabilidade: apresentar a elegibilidade e a oferta antes da correção auditável. */
public record PlanningAlignmentView(
    boolean available,
    String reason,
    long cycleRevision,
    int bigIdeaRevision,
    String currentOffer,
    String proposedOffer,
    BigDecimal proposedPriceBrl) {}
