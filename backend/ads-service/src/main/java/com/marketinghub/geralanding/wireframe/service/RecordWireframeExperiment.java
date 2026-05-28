package com.marketinghub.geralanding.wireframe.service;

/** Representa os dados mínimos do experimento expostos na fila interna da etapa wireframe. */
public record RecordWireframeExperiment(
        Long id,
        String name,
        String hypothesis,
        String status,
        String stage
) {
}
