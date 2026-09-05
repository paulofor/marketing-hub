package com.marketinghub.product.privatereading.service.workspace;

import java.util.Map;

/**
 * Responsabilidade: apresentar acesso e sinais reais sem expor convites, sessões ou dados pessoais.
 */
public record PrivateReadingWorkspace(
    String prototypeUrl,
    String prototypeVersion,
    String activityId,
    int readingNumber,
    String participantReference,
    String evidenceId,
    Map<String, Boolean> signals,
    boolean canRecord,
    String status,
    String guidance,
    String finishedAt) {}
