package com.marketinghub.salesvideo.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtra requisições para garantir a presença do tenant nos endpoints administrativos de vídeo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String USER_HEADER = "X-User-Email";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Indica quando uma requisição não precisa passar pela validação de tenant de vídeo. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if (path.startsWith("/internal/")) {
            return false;
        }
        return !requiresTenant(path);
    }

    /** Define o contexto de tenant para rotas administrativas e internas do módulo de vídeo. */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            if (path != null && path.startsWith("/internal/")) {
                TenantContextHolder.set(TenantContext.system());
            } else if (requiresTenant(path)) {
                String tenantId = request.getHeader(TENANT_HEADER);
                if (!StringUtils.hasText(tenantId)) {
                    if (isReadOnlyProfileCompatibilityRequest(request, path)) {
                        TenantContextHolder.set(TenantContext.system());
                        filterChain.doFilter(request, response);
                        return;
                    }
                    writeErrorResponse(response, request.getRequestURI());
                    return;
                }
                String userEmail = request.getHeader(USER_HEADER);
                if (!StringUtils.hasText(userEmail)) {
                    userEmail = "unknown@marketinghub.io";
                }
                TenantContextHolder.set(new TenantContext(tenantId, userEmail, false));
            } else {
                TenantContextHolder.set(TenantContext.system());
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /** Identifica rotas administrativas que precisam de tenant explícito. */
    private boolean requiresTenant(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/api/sales-videos")
                || MATCHER.match("/api/products/*/sales-videos/**", path)
                || MATCHER.match("/api/experiments/*/video-assets/**", path)
                || MATCHER.match("/api/landing-pages/*/video-slots/**", path);
    }

    /** Permite leitura legado do perfil pelo renderizador externo enquanto ele migra para `/internal/video`. */
    private boolean isReadOnlyProfileCompatibilityRequest(HttpServletRequest request, String path) {
        return HttpMethod.GET.matches(request.getMethod())
                && MATCHER.match("/api/sales-videos/profiles/*", path);
    }

    /** Escreve resposta padronizada quando o cabeçalho de tenant obrigatório está ausente. */
    private void writeErrorResponse(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "Cabeçalho X-Tenant-ID é obrigatório para operações do módulo de vídeo");
        body.put("errorCode", VideoModuleErrorCode.TENANT_HEADER_REQUIRED.name());
        body.put("path", path);
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }
}
