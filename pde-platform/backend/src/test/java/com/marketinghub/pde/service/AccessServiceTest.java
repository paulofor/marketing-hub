package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.WorkspaceResponse;
import org.junit.jupiter.api.Test;

/** Valida a liberação de acesso e o progresso da experiência PDE. */
class AccessServiceTest {

    /** Confirma que um acesso liberado retorna a experiência e progride ao concluir missão. */
    @Test
    void createsAccessAndTracksMissionProgress() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(productCatalogService);

        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "DEV");
        WorkspaceResponse initialWorkspace = accessService.getWorkspace(access.token());

        assertThat(initialWorkspace.product().name()).contains("MUSA");
        assertThat(initialWorkspace.progressPercent()).isZero();

        accessService.completeMission(access.token(), "dia-1-ruido-visual");
        WorkspaceResponse updatedWorkspace = accessService.getWorkspace(access.token());

        assertThat(updatedWorkspace.completedMissionIds()).containsExactly("dia-1-ruido-visual");
        assertThat(updatedWorkspace.progressPercent()).isEqualTo(14);
    }
}
