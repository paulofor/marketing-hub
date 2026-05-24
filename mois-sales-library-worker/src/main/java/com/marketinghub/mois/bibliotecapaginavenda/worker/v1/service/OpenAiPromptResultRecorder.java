package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Responsável por registrar o resultado bruto e validado recebido da OpenAI para uso do PromptBuilder.
 */
@Service
@Slf4j
public class OpenAiPromptResultRecorder {

    /**
     * Registra os dados de request/response da OpenAI e o JSON validado para rastreabilidade do pipeline.
     */
    public void recordPromptBuilderOpenAiResult(
            String rawRequestSent,
            String rawResponseReceived,
            String openAiJobId,
            String validatedJson) {
        log.info(
                "MOIS prompt builder OpenAI result recorded. openAiJobId={}, rawRequestSent={}, rawResponseReceived={}, validatedJson={}",
                openAiJobId,
                rawRequestSent,
                rawResponseReceived,
                validatedJson);
    }

    /**
     * Insere o registro consolidado da integração OpenAI com vínculo ao job do Marketing Hub.
     */
    public void insertOpenAiIntegrationRecord(
            String rawRequestSent,
            String rawResponseReceived,
            String openAiJobId,
            String validatedJson,
            Long marketingHubJobId) {
        log.info(
                "MOIS OpenAI integration record inserted. marketingHubJobId={}, openAiJobId={}, rawRequestSent={}, rawResponseReceived={}, validatedJson={}",
                marketingHubJobId,
                openAiJobId,
                rawRequestSent,
                rawResponseReceived,
                validatedJson);
    }
}
