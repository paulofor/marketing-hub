package com.marketinghub.agentlearning.v1;

import java.math.BigDecimal;

/** Responsabilidade: resumir qualidade, retrabalho, recorrência e custo por versão de playbook. */
public record TemisVisualLearningMetricResponse(
    String contextKey,
    String playbookVersion,
    long cases,
    BigDecimal firstPassApprovalRate,
    BigDecimal approvalWithinThreeRate,
    BigDecimal recurringIssueRate,
    BigDecimal averageCostPerApprovedAsset,
    BigDecimal minimumPremiumScore) {}
