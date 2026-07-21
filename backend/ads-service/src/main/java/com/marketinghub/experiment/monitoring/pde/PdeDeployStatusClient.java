package com.marketinghub.experiment.monitoring.pde;

import java.util.List;

/** Cliente responsável por consultar status de deploy dos ambientes PDE. */
public interface PdeDeployStatusClient {

    /** Lista os ambientes PDE configurados para monitoramento. */
    List<PdeDeployStatus> fetchStatuses();
}
