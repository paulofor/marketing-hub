package com.marketinghub.pde.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.marketinghub.pde.service.PdeOperationalHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: garantir que o tratamento global preserve falhas HTTP deliberadas. */
class ApiExceptionHandlerTest {

    /** Mantém o bloqueio operacional como 403 em vez de transformá-lo em falha técnica 500. */
    @Test
    void preservesControlledForbiddenStatus() {
        ApiExceptionHandler handler = new ApiExceptionHandler(mock(PdeOperationalHealthService.class));

        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Operação PDE não autorizada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "Operação PDE não autorizada");
    }

    /** Mantém endpoint removido como 404 em vez de reportar falso erro técnico 500. */
    @Test
    void preservesRemovedPublicRouteAsNotFound() {
        ApiExceptionHandler handler = new ApiExceptionHandler(mock(PdeOperationalHealthService.class));

        var response = handler.handleNoResource(
                new NoResourceFoundException(HttpMethod.POST, "api/pde/access/login"));

        assertThat(response).containsEntry("error", "Rota não encontrada");
    }
}
