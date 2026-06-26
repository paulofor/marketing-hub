package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import java.util.List;
import java.util.Map;

/** Responsabilidade: transportar a saída estruturada produzida pela etapa Texto. */
public record GeraAnuncioTextoOutput(List<Map<String, Object>> artifacts, Map<String, Object> audit) {}
