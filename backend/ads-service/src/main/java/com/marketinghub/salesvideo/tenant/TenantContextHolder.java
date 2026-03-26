package com.marketinghub.salesvideo.tenant;

import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Thread-local para armazenar o tenant corrente.
 */
public final class TenantContextHolder {
    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        if (context == null) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(context);
        }
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static TenantContext getContext() {
        TenantContext context = CONTEXT.get();
        return context != null ? context : TenantContext.system();
    }

    public static String requireTenant() {
        TenantContext context = getContext();
        if (context.systemRequest() || !StringUtils.hasText(context.tenantId())) {
            throw VideoModuleException.badRequest(VideoModuleErrorCode.TENANT_HEADER_REQUIRED,
                    "Cabeçalho X-Tenant-ID é obrigatório para operações do módulo de vídeo");
        }
        return context.tenantId();
    }

    public static String currentTenant() {
        return getContext().tenantId();
    }

    public static boolean isSystemRequest() {
        return getContext().systemRequest();
    }

    public static void assertTenant(String tenantId) {
        TenantContext context = getContext();
        if (tenantId == null) {
            return;
        }
        if (!context.systemRequest() && !Objects.equals(context.tenantId(), tenantId)) {
            throw VideoModuleException.forbidden(VideoModuleErrorCode.TENANT_FORBIDDEN,
                    "Recurso não pertence ao tenant ativo");
        }
    }

    public static String resolveUserEmail(String fallback) {
        TenantContext context = getContext();
        if (StringUtils.hasText(context.userEmail())) {
            return context.userEmail();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return "unknown@marketinghub.io";
    }
}
