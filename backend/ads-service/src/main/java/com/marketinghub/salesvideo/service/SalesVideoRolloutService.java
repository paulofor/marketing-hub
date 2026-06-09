package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.dto.SalesVideoRolloutStatusDto;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Política de rollout controlado do módulo de vídeo por tenant/perfil.
 */
@Component
public class SalesVideoRolloutService {

    private final boolean rolloutEnabled;
    private final Set<String> allowedTenants;
    private final Set<Long> allowedProfileIds;

    public SalesVideoRolloutService(
            @Value("${sales-video.rollout.enabled:false}") boolean rolloutEnabled,
            @Value("${sales-video.rollout.allowed-tenants:}") String allowedTenants,
            @Value("${sales-video.rollout.allowed-profile-ids:}") String allowedProfileIds) {
        this.rolloutEnabled = rolloutEnabled;
        this.allowedTenants = parseStringSet(allowedTenants);
        this.allowedProfileIds = parseLongSet(allowedProfileIds);
    }

    public void assertProductionRolloutAllowed(SalesVideoProfile profile) {
        SalesVideoRolloutStatusDto status = evaluate(profile.getId(), profile.getTenantId());
        if (!status.isRolloutEnabled() || !status.isTenantEligible() || !status.isProfileEligible()) {
            throw VideoModuleException.conflict(VideoModuleErrorCode.ROLLOUT_NOT_ALLOWED,
                    "Render produtivo bloqueado pelo rollout controlado. Habilite o tenant/perfil para continuar.");
        }
    }

    public SalesVideoRolloutStatusDto evaluate(Long profileId) {
        return evaluate(profileId, TenantContextHolder.currentTenant());
    }

    public SalesVideoRolloutStatusDto evaluate(Long profileId, String tenantId) {
        boolean tenantEligible = allowedTenants.isEmpty() || allowedTenants.contains(tenantId);
        boolean profileEligible = allowedProfileIds.isEmpty() || (profileId != null && allowedProfileIds.contains(profileId));
        return SalesVideoRolloutStatusDto.builder()
                .rolloutEnabled(rolloutEnabled)
                .tenantEligible(tenantEligible)
                .profileEligible(profileEligible)
                .tenantId(tenantId)
                .profileId(profileId)
                .allowedTenants(allowedTenants)
                .allowedProfileIds(allowedProfileIds)
                .build();
    }

    public SalesVideoExecutionMode normalizeExecutionMode(SalesVideoExecutionMode requestedMode) {
        return requestedMode != null ? requestedMode : SalesVideoExecutionMode.PRODUCTION;
    }

    private static Set<String> parseStringSet(String csv) {
        if (!StringUtils.hasText(csv)) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<Long> parseLongSet(String csv) {
        if (!StringUtils.hasText(csv)) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
