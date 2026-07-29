package com.marketinghub.experiment.funnel;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Filtra tráfego interno que não deve contaminar métricas comerciais de analytics. */
@Component
public class InternalAnalyticsTrafficFilter {

  private final Set<String> excludedIps;

  /** Carrega a lista versionada/configurável de IPs internos excluídos das métricas. */
  public InternalAnalyticsTrafficFilter(
      @Value("${experiment.analytics.internal-excluded-ips:}") String excludedIps) {
    this.excludedIps =
        Arrays.stream(excludedIps.split(","))
            .map(String::trim)
            .filter(ip -> !ip.isBlank())
            .collect(Collectors.toUnmodifiableSet());
  }

  /** Indica se o IP recebido pertence à lista interna que deve ser ignorada. */
  public boolean isInternal(String clientIp) {
    return clientIp != null && excludedIps.contains(clientIp.trim());
  }
}
