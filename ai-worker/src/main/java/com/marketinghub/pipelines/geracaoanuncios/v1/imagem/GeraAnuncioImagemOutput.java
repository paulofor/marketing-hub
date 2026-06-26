package com.marketinghub.pipelines.geracaoanuncios.v1.imagem;

import java.util.List;
import java.util.Map;

/** Responsabilidade: transportar a saída estruturada produzida pela etapa Imagem. */
public record GeraAnuncioImagemOutput(List<Map<String, Object>> artifacts, Map<String, Object> audit) {}
