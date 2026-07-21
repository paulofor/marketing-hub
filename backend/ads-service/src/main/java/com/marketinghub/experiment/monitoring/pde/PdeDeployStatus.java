package com.marketinghub.experiment.monitoring.pde;

import java.time.Instant;
import java.util.List;

/** Contrato do status de deploy retornado pelo backend PDE publicado. */
public record PdeDeployStatus(
        String environment,
        boolean available,
        String status,
        String errorMessage,
        String composeFile,
        String commitSha,
        String imageTag,
        String experienceVersion,
        String frontendUrl,
        String backendUrl,
        boolean frontendReachable,
        boolean backendReachable,
        Instant deployedAt,
        List<PdeDeployServiceStatus> services
) {
    /** Representa um serviço declarado na stack PDE publicada. */
    public record PdeDeployServiceStatus(
            String name,
            String containerName,
            String image,
            Integer publicPort,
            Integer targetPort,
            String role
    ) {}
}
