package com.marketinghub.product.privatereading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import com.marketinghub.product.privatereading.infrastructure.PdePrivateReadingClient;
import com.marketinghub.product.privatereading.service.evidence.PrivateReadingEvidence;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir leitura falsa, mistura de QA e transcrição manual no gate de Mira. */
class PdePrivateReadingServiceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ProductRepository products = mock(ProductRepository.class);
  private final PdePrivateReadingClient client = mock(PdePrivateReadingClient.class);
  private final PdePrivateReadingService service =
      new PdePrivateReadingService(products, client, json, 10);
  private final String evidenceId = "44dd047c-9069-4c1e-a889-797e52c96574";

  /** Oferece o acesso aceito, mantendo o registro desabilitado antes da leitura humana. */
  @Test
  void showsAcceptedAccessWithoutFabricatingReading() {
    when(products.findById(10L)).thenReturn(Optional.of(product()));
    when(client.fetch(1)).thenReturn(evidence("NOT_STARTED", false, false));
    var workspace = service.workspace(10, "privateReading1");
    assertThat(workspace.prototypeUrl()).isEqualTo("https://v7.clubemusa.com.br/mira-private");
    assertThat(workspace.canRecord()).isFalse();
    assertThat(workspace.signals().values()).containsOnly(false);
    assertThat(workspace.evidenceId()).isNull();
  }

  /**
   * Usa exclusivamente os eventos reais mesmo se a tela enviar identidade e sinais falsificados.
   */
  @Test
  void rechecksEvidenceAndIgnoresClientSignals() {
    when(client.fetch(1)).thenReturn(evidence("PRIVATE_READING", true, false));
    var result =
        service.verifiedEvidence(
            product(),
            "privateReading1",
            Map.of(
                "evidenceId",
                evidenceId,
                "humanReadingConfirmed",
                true,
                "participantReference",
                "PV-FFFFFFFFFFFF",
                "signals",
                Map.of("CHECKOUT_STARTED", true)));
    assertThat(result).containsEntry("participantReference", "PV-000000000001");
    assertThat((Map<?, ?>) result.get("signals")).isEqualTo(signals(true, false));
    verify(client).fetch(1);
  }

  /** Impede que o teste interno ou a ausência de consentimento satisfaçam o gate humano. */
  @Test
  void rejectsQaAndMissingHumanConfirmation() {
    assertThatThrownBy(() -> service.verifiedEvidence(product(), "privateReading1", Map.of()))
        .hasMessageContaining("pessoa real");
    verifyNoInteractions(client);
    when(client.fetch(1)).thenReturn(evidence("QA_INTERNAL", true, true));
    assertThatThrownBy(() -> service.verifiedEvidence(product(), "privateReading1", confirmation()))
        .hasMessageContaining("não corresponde");
  }

  /** Rejeita leitura em andamento e prova diferente daquela que o operador revisou. */
  @Test
  void rejectsUnfinishedOrChangedEvidence() {
    when(client.fetch(1)).thenReturn(evidence("PRIVATE_READING", false, true));
    assertThatThrownBy(() -> service.verifiedEvidence(product(), "privateReading1", confirmation()))
        .hasMessageContaining("não encerrou");
    when(client.fetch(1)).thenReturn(evidence("PRIVATE_READING", true, true));
    assertThatThrownBy(
            () ->
                service.verifiedEvidence(
                    product(),
                    "privateReading1",
                    Map.of("humanReadingConfirmed", true, "evidenceId", "old-proof")))
        .hasMessageContaining("evidência mudou");
  }

  /** Mantém falha de integração como erro explícito sem converter em sinal ausente ou sucesso. */
  @Test
  void failsClosedWhenPdeIsUnavailable() {
    when(client.fetch(1)).thenThrow(new IllegalStateException("PDE indisponível"));
    assertThatThrownBy(() -> service.verifiedEvidence(product(), "privateReading1", confirmation()))
        .hasMessageContaining("indisponível");
  }

  /** Restringe os sinais à identidade canônica do produto e ao participante da atividade. */
  @Test
  void rejectsAnotherProductAndRepeatedSlot() {
    Product other = product();
    other.setId(11L);
    assertThatThrownBy(() -> service.verifiedEvidence(other, "privateReading1", confirmation()))
        .hasMessageContaining("não possui");
    verifyNoInteractions(client);
    when(client.fetch(2)).thenReturn(evidence("PRIVATE_READING", true, true));
    assertThatThrownBy(() -> service.verifiedEvidence(product(), "privateReading2", confirmation()))
        .hasMessageContaining("não corresponde");
  }

  /**
   * Uma evolução de versão de Mira deve bloquear até adaptação, sem voltar ao formulário manual.
   */
  @Test
  void keepsMiraAssistedWhenAcceptedVersionChanges() {
    Product changed = product();
    changed.setValidationDefinitionJson(
        changed.getValidationDefinitionJson().replace("mira-private-v1", "mira-private-v2"));
    assertThat(service.supports(changed)).isTrue();
    assertThatThrownBy(() -> service.verifiedEvidence(changed, "privateReading1", confirmation()))
        .hasMessageContaining("não possui");
    verifyNoInteractions(client);
  }

  /** Rejeita uma prova de versão diferente e fronteira financeira incompatível. */
  @Test
  void rejectsWrongVersionOrEnabledPayment() {
    var valid = evidence("PRIVATE_READING", true, true);
    when(client.fetch(1))
        .thenReturn(
            new PrivateReadingEvidence(
                valid.productSlug(),
                "old-v1",
                valid.participantReference(),
                valid.trafficClass(),
                evidenceId,
                valid.consentedAt(),
                valid.finishedAt(),
                null,
                valid.signals(),
                valid.checkoutMode(),
                false,
                false,
                0));
    assertThatThrownBy(() -> service.verifiedEvidence(product(), "privateReading1", confirmation()))
        .hasMessageContaining("não corresponde");
    when(client.fetch(1))
        .thenReturn(
            new PrivateReadingEvidence(
                valid.productSlug(),
                valid.prototypeVersion(),
                valid.participantReference(),
                valid.trafficClass(),
                evidenceId,
                valid.consentedAt(),
                valid.finishedAt(),
                null,
                valid.signals(),
                valid.checkoutMode(),
                true,
                false,
                0));
    assertThatThrownBy(() -> service.verifiedEvidence(product(), "privateReading1", confirmation()))
        .hasMessageContaining("não corresponde");
  }

  /** Uma negativa encerrada pode ser registrada, permanecendo negativa para avaliação do gate. */
  @Test
  void preservesNegativeFinishedReading() {
    when(products.findById(10L)).thenReturn(Optional.of(product()));
    when(client.fetch(1)).thenReturn(evidence("PRIVATE_READING", true, false));
    var workspace = service.workspace(10, "privateReading1");
    assertThat(workspace.canRecord()).isTrue();
    assertThat(workspace.guidance()).contains("bloqueado para ajuste");
    assertThat(workspace.signals()).containsEntry("CHECKOUT_STARTED", false);
  }

  /** Monta confirmação humana explícita sem enviar sinais ou dados pessoais. */
  private Map<String, Object> confirmation() {
    return Map.of("humanReadingConfirmed", true, "evidenceId", evidenceId);
  }

  /** Monta a versão aceita de Mira com endereço privado sem segredo. */
  private Product product() {
    return Product.builder()
        .id(10L)
        .commercialStatus("PLANNED")
        .validationDefinitionJson(
            """
        {"privatePrototypeAcceptance":{"prototypeVersion":"mira-private-v1","status":"READY",
        "acceptedAt":"2026-09-05T00:00:00Z","privateAccessUrl":"https://v7.clubemusa.com.br/mira-private"}}
        """)
        .build();
  }

  /** Constrói uma resposta interna sanitizada ou a ausência legítima de leitura. */
  private PrivateReadingEvidence evidence(String trafficClass, boolean finished, boolean checkout) {
    boolean started = !"NOT_STARTED".equals(trafficClass);
    return new PrivateReadingEvidence(
        "mira-private-validation",
        "mira-private-v1",
        "PV-000000000001",
        trafficClass,
        started ? evidenceId : null,
        started ? Instant.now().minusSeconds(60).toString() : null,
        finished ? Instant.now().toString() : null,
        null,
        signals(started, started && checkout),
        "SIMULATED_NO_CHARGE",
        false,
        false,
        0);
  }

  /** Representa os cinco sinais canônicos sem omitir uma resposta negativa. */
  private Map<String, Boolean> signals(boolean started, boolean checkout) {
    return Map.of(
        "EXPERIENCE_STARTED",
        started,
        "VALUE_MOMENT",
        started,
        "READY_RESULT_USED",
        started,
        "PREFERRED_OVER_FREE",
        started,
        "CHECKOUT_STARTED",
        checkout);
  }
}
