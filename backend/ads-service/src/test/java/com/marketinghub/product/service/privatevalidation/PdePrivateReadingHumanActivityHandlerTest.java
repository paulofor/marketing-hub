package com.marketinghub.product.service.privatevalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar consentimento, pseudonimização e independência das leituras PDE. */
class PdePrivateReadingHumanActivityHandlerTest {
  private final ObjectMapper json = new ObjectMapper();
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final PdePrivateReadingHumanActivityHandler handler =
      new PdePrivateReadingHumanActivityHandler(instances, json);

  /** Libera a primeira leitura quando produto, referência e cinco sinais estão congelados. */
  @Test
  void exposesPrivateReadingWorkspaceForValidProduct() {
    var readiness =
        handler.readiness(
            process(),
            activity(PdePrivateReadingHumanActivityHandler.FIRST_READING),
            product(),
            "product:9@private-validation-v1");

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.workspaceCode()).isEqualTo("PDE_PRIVATE_READING");
    assertThat(readiness.workspaceReferenceId()).isEqualTo(9L);
    assertThat(readiness.requirements()).allMatch(requirement -> requirement.satisfied());
  }

  /** Conclui a leitura somente quando os cinco sinais atingem os critérios predeclarados. */
  @Test
  void acceptsCompleteObservedSignals() {
    var completion =
        handler.completeApproval(
            process(),
            activity(PdePrivateReadingHumanActivityHandler.FIRST_READING),
            product(),
            "product:9@private-validation-v1",
            decision("PV-A1B2C3D4E5F6", true, true));

    assertThat(completion.objectiveAchieved()).isTrue();
    assertThat(completion.structuredEvidence()).containsEntry("criteriaPassed", true);
  }

  /** Preserva a tentativa abaixo do gate como bloqueada para ajuste e nova leitura. */
  @Test
  void blocksObservedReadingWhenOneSignalFails() {
    var completion =
        handler.completeApproval(
            process(),
            activity(PdePrivateReadingHumanActivityHandler.FIRST_READING),
            product(),
            "product:9@private-validation-v1",
            decision("PV-A1B2C3D4E5F6", true, false));

    assertThat(completion.objectiveAchieved()).isFalse();
    assertThat(completion.structuredEvidence()).containsEntry("criteriaPassed", false);
    assertThat(completion.blockedReason()).contains("cinco sinais");
  }

  /** Rejeita a segunda leitura quando o código representa a mesma pessoa da primeira. */
  @Test
  void rejectsRepeatedParticipantInSecondReading() throws Exception {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition firstActivity =
        activity(PdePrivateReadingHumanActivityHandler.FIRST_READING);
    firstActivity.setProcessDefinition(process);
    BusinessProcessActivityInstance first = new BusinessProcessActivityInstance();
    first.setId(81L);
    first.setActivityDefinition(firstActivity);
    first.setSourceReference("product:9@private-validation-v1");
    first.setStatus("COMPLETED");
    first.setObjectiveAchieved(true);
    first.setObjectiveEvidenceJson(
        json.writeValueAsString(
            Map.of(
                "structuredEvidence",
                decision("PV-A1B2C3D4E5F6", true, true).structuredEvidence())));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                66L, "product:9@private-validation-v1"))
        .thenReturn(List.of(first));

    assertThatThrownBy(
            () ->
                handler.approve(
                    process,
                    activity(PdePrivateReadingHumanActivityHandler.SECOND_READING),
                    product(),
                    "product:9@private-validation-v1",
                    decision("PV-A1B2C3D4E5F6", true, true)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pessoa distinta");
  }

  /** Rejeita consentimento ausente antes de persistir qualquer leitura. */
  @Test
  void rejectsMissingConsent() {
    assertThatThrownBy(
            () ->
                handler.approve(
                    process(),
                    activity(PdePrivateReadingHumanActivityHandler.FIRST_READING),
                    product(),
                    "product:9@private-validation-v1",
                    decision("PV-A1B2C3D4E5F6", false, true)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("consentimento");
  }

  /** Impede nova leitura quando a fonte aceita para o protótipo já perdeu vigência. */
  @Test
  void blocksReadingWhenAcceptedSourceExpired() throws Exception {
    Product product = product();
    var definition =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(product.getValidationDefinitionJson());
    definition
        .with("privatePrototypeAcceptance")
        .put("sourceQualityEvaluatedAt", Instant.now().minusSeconds(40L * 86_400L).toString());
    product.setValidationDefinitionJson(json.writeValueAsString(definition));

    var readiness =
        handler.readiness(
            process(),
            activity(PdePrivateReadingHumanActivityHandler.FIRST_READING),
            product,
            "product:9@private-validation-v1");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.requirements())
        .filteredOn(requirement -> "PRIVATE_PROTOTYPE_ACCEPTED".equals(requirement.code()))
        .singleElement()
        .satisfies(requirement -> assertThat(requirement.satisfied()).isFalse());
  }

  /** Monta o produto planejado com o contrato canônico de duas leituras. */
  private Product product() {
    Instant now = Instant.now();
    return Product.builder()
        .id(9L)
        .validationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1")
        .validationDefinitionJson(
            """
            {"privateValidationPlan":{"minimumIndependentReadings":2,
              "minimumEligibleParticipantsPerReading":1,
              "criteriaDeclaredAt":"%s",
              "sourceMaxAgeDays":30,
              "minimumExperienceStartRate":1,
              "minimumValueMomentRate":1,
              "minimumReadyResultUseRate":1,
              "minimumPrototypePreferenceRate":1,
              "minimumCheckoutStartRate":1,
              "requiredSignals":["EXPERIENCE_STARTED","VALUE_MOMENT","READY_RESULT_USED",
                "PREFERRED_OVER_FREE","CHECKOUT_STARTED"]},
             "privatePrototype":{"checkoutMode":"SIMULATED_NO_CHARGE"},
             "privatePrototypeAcceptance":{
               "status":"READY","sourceQualityPassed":true,
               "sourceQualityEvaluatedAt":"%s","acceptedAt":"%s",
               "privateAccessUrl":"https://private.local/prototype",
               "prototypeVersion":"private-v1",
               "instrumentationReference":"events:local-01",
               "sourceEvidenceReference":"source-snapshot:local-01",
               "privateAccessConfirmed":true,
               "desktopValidated":true,"mobileValidated":true,
               "paymentEnabled":false,
               "published":false,"mediaSpendBrl":0,
               "eventSource":"FIRST_PARTY_EVENTS","testMarker":"PRIVATE_PROTOTYPE"
             }}
            """
                .formatted(now.minusSeconds(3600), now.minusSeconds(1800), now.minusSeconds(1200)))
        .build();
  }

  /** Monta a decisão estruturada sem dado pessoal em claro. */
  private ProductProcessActivityExecutionRequest decision(
      String participant, boolean consent, boolean checkoutStarted) {
    return new ProductProcessActivityExecutionRequest(
        "APPROVE",
        "Operador local",
        "Leitura observada integralmente no protótipo privado.",
        "private-session:local",
        "CONFIRM:pde-construction-approval:privateReading1",
        Map.of(
            "participantReference",
            participant,
            "consentConfirmed",
            consent,
            "firstPartyEvidenceConfirmed",
            true,
            "signals",
            Map.of(
                "EXPERIENCE_STARTED",
                true,
                "VALUE_MOMENT",
                true,
                "READY_RESULT_USED",
                true,
                "PREFERRED_OVER_FREE",
                true,
                "CHECKOUT_STARTED",
                checkoutStarted)));
  }

  /** Monta a versão publicada do processo de construção. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(66L);
    process.setProcessCode("pde-construction-approval");
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Monta uma das duas atividades humanas privadas. */
  private BusinessProcessActivityDefinition activity(String activityId) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(
        PdePrivateReadingHumanActivityHandler.FIRST_READING.equals(activityId) ? 601L : 602L);
    activity.setActivityId(activityId);
    activity.setOwnerName("Operador humano");
    return activity;
  }
}
