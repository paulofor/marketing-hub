package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Resume a fabricacao com evidencias, decisoes e artefatos entregues.
 */
public record FabricationReport(
        String requestId,
        String status,
        String commercialDecision,
        List<String> evidence,
        List<String> generatedFiles,
        List<String> nextActions) {
}
