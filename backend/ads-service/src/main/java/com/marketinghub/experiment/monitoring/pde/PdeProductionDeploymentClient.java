package com.marketinghub.experiment.monitoring.pde;

import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionDeployResponseDto;

/** Cliente responsável por solicitar deploy produtivo do PDE no pipeline oficial. */
public interface PdeProductionDeploymentClient {

    /** Informa se existe configuração suficiente para acionar o deploy pelo Marketing Hub. */
    boolean isConfigured();

    /** Dispara a publicação produtiva usando o workflow canônico do PDE. */
    PostDeployPdeProductionDeployResponseDto dispatchProductionDeploy(
            Long experimentId,
            String requestedBy,
            String sourceCommitSha);
}
