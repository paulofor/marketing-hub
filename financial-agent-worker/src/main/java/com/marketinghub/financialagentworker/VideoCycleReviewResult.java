package com.marketinghub.financialagentworker;

import java.util.Map;

/** Responsabilidade: transportar decisão e auditoria da mesma interação de Plutus. */
public record VideoCycleReviewResult(Map<String, Object> decision, Map<String, Object> audit) {}
