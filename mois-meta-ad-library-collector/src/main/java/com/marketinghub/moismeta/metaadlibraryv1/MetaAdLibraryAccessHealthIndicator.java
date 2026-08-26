package com.marketinghub.moismeta.metaadlibraryv1;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Expõe a disponibilidade do coletor separada da autorização externa da Meta. */
@Component("metaAdLibraryAccess")
public class MetaAdLibraryAccessHealthIndicator implements HealthIndicator {

  private volatile MetaAdLibraryContracts.AccessPreflight lastPreflight =
      new MetaAdLibraryContracts.AccessPreflight(
          false, "NOT_CHECKED", null, null, "Preflight ainda não executado", null);

  /** Registra somente o diagnóstico sanitizado mais recente. */
  public void record(MetaAdLibraryContracts.AccessPreflight preflight) {
    lastPreflight = preflight;
  }

  /** Mantém o processo saudável e detalha se a fonte externa está autorizada. */
  @Override
  public Health health() {
    MetaAdLibraryContracts.AccessPreflight current = lastPreflight;
    return Health.up()
        .withDetail("apiAccess", current.status())
        .withDetail("authorized", current.authorized())
        .withDetail("errorCode", valueOrEmpty(current.errorCode()))
        .withDetail("errorSubcode", valueOrEmpty(current.errorSubcode()))
        .withDetail("message", current.message())
        .withDetail("checkedAt", valueOrEmpty(current.checkedAt()))
        .build();
  }

  /** Evita valores nulos incompatíveis com os detalhes do Actuator. */
  private Object valueOrEmpty(Object value) {
    return value == null ? "" : value;
  }
}
