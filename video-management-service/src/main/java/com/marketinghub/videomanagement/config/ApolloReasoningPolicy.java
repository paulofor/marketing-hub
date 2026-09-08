package com.marketinghub.videomanagement.config;

/** Responsabilidade: exigir a política única de raciocínio máximo nas chamadas de Apolo. */
public final class ApolloReasoningPolicy {
    public static final String MAXIMUM = "max";

    /** Impede instâncias de uma política sem estado. */
    private ApolloReasoningPolicy() {}

    /** Bloqueia configuração ausente ou inferior antes de iniciar a chamada ao modelo. */
    public static String requireMaximum(String configured) {
        if (configured == null || !MAXIMUM.equals(configured.trim())) {
            throw new IllegalStateException(
                    "APOLLO_CODEX_REASONING_EFFORT deve ser max em toda execução de Apolo.");
        }
        return MAXIMUM;
    }
}
