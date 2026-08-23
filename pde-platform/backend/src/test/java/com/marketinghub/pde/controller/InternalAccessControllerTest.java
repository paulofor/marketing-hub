package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.dto.AccessRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import org.junit.jupiter.api.Test;

/** Valida a criação segregada de acesso interno usado apenas na homologação PDE. */
class InternalAccessControllerTest {

    /** Cria acesso de QA somente quando segredo e trava do ambiente permitem a operação. */
    @Test
    void createsInternalQaAccessWhenExplicitlyEnabled() {
        AccessService accessService = mock(AccessService.class);
        InternalApiAuthorizer authorizer = new InternalApiAuthorizer("segredo-interno");
        InternalAccessController controller = new InternalAccessController(accessService, authorizer, true);
        AccessRequest request = new AccessRequest("metodo-musa-7-dias", "teste@sandbox.local", "musa-v7");
        AccessResponse expected = new AccessResponse(
                "token-qa", "metodo-musa-7-dias", "teste@sandbox.local", "INTERNAL_QA", "/access/token-qa");
        when(accessService.createInternalQaAccess(
                        "metodo-musa-7-dias", "teste@sandbox.local", "musa-v7"))
                .thenReturn(expected);

        AccessResponse actual = controller.createInternalQaAccess("segredo-interno", request);

        assertThat(actual).isEqualTo(expected);
        verify(accessService)
                .createInternalQaAccess("metodo-musa-7-dias", "teste@sandbox.local", "musa-v7");
    }

    /** Bloqueia a simulação de acesso pago quando a trava de QA está desligada. */
    @Test
    void rejectsInternalQaAccessWhenEnvironmentFlagIsDisabled() {
        AccessService accessService = mock(AccessService.class);
        InternalAccessController controller =
                new InternalAccessController(accessService, new InternalApiAuthorizer("segredo-interno"), false);

        assertThatThrownBy(() -> controller.createInternalQaAccess(
                        "segredo-interno",
                        new AccessRequest("metodo-musa-7-dias", "teste@sandbox.local", "musa-v7")))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(accessService);
    }

    /** Bloqueia a operação antes do serviço quando a credencial interna diverge. */
    @Test
    void rejectsInternalQaAccessWithInvalidToken() {
        AccessService accessService = mock(AccessService.class);
        InternalAccessController controller =
                new InternalAccessController(accessService, new InternalApiAuthorizer("segredo-interno"), true);

        assertThatThrownBy(() -> controller.createInternalQaAccess(
                        "token-invalido",
                        new AccessRequest("metodo-musa-7-dias", "teste@sandbox.local", "musa-v7")))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(accessService);
    }

    /** Expira acesso somente pela mesma credencial e trava usadas na homologação interna. */
    @Test
    void expiresInternalQaAccessWhenExplicitlyEnabled() {
        AccessService accessService = mock(AccessService.class);
        InternalAccessController controller =
                new InternalAccessController(accessService, new InternalApiAuthorizer("segredo-interno"), true);

        controller.expireInternalQaAccess("segredo-interno", "token-qa");

        verify(accessService).expireInternalQaAccess("token-qa");
    }
}
