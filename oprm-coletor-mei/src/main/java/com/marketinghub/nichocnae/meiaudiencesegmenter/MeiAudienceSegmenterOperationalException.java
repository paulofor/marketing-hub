package com.marketinghub.nichocnae.meiaudiencesegmenter;

/** Representa uma falha operacional de configuração que impede a segmentação MEI/autônomo. */
public class MeiAudienceSegmenterOperationalException extends RuntimeException {

    /** Cria a exceção com mensagem clara para operador e backend. */
    public MeiAudienceSegmenterOperationalException(String message) {
        super(message);
    }

    /** Cria a exceção preservando a causa-raiz técnica original. */
    public MeiAudienceSegmenterOperationalException(String message, Throwable cause) {
        super(message, cause);
    }
}
