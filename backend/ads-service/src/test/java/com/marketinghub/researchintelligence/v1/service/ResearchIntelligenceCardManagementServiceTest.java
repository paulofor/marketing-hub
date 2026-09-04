package com.marketinghub.researchintelligence.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.repository.jpa.researchintelligence.ResearchIntelligenceCardRepository;
import com.marketinghub.repository.jpa.researchintelligence.ResearchIntelligenceCardVersionRepository;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCard;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardStatus;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardVersion;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceInternalRequestVerifier;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceSourceKind;
import com.marketinghub.researchintelligence.v1.service.managecard.RegisterResearchIntelligenceCardRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Valida versionamento, idempotência e substituição atômica da curadoria. */
class ResearchIntelligenceCardManagementServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-04T12:00:00Z"), ZoneOffset.UTC);
  private ResearchIntelligenceCardRepository cardRepository;
  private ResearchIntelligenceCardVersionRepository versionRepository;
  private ResearchIntelligenceCardManagementService service;

  /** Monta o serviço com repositories controlados e catálogo empacotado real. */
  @BeforeEach
  void setUp() {
    cardRepository = mock(ResearchIntelligenceCardRepository.class);
    versionRepository = mock(ResearchIntelligenceCardVersionRepository.class);
    ResearchIntelligenceService catalogService =
        new ResearchIntelligenceService(CLOCK, versionRepository);
    service =
        new ResearchIntelligenceCardManagementService(
            CLOCK,
            cardRepository,
            versionRepository,
            mock(ResearchIntelligenceInternalRequestVerifier.class),
            new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
            catalogService);
  }

  /** Cria a primeira versão como rascunho com identidade auditável. */
  @Test
  void shouldRegisterFirstVersionAsDraft() {
    ResearchIntelligenceCard root =
        new ResearchIntelligenceCard("homologacao-card", LocalDateTime.now(CLOCK));
    when(versionRepository.findByIdempotencyKey("idem-register-001")).thenReturn(Optional.empty());
    when(cardRepository.findByCardKeyForUpdate("homologacao-card")).thenReturn(Optional.of(root));
    when(versionRepository.findMaximumVersionNumber("homologacao-card"))
        .thenReturn(Optional.empty());
    when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.registerCard(request(), "codex-homologacao", "idem-register-001");

    assertThat(response.version()).isEqualTo(1);
    assertThat(response.status()).isEqualTo(ResearchIntelligenceCardStatus.DRAFT);
    assertThat(response.cardId()).matches("RI1-[0-9A-F]{12}");
    assertThat(response.routableAgents()).contains("videomaker");
    verify(cardRepository).insertIfMissing("homologacao-card", LocalDateTime.now(CLOCK));
  }

  /** Mantém a versão ativa anterior até a substituta ter sido revisada e ativada. */
  @Test
  void shouldArchivePriorActiveVersionWhenActivatingReplacement() {
    ResearchIntelligenceCard root =
        new ResearchIntelligenceCard("homologacao-card", LocalDateTime.now(CLOCK));
    ResearchIntelligenceCardVersion prior = version(1, "RI1-AAAAAAAAAAAA");
    prior.submitForReview("reviewer", "Revisado", LocalDateTime.now(CLOCK));
    prior.activate("reviewer", "Aprovado", LocalDateTime.now(CLOCK));
    ResearchIntelligenceCardVersion replacement = version(2, "RI1-BBBBBBBBBBBB");
    replacement.submitForReview("reviewer", "Revisado", LocalDateTime.now(CLOCK));
    when(cardRepository.findByCardKeyForUpdate("homologacao-card")).thenReturn(Optional.of(root));
    when(versionRepository.findVersionForUpdate("homologacao-card", 2))
        .thenReturn(Optional.of(replacement));
    when(versionRepository.findByCardKeyAndStatusForUpdate(
            "homologacao-card", ResearchIntelligenceCardStatus.ACTIVE))
        .thenReturn(List.of(prior));
    when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.activateCard("homologacao-card", 2, "reviewer", "Substituição aprovada");

    assertThat(response.status()).isEqualTo(ResearchIntelligenceCardStatus.ACTIVE);
    assertThat(prior.getStatus()).isEqualTo(ResearchIntelligenceCardStatus.ARCHIVED);
    assertThat(prior.getArchiveNote()).contains("versão 2");
  }

  /** Monta o contrato completo usado em registros válidos. */
  private RegisterResearchIntelligenceCardRequest request() {
    return new RegisterResearchIntelligenceCardRequest(
        "homologacao-card",
        "video",
        "Demonstração clara",
        "Mostrar reduz ambiguidade.",
        "Concretização visual.",
        "Comparar retenção e CTA.",
        "Hipótese externa.",
        LocalDate.of(2026, 9, 4),
        LocalDate.of(2026, 10, 19),
        "A demonstração aumentará CTA.",
        "Generalização.",
        "Pagamento comprova venda.",
        ResearchIntelligenceSourceKind.TEXT,
        "urn:test:card",
        "Fonte sintética",
        "a".repeat(64));
  }

  /** Cria uma entidade de versão pronta para transições nos testes. */
  private ResearchIntelligenceCardVersion version(int number, String cardId) {
    return new ResearchIntelligenceCardVersion(
        "homologacao-card",
        number,
        cardId,
        "video",
        "Demonstração clara",
        "Mostrar reduz ambiguidade.",
        "Concretização visual.",
        "Comparar retenção e CTA.",
        "Hipótese externa.",
        LocalDate.of(2026, 9, 4),
        LocalDate.of(2026, 10, 19),
        "A demonstração aumentará CTA.",
        "Generalização.",
        "Pagamento comprova venda.",
        ResearchIntelligenceSourceKind.TEXT,
        "urn:test:card",
        "Fonte sintética",
        "a".repeat(64),
        "idem-version-" + number,
        "b".repeat(64),
        "codex-homologacao",
        LocalDateTime.now(CLOCK));
  }
}
