package com.marketinghub.geraanuncio.v2;

import java.util.Set;

/** Representa o núcleo declarativo do pipeline GeraAnuncio v2 no AI Worker. */
public final class GeraAnuncioV2Pipeline {
    public static final String TEXTO = "texto";
    public static final String IMAGEM = "imagem";
    private static final Set<String> ETAPAS = Set.of(TEXTO, IMAGEM);

    /** Impede instanciação de classe utilitária do núcleo do pipeline. */
    private GeraAnuncioV2Pipeline() {}

    /** Informa se o código recebido representa uma etapa conhecida do pipeline. */
    public static boolean contemEtapa(String etapa) {
        return ETAPAS.contains(etapa);
    }
}
