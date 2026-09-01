package com.marketinghub.experiment.directcontact.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experimentdirectcontact.ExperimentDirectContactRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar a verdade e os limites da amostra individual consentida. */
class ExperimentDirectContactServiceTest {
  private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");
  private ExperimentRepository experiments;
  private ExperimentDirectContactRepository contacts;
  private ExperimentDirectContactService service;

  /** Prepara dependências isoladas e um relógio determinístico. */
  @BeforeEach
  void setUp() {
    experiments = mock(ExperimentRepository.class);
    contacts = mock(ExperimentDirectContactRepository.class);
    service =
        new ExperimentDirectContactService(experiments, contacts, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  /** Registra somente o identificador pseudonimizado e devolve o avanço real da amostra. */
  @Test
  void shouldRegisterAuditedContactWithoutRawIdentity() {
    Experiment experiment = directExperiment(15);
    when(contacts.findExperimentByIdForUpdate(89L)).thenReturn(Optional.of(experiment));
    when(experiments.findById(89L)).thenReturn(Optional.of(experiment));
    when(contacts.countByExperimentId(89L)).thenReturn(0L, 1L);
    when(contacts.existsByExperimentIdAndContactFingerprint(89L, "a".repeat(64))).thenReturn(false);
    doAnswer(
            invocation -> {
              ExperimentDirectContact saved = invocation.getArgument(0);
              saved.setId(701L);
              return saved;
            })
        .when(contacts)
        .saveAndFlush(any(ExperimentDirectContact.class));
    when(contacts.findByExperimentIdOrderByContactedAtAscIdAsc(89L)).thenReturn(List.of());

    ExperimentDirectContactSampleResponse response = service.register(89L, request("a".repeat(64)));

    ArgumentCaptor<ExperimentDirectContact> persisted =
        ArgumentCaptor.forClass(ExperimentDirectContact.class);
    verify(contacts).saveAndFlush(persisted.capture());
    assertThat(persisted.getValue().getContactFingerprint()).isEqualTo("a".repeat(64));
    assertThat(persisted.getValue().getConsentEvidenceReference())
        .isEqualTo("internal://consentimentos/rigel-001");
    assertThat(persisted.getValue().getCreatedAt()).isEqualTo(NOW);
    assertThat(response.recordedContacts()).isEqualTo(1);
    assertThat(response.remainingContacts()).isEqualTo(14);
    assertThat(response.readyForHermesReview()).isFalse();
  }

  /** Impede que chamadas concorrentes ultrapassem a meta comercial do piloto. */
  @Test
  void shouldRejectContactAfterTargetWasReached() {
    Experiment experiment = directExperiment(15);
    when(contacts.findExperimentByIdForUpdate(89L)).thenReturn(Optional.of(experiment));
    when(contacts.countByExperimentId(89L)).thenReturn(15L);

    assertThatThrownBy(() -> service.register(89L, request("b".repeat(64))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("já atingiu a meta");

    verify(contacts, never()).saveAndFlush(any());
  }

  /** Não contabiliza novamente a mesma pessoa dentro do experimento. */
  @Test
  void shouldRejectDuplicateFingerprint() {
    Experiment experiment = directExperiment(15);
    when(contacts.findExperimentByIdForUpdate(89L)).thenReturn(Optional.of(experiment));
    when(contacts.countByExperimentId(89L)).thenReturn(3L);
    when(contacts.existsByExperimentIdAndContactFingerprint(89L, "c".repeat(64))).thenReturn(true);

    assertThatThrownBy(() -> service.register(89L, request("c".repeat(64))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("já foi contabilizado");

    verify(contacts, never()).saveAndFlush(any());
  }

  /** Rejeita um registro cujo consentimento ocorreu depois da abordagem. */
  @Test
  void shouldRejectConsentAfterContact() {
    Experiment experiment = directExperiment(15);
    when(contacts.findExperimentByIdForUpdate(89L)).thenReturn(Optional.of(experiment));
    when(contacts.countByExperimentId(89L)).thenReturn(0L);
    RegisterExperimentDirectContactRequest invalid =
        new RegisterExperimentDirectContactRequest(
            "d".repeat(64),
            "internal://consentimentos/rigel-002",
            NOW.minusSeconds(30),
            NOW.minusSeconds(60),
            true,
            "Operador QA");

    assertThatThrownBy(() -> service.register(89L, invalid))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("consentimento precisa existir antes");

    verify(contacts, never()).saveAndFlush(any());
  }

  /** Mantém a amostra indisponível para experimentos de mídia paga. */
  @Test
  void shouldRejectSampleForPaidChannel() {
    Experiment paid =
        Experiment.builder().id(90L).platform(ExperimentPlatform.FACEBOOK).sampleSize(100).build();
    when(experiments.findById(90L)).thenReturn(Optional.of(paid));

    assertThatThrownBy(() -> service.getSample(90L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("DIRECT_ONE_TO_ONE");
  }

  /** Cria o experimento direto mínimo usado pelos cenários. */
  private Experiment directExperiment(int sampleSize) {
    return Experiment.builder()
        .id(89L)
        .platform(ExperimentPlatform.DIRECT_ONE_TO_ONE)
        .status(ExperimentStatus.RUNNING)
        .sampleSize(sampleSize)
        .build();
  }

  /** Cria um payload válido sem transportar telefone ou e-mail. */
  private RegisterExperimentDirectContactRequest request(String fingerprint) {
    return new RegisterExperimentDirectContactRequest(
        fingerprint,
        "internal://consentimentos/rigel-001",
        NOW.minusSeconds(120),
        NOW.minusSeconds(60),
        true,
        "Operador QA");
  }
}
