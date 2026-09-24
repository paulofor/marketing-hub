package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCustomerInterview;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryInterviewOutcome;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCustomerInterviewRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: comprovar o gate de comportamento passado da descoberta PDE. */
@ExtendWith(MockitoExtension.class)
class ProductDiscoveryCustomerInterviewServiceTest {

  @Mock private ProductDiscoveryCycleRepository cycleRepository;
  @Mock private ProductDiscoveryOpportunityRepository opportunityRepository;
  @Mock private ProductDiscoveryCustomerInterviewRepository interviewRepository;
  @Mock private ProductDiscoveryBpmAuditService bpmAuditService;

  private final List<ProductDiscoveryCustomerInterview> interviews = new ArrayList<>();
  private ProductDiscoveryCustomerInterviewService service;
  private ProductDiscoveryCycle cycle;
  private ProductDiscoveryOpportunity first;
  private ProductDiscoveryOpportunity second;

  /** Prepara duas candidatas do mesmo ciclo e uma persistência em memória controlada. */
  @BeforeEach
  void setUp() {
    cycle = new ProductDiscoveryCycle();
    cycle.setId(65L);
    cycle.setStatus(ProductDiscoveryCycleStatus.AWAITING_CUSTOMER_EVIDENCE);
    cycle.setStageCode(ProductDiscoveryCustomerInterviewService.WAITING_STAGE_CODE);
    first = opportunity(701L, "Preparação para ocasião especial");
    second = opportunity(702L, "Escolha de imagem para encontro");

    when(cycleRepository.findByIdForUpdate(65L)).thenReturn(Optional.of(cycle));
    when(bpmAuditService.supportsCandidateGapDeepening(cycle)).thenReturn(true);
    when(interviewRepository.findAllByCycleIdOrderByIdAsc(65L))
        .thenAnswer(invocation -> List.copyOf(interviews));
    service =
        new ProductDiscoveryCustomerInterviewService(
            cycleRepository, opportunityRepository, interviewRepository, bpmAuditService);
  }

  /** Adota a rota pública uma única vez e conserva as mesmas candidatas sem criar entrevistas. */
  @Test
  void adoptsPublicResearchWithoutRecreatingCandidatesOrInterviews() {
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(65L))
        .thenReturn(List.of(first, second));
    var response = service.adoptPublicEvidence(65L);
    assertThat(response.evidencePolicy()).isEqualTo("PUBLIC_SOURCES_V1");
    assertThat(response.readyForResearch()).isTrue();
    assertThat(response.interviewCount()).isZero();
    assertThat(response.minimumInterviews()).isZero();
    assertThat(response.canAdoptPublicEvidence()).isFalse();
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    assertThat(cycle.getStageCode()).isEqualTo("candidate-gap-deepening");
    assertThat(service.adoptPublicEvidence(65L).readyForResearch()).isTrue();
    org.mockito.Mockito.verify(cycleRepository, org.mockito.Mockito.times(1)).save(cycle);
    org.mockito.Mockito.verify(interviewRepository, org.mockito.Mockito.never()).save(any());
  }

  /** Recusa a alteração de política quando o worker já assumiu a pesquisa. */
  @Test
  void rejectsPolicyChangeOutsideTheWaitingGate() {
    org.mockito.Mockito.lenient().when(interviewRepository.findAllByCycleIdOrderByIdAsc(65L)).thenReturn(List.of());
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    assertThatThrownBy(() -> service.adoptPublicEvidence(65L))
        .hasMessageContaining("antes do aprofundamento");
    org.mockito.Mockito.verify(cycleRepository, org.mockito.Mockito.never()).save(any());
  }

  /** Deve liberar Argos somente com cinco relatos, compra, desistência e todas as candidatas. */
  @Test
  void releasesGapResearchOnlyWhenEveryBehavioralCriterionIsMet() {
    stubRecordPersistence();
    when(cycleRepository.save(any(ProductDiscoveryCycle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    service.record(65L, request(701L, "P01", ProductDiscoveryInterviewOutcome.PURCHASED));
    service.record(65L, request(702L, "P02", ProductDiscoveryInterviewOutcome.ABANDONED));
    service.record(65L, request(701L, "P03", ProductDiscoveryInterviewOutcome.ABANDONED));
    ProductDiscoveryGapDeepeningResponse before =
        service.record(65L, request(702L, "P04", ProductDiscoveryInterviewOutcome.PURCHASED));

    assertThat(before.readyForResearch()).isFalse();
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.AWAITING_CUSTOMER_EVIDENCE);

    ProductDiscoveryGapDeepeningResponse released =
        service.record(65L, request(701L, "P05", ProductDiscoveryInterviewOutcome.PURCHASED));

    assertThat(released.readyForResearch()).isTrue();
    assertThat(released.interviewCount()).isEqualTo(5);
    assertThat(released.missingOpportunityIds()).isEmpty();
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    assertThat(cycle.getStageCode())
        .isEqualTo(ProductDiscoveryCustomerInterviewService.GAP_STAGE_CODE);
  }

  /** Deve rejeitar contato pessoal no relato antes de gravar qualquer evidência. */
  @Test
  void rejectsContactDataInsideAnonymousNarrative() {
    stubDuplicateLookup();
    ProductDiscoveryCustomerInterviewRequest request =
        new ProductDiscoveryCustomerInterviewRequest(
            701L,
            "P01",
            ProductDiscoveryInterviewOutcome.ABANDONED,
            LocalDate.now().minusDays(5),
            "Precisava se preparar para um encontro.",
            "Queria sentir segurança.",
            "Pediu ajuda pelo contato pessoa@example.com.",
            "Tentou vídeos gratuitos.",
            null,
            null,
            "Ainda não sabia escolher.",
            true,
            true);

    assertThatThrownBy(() -> service.record(65L, request))
        .hasMessageContaining("Remova e-mail, telefone ou contato pessoal");
    assertThat(interviews).isEmpty();
  }

  /** Deve impedir que um código anônimo conte a mesma pessoa duas vezes no ciclo. */
  @Test
  void rejectsDuplicateAnonymousParticipantCode() {
    stubRecordPersistence();
    service.record(65L, request(701L, "P01", ProductDiscoveryInterviewOutcome.PURCHASED));

    assertThatThrownBy(
            () ->
                service.record(
                    65L, request(702L, "p01", ProductDiscoveryInterviewOutcome.ABANDONED)))
        .hasMessageContaining("já foi registrado");
    assertThat(interviews).hasSize(1);
  }

  /** Deve rejeitar uma situação futura porque a etapa investiga comportamento já ocorrido. */
  @Test
  void rejectsFuturePurchaseSituation() {
    stubDuplicateLookup();
    ProductDiscoveryCustomerInterviewRequest future =
        new ProductDiscoveryCustomerInterviewRequest(
            701L,
            "P01",
            ProductDiscoveryInterviewOutcome.ABANDONED,
            LocalDate.now().plusDays(1),
            "Pretendia se preparar para uma ocasião futura.",
            "Queria sentir segurança.",
            "Ainda não havia tomado uma decisão real.",
            "Considerava vídeos gratuitos.",
            null,
            null,
            "A situação ainda não ocorreu.",
            true,
            true);

    assertThatThrownBy(() -> service.record(65L, future))
        .hasMessageContaining("não pode estar no futuro");
    assertThat(interviews).isEmpty();
  }

  /** Deve encerrar a coleta exploratória no oitavo relato sem abrir amostra ilimitada. */
  @Test
  void rejectsInterviewAboveExploratoryLimit() {
    for (int index = 1;
        index <= ProductDiscoveryCustomerInterviewService.MAXIMUM_INTERVIEWS;
        index++) {
      ProductDiscoveryCustomerInterview interview = new ProductDiscoveryCustomerInterview();
      interview.setCycle(cycle);
      interview.setOpportunity(index % 2 == 0 ? first : second);
      interview.setAnonymousParticipantCode("P" + index);
      interviews.add(interview);
    }

    assertThatThrownBy(
            () ->
                service.record(
                    65L, request(701L, "P09", ProductDiscoveryInterviewOutcome.PURCHASED)))
        .hasMessageContaining("limite exploratório de oito entrevistas");
    assertThat(interviews).hasSize(ProductDiscoveryCustomerInterviewService.MAXIMUM_INTERVIEWS);
  }

  /** Deve impedir que uma entrevista de um ciclo seja vinculada à candidata de outro ciclo. */
  @Test
  void rejectsOpportunityFromAnotherCycle() {
    stubDuplicateLookup();
    when(opportunityRepository.findByIdAndCycleId(999L, 65L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.record(
                    65L, request(999L, "P01", ProductDiscoveryInterviewOutcome.PURCHASED)))
        .hasMessageContaining("não pertence a este ciclo");
    assertThat(interviews).isEmpty();
  }

  /** Configura somente a persistência necessária aos cenários que realmente gravam relatos. */
  private void stubRecordPersistence() {
    stubDuplicateLookup();
    when(opportunityRepository.findAllByCycleIdOrderByScoreDesc(65L))
        .thenReturn(List.of(first, second));
    when(opportunityRepository.findByIdAndCycleId(any(), any()))
        .thenAnswer(
            invocation ->
                Optional.of(Long.valueOf(701L).equals(invocation.getArgument(0)) ? first : second));
    when(interviewRepository.save(any(ProductDiscoveryCustomerInterview.class)))
        .thenAnswer(
            invocation -> {
              ProductDiscoveryCustomerInterview interview = invocation.getArgument(0);
              interview.setId((long) interviews.size() + 1);
              interview.prePersist();
              interviews.add(interview);
              return interview;
            });
  }

  /**
   * Simula a proteção contra participante duplicado somente nos cenários que chegam a essa etapa.
   */
  private void stubDuplicateLookup() {
    when(interviewRepository.existsByCycleIdAndAnonymousParticipantCodeIgnoreCase(any(), any()))
        .thenAnswer(
            invocation -> {
              String code = invocation.getArgument(1);
              return interviews.stream()
                  .anyMatch(item -> item.getAnonymousParticipantCode().equalsIgnoreCase(code));
            });
  }

  /** Cria uma candidata vinculada ao ciclo usado no teste. */
  private ProductDiscoveryOpportunity opportunity(Long id, String name) {
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    ReflectionTestUtils.setField(opportunity, "id", id);
    opportunity.setCycle(cycle);
    opportunity.setName(name);
    return opportunity;
  }

  /** Monta uma narrativa concreta sem inferir intenção futura ou dado pessoal. */
  private ProductDiscoveryCustomerInterviewRequest request(
      Long opportunityId, String code, ProductDiscoveryInterviewOutcome outcome) {
    return new ProductDiscoveryCustomerInterviewRequest(
        opportunityId,
        code,
        outcome,
        LocalDate.now().minusDays(10),
        "Tinha uma ocasião marcada para a semana seguinte.",
        "Queria escolher uma imagem coerente e sentir mais segurança.",
        "Não conseguia comparar as opções disponíveis.",
        "Tentou referências gratuitas e uma consultoria.",
        null,
        null,
        "Ainda precisava montar a decisão sozinha.",
        true,
        true);
  }
}
