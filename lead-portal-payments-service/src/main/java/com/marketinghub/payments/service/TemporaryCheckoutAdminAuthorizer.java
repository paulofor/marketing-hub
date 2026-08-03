package com.marketinghub.payments.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Protege comandos administrativos de checkout contra chamadas públicas. */
@Service
public class TemporaryCheckoutAdminAuthorizer {
    private final String expectedToken;

    /** Carrega o token interno obrigatório usado pelo backend principal. */
    public TemporaryCheckoutAdminAuthorizer(
            @Value("${payments.admin-auth-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    /** Autoriza apenas chamadas Bearer com o segredo interno configurado. */
    public void authorize(String authorization) {
        if (!StringUtils.hasText(expectedToken)
                || !StringUtils.hasText(authorization)
                || !authorization.equals("Bearer " + expectedToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operação administrativa não autorizada");
        }
    }
}
