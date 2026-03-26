package com.marketinghub.salesvideo.tenant;

import java.util.Objects;

/**
 * Representa o escopo de tenant associado à requisição atual.
 */
public record TenantContext(String tenantId, String userEmail, boolean systemRequest) {

    public TenantContext {
        tenantId = tenantId != null ? tenantId.trim() : null;
        userEmail = userEmail != null ? userEmail.trim() : null;
    }

    public static TenantContext system() {
        return new TenantContext("__system__", "system@marketinghub.io", true);
    }

    public boolean matches(String resourceTenant) {
        if (systemRequest) {
            return true;
        }
        return Objects.equals(tenantId, resourceTenant);
    }
}
