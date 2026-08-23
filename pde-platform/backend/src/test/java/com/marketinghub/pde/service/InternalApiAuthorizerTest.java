package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Valida a proteção das operações internas do PDE por segredo configurado. */
class InternalApiAuthorizerTest {

    /** Autoriza somente a credencial exatamente igual à configurada. */
    @Test
    void authorizesMatchingInternalToken() {
        InternalApiAuthorizer authorizer = new InternalApiAuthorizer("segredo-interno");

        assertThatCode(() -> authorizer.requireAuthorized("segredo-interno")).doesNotThrowAnyException();
    }

    /** Bloqueia credenciais ausentes, vazias ou divergentes. */
    @Test
    void rejectsMissingOrDivergentInternalToken() {
        InternalApiAuthorizer authorizer = new InternalApiAuthorizer("segredo-interno");

        assertThatThrownBy(() -> authorizer.requireAuthorized(null)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> authorizer.requireAuthorized("")).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> authorizer.requireAuthorized("outro-segredo"))
                .isInstanceOf(SecurityException.class);
    }

    /** Impede operações internas quando o ambiente não possui segredo configurado. */
    @Test
    void rejectsEveryTokenWhenConfigurationIsBlank() {
        InternalApiAuthorizer authorizer = new InternalApiAuthorizer("");

        assertThatThrownBy(() -> authorizer.requireAuthorized("qualquer-token"))
                .isInstanceOf(SecurityException.class);
    }
}
