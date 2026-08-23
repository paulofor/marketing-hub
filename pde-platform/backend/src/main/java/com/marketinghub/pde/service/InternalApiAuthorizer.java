package com.marketinghub.pde.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Autoriza operações administrativas internas do PDE com segredo configurado fora do código. */
@Service
public class InternalApiAuthorizer {
    private final String configuredToken;

    /** Recebe o segredo obrigatório das integrações administrativas do PDE. */
    public InternalApiAuthorizer(@Value("${pde.internal-api.token:}") String configuredToken) {
        this.configuredToken = configuredToken == null ? "" : configuredToken;
    }

    /** Interrompe a operação quando o segredo interno estiver ausente ou não coincidir. */
    public void requireAuthorized(String informedToken) {
        if (configuredToken.isBlank() || informedToken == null || !MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                informedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Operação interna PDE não autorizada");
        }
    }
}
