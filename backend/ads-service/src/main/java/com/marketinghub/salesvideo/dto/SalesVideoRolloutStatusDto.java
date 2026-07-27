package com.marketinghub.salesvideo.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

/** Estado de rollout do módulo Avatar Sales Video para o tenant/perfil atual. */
@Data
@Builder
public class SalesVideoRolloutStatusDto {
  private boolean rolloutEnabled;
  private boolean tenantEligible;
  private boolean profileEligible;
  private String tenantId;
  private Long profileId;
  private Set<String> allowedTenants;
  private Set<Long> allowedProfileIds;
}
