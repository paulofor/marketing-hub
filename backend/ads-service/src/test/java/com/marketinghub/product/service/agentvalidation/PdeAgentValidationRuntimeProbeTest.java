package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar deterministicamente a identidade e as travas do runtime PDE. */
class PdeAgentValidationRuntimeProbeTest {
  private final ObjectMapper json = new ObjectMapper();
  private final PdeAgentValidationRuntimeProbe probe = new PdeAgentValidationRuntimeProbe();

  /** Aceita somente diagnóstico e contrato coerentes com a mesma versão privada. */
  @Test
  void acceptsCanonicalRuntime() {
    var identity =
        probe.validatedIdentity(
            "https://alcyone.example", 11L, "pde-planejado-46", diagnostic(), contract());

    assertThat(identity.prototypeVersion()).isEqualTo("alcyone-private-v2");
    assertThat(identity.commitSha()).isEqualTo("a".repeat(40));
    assertThat(identity.frontendSourceSha256()).isEqualTo("b".repeat(64));
  }

  /** Recusa contrato com sinal adicional, mesmo quando os cinco sinais obrigatórios existem. */
  @Test
  void rejectsAdditionalInstrumentationSignal() {
    ObjectNode contract = contract();
    contract.withArray("instrumentationEvents").add("RESULT_PRESENTED");

    assertThatThrownBy(
            () ->
                probe.validatedIdentity(
                    "https://alcyone.example", 11L, "pde-planejado-46", diagnostic(), contract))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não está travado");
  }

  /** Recusa trava comercial ausente ou com tipo ambíguo no contrato público. */
  @Test
  void rejectsMissingOrUntypedCommercialLocks() {
    ObjectNode missingSpend = contract();
    missingSpend.remove("mediaSpendBrl");
    assertThatThrownBy(
            () ->
                probe.validatedIdentity(
                    "https://alcyone.example", 11L, "pde-planejado-46", diagnostic(), missingSpend))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não está travado");

    ObjectNode textualPayment = contract();
    textualPayment.put("paymentEnabled", "false");
    assertThatThrownBy(
            () ->
                probe.validatedIdentity(
                    "https://alcyone.example",
                    11L,
                    "pde-planejado-46",
                    diagnostic(),
                    textualPayment))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não está travado");
  }

  /** Recusa identidade divergente e destinos que possam carregar credenciais ou parâmetros. */
  @Test
  void rejectsDivergentIdentityAndUnsafeUrl() {
    ObjectNode diagnostic = diagnostic();
    diagnostic.put("imageVersionId", "alcyone-private-v1");

    assertThatThrownBy(
            () ->
                probe.validatedIdentity(
                    "https://alcyone.example", 11L, "pde-planejado-46", diagnostic, contract()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("divergente");
    assertThatThrownBy(() -> probe.validatedBaseUrl("https://token@alcyone.example?secret=1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Monta o diagnóstico autodeclarado pelo container publicado. */
  private ObjectNode diagnostic() {
    ObjectNode node = json.createObjectNode();
    node.put("status", "UP");
    node.put("productId", 11);
    node.put("productSlug", "pde-planejado-46");
    node.put("publicUrl", "https://alcyone.example");
    node.put("version", "alcyone-private-v2");
    node.put("experienceVersion", "alcyone-private-v2");
    node.put("imageVersionId", "alcyone-private-v2");
    node.put("commitSha", "a".repeat(40));
    node.put("frontendSourceSha256", "b".repeat(64));
    node.put("imageTag", "a".repeat(40));
    node.put("image", "ghcr.io/example/alcyone:" + "a".repeat(40));
    node.put("deployedAt", "2026-09-30T10:13:06Z");
    return node;
  }

  /** Monta o contrato público sem pagamento, publicação, provedor ou sinais extras. */
  private ObjectNode contract() {
    ObjectNode node = json.createObjectNode();
    node.put("productId", 11);
    node.put("productSlug", "pde-planejado-46");
    node.put("prototypeVersion", "alcyone-private-v2");
    node.put("checkoutMode", "SIMULATED_NO_CHARGE");
    node.put("intakeConsentVersion", "ALCYONE_AGENT_INTAKE_CONSENT_V1");
    node.put("published", false);
    node.put("paymentEnabled", false);
    node.put("mediaSpendBrl", 0);
    node.put("providerCallsAuthorized", 0);
    var signals = node.putArray("instrumentationEvents");
    for (String signal :
        List.of(
            "EXPERIENCE_STARTED",
            "VALUE_MOMENT",
            "READY_RESULT_USED",
            "PREFERRED_OVER_FREE",
            "CHECKOUT_STARTED")) signals.add(signal);
    var errors = node.putArray("errorStates");
    for (String code :
        List.of(
            "ACCESS_INVALID",
            "SESSION_EXPIRED",
            "INPUT_INCOMPLETE",
            "HARNESS_FAILURE",
            "RESULT_UNAVAILABLE",
            "RESUME_FAILED")) errors.addObject().put("code", code);
    return node;
  }
}
