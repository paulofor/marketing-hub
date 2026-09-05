package com.marketinghub.product.privatereading.service.evidence;

import java.util.Map;

/** Responsabilidade: transportar prova sanitizada emitida pelo backend do protótipo privado. */
public record PrivateReadingEvidence(
    String productSlug,
    String prototypeVersion,
    String participantReference,
    String trafficClass,
    String evidenceId,
    String consentedAt,
    String finishedAt,
    String blocker,
    Map<String, Boolean> signals,
    String checkoutMode,
    Boolean paymentEnabled,
    Boolean published,
    Integer mediaSpendBrl) {}
