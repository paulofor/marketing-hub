package com.marketinghub.pde.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Protege a ingestão financeira do PDE com um segredo interno dedicado e comparação constante. */
@Service
public class MercadoPagoEntitlementAuthorizer {
    private final String expectedAuthorization;

    /** Carrega o segredo que também é fornecido ao serviço oficial de pagamentos. */
    public MercadoPagoEntitlementAuthorizer(
            @Value("${pde.access.mercado-pago.internal-token:}") String expectedToken) {
        this.expectedAuthorization = StringUtils.hasText(expectedToken) ? "Bearer " + expectedToken : "";
    }

    /** Exige um cabeçalho Bearer válido sem vazar se o segredo está parcialmente correto. */
    public void requireAuthorized(String authorization) {
        if (!StringUtils.hasText(expectedAuthorization)
                || !StringUtils.hasText(authorization)
                || !MessageDigest.isEqual(
                        expectedAuthorization.getBytes(StandardCharsets.UTF_8),
                        authorization.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Entitlement financeiro não autorizado");
        }
    }
}
