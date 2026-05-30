package com.marketinghub.worker.openai.core.exception;

/** Responsabilidade: transportar falhas HTTP retornadas pela OpenAI com o payload bruto de erro. */
public class OpenAiHttpException extends StageWorkerException {

    private final int statusCode;
    private final String responseBody;

    /** Inicializa a exceção com status HTTP, payload bruto de erro e causa original da integração. */
    public OpenAiHttpException(int statusCode, String responseBody, Throwable cause) {
        super(buildMessage(statusCode, responseBody), cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /** Retorna o status HTTP recebido da OpenAI. */
    public int statusCode() {
        return statusCode;
    }

    /** Retorna o corpo bruto de erro recebido da OpenAI. */
    public String responseBody() {
        return responseBody;
    }

    /** Monta uma mensagem operacional incluindo o payload bruto quando disponível. */
    private static String buildMessage(int statusCode, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "OpenAI Responses API returned HTTP " + statusCode;
        }
        return "OpenAI Responses API returned HTTP " + statusCode + ": " + responseBody;
    }
}
