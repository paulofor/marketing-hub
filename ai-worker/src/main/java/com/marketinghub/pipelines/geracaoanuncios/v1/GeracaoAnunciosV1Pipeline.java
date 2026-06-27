package com.marketinghub.pipelines.geracaoanuncios.v1;

import java.util.Set;

/** Representa o núcleo declarativo do pipeline GeracaoAnuncios v1 no AI Worker. */
public final class GeracaoAnunciosV1Pipeline {
    public static final String TEXTO = "texto";
    public static final String IMAGEM = "imagem";
    private static final Set<String> ETAPAS = Set.of(TEXTO, IMAGEM);

    /** Impede instanciação de classe utilitária do núcleo do pipeline. */
    private GeracaoAnunciosV1Pipeline() {}

    /** Informa se o código recebido representa uma etapa conhecida do pipeline. */
    public static boolean contemEtapa(String etapa) {
        return ETAPAS.contains(etapa);
    }
}
