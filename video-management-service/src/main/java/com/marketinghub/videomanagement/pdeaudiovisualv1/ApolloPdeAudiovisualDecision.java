package com.marketinghub.videomanagement.pdeaudiovisualv1;

/** Responsabilidade: expressar a decisão determinística de Apolo sobre a necessidade audiovisual. */
public record ApolloPdeAudiovisualDecision(
        Outcome outcome,
        String rationale,
        String recommendedAction,
        String blockerCategory) {

    /** Responsabilidade: enumerar os únicos desfechos aceitos pelo contrato audiovisual v1. */
    public enum Outcome {
        NOT_REQUIRED,
        REQUIRES_AUTHORIZATION,
        MISSING_CONTRACT,
        TECHNICAL_FAILURE
    }

    /** Informa se o contrato permite concluir a atividade sem materialização audiovisual. */
    public boolean canComplete() {
        return Outcome.NOT_REQUIRED.equals(outcome);
    }
}
