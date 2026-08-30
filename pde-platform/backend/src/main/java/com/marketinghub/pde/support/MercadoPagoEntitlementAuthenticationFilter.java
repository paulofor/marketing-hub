package com.marketinghub.pde.support;

import com.marketinghub.pde.service.MercadoPagoEntitlementAuthorizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

/** Autentica a ingestão financeira do Rigel antes da leitura e validação do payload HTTP. */
@Component
public class MercadoPagoEntitlementAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log =
            LoggerFactory.getLogger(MercadoPagoEntitlementAuthenticationFilter.class);
    private static final String ENDPOINT = "/api/internal/pde/mercado-pago/entitlements";
    private static final String UNAUTHORIZED_RESPONSE =
            "{\"error\":\"Entitlement financeiro não autorizado\"}";

    private final MercadoPagoEntitlementAuthorizer authorizer;

    /** Recebe a comparação constante usada também como defesa adicional no controller. */
    public MercadoPagoEntitlementAuthenticationFilter(
            MercadoPagoEntitlementAuthorizer authorizer) {
        this.authorizer = authorizer;
    }

    /** Limita a autenticação antecipada ao POST financeiro interno do Rigel. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !ENDPOINT.equals(request.getRequestURI());
    }

    /** Rejeita a chamada antes de permitir desserialização, validação ou escrita financeira. */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        try {
            authorizer.requireAuthorized(request.getHeader(HttpHeaders.AUTHORIZATION));
        } catch (ResponseStatusException ex) {
            log.warn(
                    "Ingestão financeira do Rigel rejeitada antes do payload; method={}, endpoint={}, clientIp={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    ex);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(UNAUTHORIZED_RESPONSE);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
