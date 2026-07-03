package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai;

/** Define a sequência canônica de tentativas OpenAI: duas em Flex e a terceira em Standard/default. */
public final class OpenAiServiceTierRetryPolicy {

    /** Quantidade máxima de tentativas canônicas para chamadas OpenAI síncronas. */
    public static final int MAX_ATTEMPTS = 3;

    private OpenAiServiceTierRetryPolicy() {
    }

    /** Retorna o service_tier efetivo da tentativa informada, usando Standard/default na terceira tentativa. */
    public static String serviceTierForAttempt(int attempt) {
        if (attempt <= 0 || attempt > MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Tentativa OpenAI fora da regra canônica: " + attempt);
        }
        return attempt < MAX_ATTEMPTS ? "flex" : "default";
    }

    /** Informa se a tentativa deve omitir service_tier explícito para representar Standard/default. */
    public static boolean shouldOmitServiceTier(int attempt) {
        return "default".equals(serviceTierForAttempt(attempt));
    }
}
