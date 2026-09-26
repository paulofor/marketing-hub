package com.marketinghub.communication.v1;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Responsabilidade: definir e validar os marcadores de instrumentação que a landing de Íris deve
 * entregar ao runtime publicador.
 */
public final class IrisLandingInstrumentationContract {
  public static final String VERSION = "IRIS_LANDING_INSTRUMENTATION_V1";
  private static final List<String> REQUIRED_EVENTS =
      List.of("page_view", "page_load_metric", "section_view_time", "checkout_click");
  private static final List<String> PRIMARY_CHECKOUT_SELECTORS =
      List.of("#checkout-cta-primary", "[data-analytics-role=\"primary-checkout\"]");

  /** Impede instanciação; o contrato é imutável e compartilhado por projeção e gate. */
  private IrisLandingInstrumentationContract() {}

  /** Expõe somente os marcadores que o HTML deve materializar antes da injeção no runtime. */
  public static Map<String, Object> payload() {
    return Map.ofEntries(
        Map.entry("contractVersion", VERSION),
        Map.entry("collectorOwner", "BACKEND_PUBLICATION_RUNTIME"),
        Map.entry("runtimeInjectionRequired", true),
        Map.entry("inlineScriptAllowed", false),
        Map.entry("sectionAttribute", "data-track-section"),
        Map.entry("primaryCheckoutSelectors", PRIMARY_CHECKOUT_SELECTORS),
        Map.entry("events", REQUIRED_EVENTS),
        Map.entry("internalTrafficQueryParameter", "mh_test=1"),
        Map.entry("internalTrafficSessionMarker", "mh_internal_test"),
        Map.entry("persistenceSource", "landing-page-analytics"));
  }

  /** Confirma que a tarefa recebeu a versão e os controles mínimos do publicador canônico. */
  public static boolean valid(Object value) {
    if (!(value instanceof Map<?, ?> contract)) return false;
    return VERSION.equals(contract.get("contractVersion"))
        && Boolean.TRUE.equals(contract.get("runtimeInjectionRequired"))
        && Boolean.FALSE.equals(contract.get("inlineScriptAllowed"))
        && "data-track-section".equals(contract.get("sectionAttribute"))
        && containsAll(contract.get("primaryCheckoutSelectors"), PRIMARY_CHECKOUT_SELECTORS)
        && containsAll(contract.get("events"), REQUIRED_EVENTS)
        && "mh_test=1".equals(contract.get("internalTrafficQueryParameter"))
        && "mh_internal_test".equals(contract.get("internalTrafficSessionMarker"))
        && "landing-page-analytics".equals(contract.get("persistenceSource"));
  }

  /** Compara coleções sem depender da implementação concreta usada pelo transporte JSON. */
  private static boolean containsAll(Object value, Collection<String> required) {
    return value instanceof Collection<?> values && values.containsAll(required);
  }
}
