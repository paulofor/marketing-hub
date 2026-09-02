package com.marketinghub.product.service.privatevalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar o gate humano que aceita somente um protótipo privado utilizável. */
class PdePrivatePrototypeAcceptanceHumanActivityHandlerTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ProductRepository products = mock(ProductRepository.class);
  private final PdePrivatePrototypeAcceptanceHumanActivityHandler handler =
      new PdePrivatePrototypeAcceptanceHumanActivityHandler(products, json);

  /** Libera o formulário especializado somente para o produto privado planejado. */
  @Test
  void exposesPrototypeAcceptanceWorkspace() {
    var readiness =
        handler.readiness(process(), activity(), product(), "product:9@private-validation-v1");

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.workspaceCode()).isEqualTo("PDE_PRIVATE_PROTOTYPE_ACCEPTANCE");
    assertThat(readiness.workspaceReferenceId()).isEqualTo(9L);
  }

  /** Persiste versão, URL, fontes e travas sem habilitar pagamento, publicação ou mídia. */
  @Test
  void acceptsUsablePrivatePrototypeWithoutCommercialEffects() throws Exception {
    Product product = product();

    handler.approve(
        process(),
        activity(),
        product,
        "product:9@private-validation-v1",
        decision(allConfirmations()));

    var validation = json.readTree(product.getValidationDefinitionJson());
    var acceptance = validation.path("privatePrototypeAcceptance");
    assertThat(acceptance.path("status").asText()).isEqualTo("READY");
    assertThat(acceptance.path("prototypeVersion").asText()).isEqualTo("private-v1");
    assertThat(acceptance.path("privateAccessUrl").asText())
        .isEqualTo("https://private.local/prototype");
    assertThat(acceptance.path("paymentEnabled").asBoolean()).isFalse();
    assertThat(acceptance.path("published").asBoolean()).isFalse();
    assertThat(acceptance.path("mediaSpendBrl").asDouble()).isZero();
    assertThat(product.getCommercialStatus()).isEqualTo("PLANNED");
    verify(products).save(product);
  }

  /** Bloqueia a aceitação quando qualquer trava privada não foi confirmada. */
  @Test
  void rejectsPrototypeWithPaymentEnabled() {
    Map<String, Object> evidence = new LinkedHashMap<>(allConfirmations());
    evidence.put("paymentDisabled", false);

    assertThatThrownBy(
            () ->
                handler.approve(
                    process(),
                    activity(),
                    product(),
                    "product:9@private-validation-v1",
                    decision(evidence)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("confirmações");
  }

  /** Monta todas as evidências exigidas para a aceitação privada. */
  private Map<String, Object> allConfirmations() {
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("prototypeVersion", "private-v1");
    evidence.put("privateAccessUrl", "https://private.local/prototype");
    evidence.put("instrumentationReference", "events:local-01");
    evidence.put("sourceEvidenceReference", "source-snapshot:local-01");
    evidence.put("sourceEvaluatedAt", java.time.Instant.now().minusSeconds(3600).toString());
    evidence.put("privateAccessConfirmed", true);
    evidence.put("paymentDisabled", true);
    evidence.put("publicationDisabled", true);
    evidence.put("noMediaSpendConfirmed", true);
    evidence.put("firstPartyEventsConfirmed", true);
    evidence.put("desktopValidated", true);
    evidence.put("mobileValidated", true);
    return evidence;
  }

  /** Monta a decisão humana auditável do gate de protótipo. */
  private ProductProcessActivityExecutionRequest decision(Map<String, Object> evidence) {
    return new ProductProcessActivityExecutionRequest(
        "APPROVE",
        "Operador local",
        "Protótipo utilizável homologado em desktop e celular.",
        "homologation:local-01",
        "CONFIRM:pde-construction-approval:prototypeAcceptance",
        evidence);
  }

  /** Monta um produto ainda privado com plano e arquitetura congelados. */
  private Product product() {
    return Product.builder()
        .id(9L)
        .commercialStatus("PLANNED")
        .validationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1")
        .validationDefinitionJson(
            """
            {"privateValidationPlan":{
               "minimumIndependentReadings":2,
               "minimumEligibleParticipantsPerReading":1,
               "sourceMaxAgeDays":30,
               "requiredSignals":[
                 "EXPERIENCE_STARTED",
                 "VALUE_MOMENT",
                 "READY_RESULT_USED",
                 "PREFERRED_OVER_FREE",
                 "CHECKOUT_STARTED"
               ]},
             "privatePrototype":{"checkoutMode":"SIMULATED_NO_CHARGE"},
             "purchaseMomentStatus":"WAITING_PRIVATE_PROTOTYPE"}
            """)
        .pdeExperienceJson(
            """
            {"experienceVersion":"private-validation-v1","status":"PLANNED"}
            """)
        .build();
  }

  /** Monta o processo canônico de construção privada. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("pde-construction-approval");
    return process;
  }

  /** Monta a atividade humana que aceita o protótipo. */
  private BusinessProcessActivityDefinition activity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("prototypeAcceptance");
    activity.setOwnerName("Operador humano");
    return activity;
  }
}
