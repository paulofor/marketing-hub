package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution;

import java.util.List;

/** Contrato de entrada para gravar o seed e as queries gerados pela IA na etapa dois. */
public record CompleteNicheResearchSeedBuilderRequest(
    String nicheName,
    String businessType,
    String operationType,
    String customerType,
    String commercialObjects,
    String initialAssumptions,
    String confidenceLevel,
    String createdBy,
    String model,
    String rawModelResponse,
    String rawOpenAiRequest,
    String rawOpenAiResponse,
    Integer inputTokens,
    Integer outputTokens,
    String openAiResponseId,
    List<NicheResearchQueryRequest> queries) {
  /** Mantém compatibilidade com chamadas antigas que ainda não enviam payloads crus da OpenAI. */
  public CompleteNicheResearchSeedBuilderRequest(
      String nicheName,
      String businessType,
      String operationType,
      String customerType,
      String commercialObjects,
      String initialAssumptions,
      String confidenceLevel,
      String createdBy,
      String model,
      String rawModelResponse,
      Integer inputTokens,
      Integer outputTokens,
      String openAiResponseId,
      List<NicheResearchQueryRequest> queries) {
    this(
        nicheName,
        businessType,
        operationType,
        customerType,
        commercialObjects,
        initialAssumptions,
        confidenceLevel,
        createdBy,
        model,
        rawModelResponse,
        null,
        null,
        inputTokens,
        outputTokens,
        openAiResponseId,
        queries);
  }
}
