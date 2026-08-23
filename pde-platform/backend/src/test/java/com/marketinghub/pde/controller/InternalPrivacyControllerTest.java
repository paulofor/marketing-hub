package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import org.junit.jupiter.api.Test;

/** Valida a fronteira autenticada usada pelo executor externo de retenção PDE. */
class InternalPrivacyControllerTest {

    /** Executa a política e retorna contagem auditável somente com o segredo correto. */
    @Test
    void executesRetentionForAuthorizedWorker() {
        AccessService accessService = mock(AccessService.class);
        when(accessService.enforceDataRetention(any())).thenReturn(2);
        InternalPrivacyController controller = new InternalPrivacyController(
                accessService, new InternalApiAuthorizer("segredo-interno"));

        var response = controller.enforceRetention("segredo-interno", "retention-job-1");

        assertThat(response.anonymizedAccesses()).isEqualTo(2);
        assertThat(response.executedAt()).isNotBlank();
        verify(accessService).enforceDataRetention(any());
    }

    /** Bloqueia o executor antes de acessar dados quando o segredo diverge. */
    @Test
    void rejectsUnauthorizedRetentionWorker() {
        AccessService accessService = mock(AccessService.class);
        InternalPrivacyController controller = new InternalPrivacyController(
                accessService, new InternalApiAuthorizer("segredo-interno"));

        assertThatThrownBy(() -> controller.enforceRetention("inválido", "retention-job-2"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(accessService);
    }
}
