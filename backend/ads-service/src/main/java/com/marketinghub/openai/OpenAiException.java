package com.marketinghub.openai;

/**
 * Representa falhas de integração com OpenAI preservando os dados essenciais para diagnóstico.
 */
public class OpenAiException extends RuntimeException {

    private final String rawRequest;
    private final String rawResponse;
    private final String validatedJson;
    private final String marketingHubJobId;
    private final String openAiJobId;

    /**
     * Inicializa a exception com os dados crus e identificadores de rastreabilidade da execução.
     */
    public OpenAiException(String rawRequest,
                           String rawResponse,
                           String validatedJson,
                           String marketingHubJobId,
                           String openAiJobId) {
        super("Falha de integração OpenAI");
        this.rawRequest = rawRequest;
        this.rawResponse = rawResponse;
        this.validatedJson = validatedJson;
        this.marketingHubJobId = marketingHubJobId;
        this.openAiJobId = openAiJobId;
    }

    /**
     * Retorna a representação textual completa para logging com todos os campos obrigatórios.
     */
    @Override
    public String toString() {
        return "OpenAiException{" +
                "rawRequest='" + rawRequest + '\'' +
                ", rawResponse='" + rawResponse + '\'' +
                ", validatedJson='" + validatedJson + '\'' +
                ", marketingHubJobId='" + marketingHubJobId + '\'' +
                ", openAiJobId='" + openAiJobId + '\'' +
                "}";
    }
}
