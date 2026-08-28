package com.marketinghub.product.service.processcommit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductProcessCommit;
import com.marketinghub.product.service.valuechainposition.ProductStageMeasurementResponse;
import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionResponse;
import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionService;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductProcessCommitRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar a segregação e idempotência dos commits por produto e processo. */
class ProductProcessCommitServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-26T12:30:00Z");
  private static final String SHA = "a".repeat(40);

  private final ProductRepository productRepository = mock(ProductRepository.class);
  private final BusinessProcessDefinitionRepository processRepository =
      mock(BusinessProcessDefinitionRepository.class);
  private final ProductProcessCommitRepository commitRepository =
      mock(ProductProcessCommitRepository.class);
  private final ProductValueChainPositionService positionService =
      mock(ProductValueChainPositionService.class);
  private final ProductProcessCommitService service =
      new ProductProcessCommitService(
          productRepository,
          processRepository,
          commitRepository,
          positionService,
          Clock.fixed(NOW, ZoneOffset.UTC));

  private Product product;
  private BusinessProcessDefinition process;

  /** Prepara um produto e um processo conhecidos para cada cenário. */
  @BeforeEach
  void setUp() {
    product = new Product();
    product.setId(9L);
    product.setName("Kit WhatsApp Pronto");
    process = new BusinessProcessDefinition();
    process.setId(43L);
    process.setProcessCode("pde-communication-sales-journey");
    process.setName("Comunicação e jornada de venda do PDE");
    process.setVersionNumber(4);
    when(productRepository.findById(9L)).thenReturn(Optional.of(product));
    when(processRepository.findById(43L)).thenReturn(Optional.of(process));
    when(positionService.getPosition(9L)).thenReturn(position(43L));
  }

  /** Registra um SHA completo normalizado contra a versão exata do processo. */
  @Test
  void registersCommitForKnownProductProcess() {
    when(commitRepository.findByProductIdAndProcessDefinitionIdAndRepositoryNameAndCommitSha(
            9L, 43L, "paulofor/marketing-hub", SHA))
        .thenReturn(Optional.empty());
    when(commitRepository.saveAndFlush(any(ProductProcessCommit.class)))
        .thenAnswer(
            invocation -> {
              ProductProcessCommit saved = invocation.getArgument(0);
              saved.setId(71L);
              return saved;
            });

    var result = service.register(9L, request(SHA.toUpperCase()));

    assertThat(result.created()).isTrue();
    assertThat(result.commit().id()).isEqualTo(71L);
    assertThat(result.commit().productId()).isEqualTo(9L);
    assertThat(result.commit().processDefinitionId()).isEqualTo(43L);
    assertThat(result.commit().processVersion()).isEqualTo(4);
    assertThat(result.commit().commitSha()).isEqualTo(SHA);
    assertThat(result.commit().recordedAt()).isEqualTo(NOW);
    verify(commitRepository).lockProductForCommitRegistration(9L);
  }

  /** Devolve o vínculo existente sem inserir uma segunda linha para o mesmo commit. */
  @Test
  void keepsDuplicateRegistrationIdempotent() {
    ProductProcessCommit existing = commit(71L, SHA);
    when(commitRepository.findByProductIdAndProcessDefinitionIdAndRepositoryNameAndCommitSha(
            9L, 43L, "paulofor/marketing-hub", SHA))
        .thenReturn(Optional.of(existing));

    var result = service.register(9L, request(SHA));

    assertThat(result.created()).isFalse();
    assertThat(result.commit().id()).isEqualTo(71L);
    verify(commitRepository, never()).saveAndFlush(any());
  }

  /** Rejeita um processo que não pertence ao histórico conhecido do produto. */
  @Test
  void rejectsProcessOutsideProductHistory() {
    when(positionService.getPosition(9L)).thenReturn(position(99L));

    assertThatThrownBy(() -> service.register(9L, request(SHA)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não pertence ao histórico conhecido");
    verify(commitRepository, never()).saveAndFlush(any());
  }

  /** Rejeita commit em processo futuro exibido apenas para completar a cadeia visual. */
  @Test
  void rejectsCommitForPlannedFutureProcess() {
    when(positionService.getPosition(9L)).thenReturn(positionWithPlannedProcess(99L, 43L));

    assertThatThrownBy(() -> service.register(9L, request(SHA)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não pertence ao histórico conhecido");
    verify(commitRepository, never()).saveAndFlush(any());
  }

  /** Não lista commits de uma identidade de produto inexistente. */
  @Test
  void rejectsMissingProductOnList() {
    when(productRepository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.list(404L)).isInstanceOf(EntityNotFoundException.class);
    verify(commitRepository, never()).findByProductIdOrderByRecordedAtDescIdDesc(404L);
  }

  /** Monta um pedido válido para os cenários de persistência. */
  private RegisterProductProcessCommitRequest request(String sha) {
    return new RegisterProductProcessCommitRequest(
        43L,
        " paulofor/marketing-hub ",
        sha,
        " Registra commits no histórico do produto ",
        "https://github.com/paulofor/marketing-hub/commit/" + SHA,
        " time@marketinghub.io ");
  }

  /** Monta um registro persistido para validar repetição idempotente. */
  private ProductProcessCommit commit(Long id, String sha) {
    ProductProcessCommit commit = new ProductProcessCommit();
    commit.setId(id);
    commit.setProduct(product);
    commit.setProcessDefinition(process);
    commit.setRepositoryName("paulofor/marketing-hub");
    commit.setCommitSha(sha);
    commit.setCommitSummary("Registra commits no histórico do produto");
    commit.setCommitUrl("https://github.com/paulofor/marketing-hub/commit/" + sha);
    commit.setRecordedBy("time@marketinghub.io");
    commit.setRecordedAt(NOW);
    return commit;
  }

  /** Monta a posição mínima que autoriza somente o processo indicado. */
  private ProductValueChainPositionResponse position(Long processDefinitionId) {
    return new ProductValueChainPositionResponse(
        9L,
        "COMUNICACAO_E_JORNADA",
        "IDENTIFIED",
        "Posição identificada.",
        5L,
        "Cadeia PDE",
        5,
        processDefinitionId,
        "process-code",
        "Processo conhecido",
        4,
        4,
        6,
        List.of(),
        null);
  }

  /** Monta uma posição cuja cadeia contém outro processo ainda sem execução autorizada. */
  private ProductValueChainPositionResponse positionWithPlannedProcess(
      Long currentProcessDefinitionId, Long plannedProcessDefinitionId) {
    ProductStageMeasurementResponse planned =
        new ProductStageMeasurementResponse(
            "PROCESS",
            "5",
            "PLANNED",
            plannedProcessDefinitionId,
            "planned-process",
            "Processo futuro",
            null,
            "NOT_RECORDED",
            null,
            null,
            false,
            null,
            BigDecimal.ZERO,
            "NO_EXECUTIONS",
            0,
            0,
            false);
    ProductValueChainPositionResponse current = position(currentProcessDefinitionId);
    return new ProductValueChainPositionResponse(
        current.productId(),
        current.commercialStatus(),
        current.resolutionStatus(),
        current.resolutionMessage(),
        current.chainDefinitionId(),
        current.chainName(),
        current.chainVersion(),
        current.processDefinitionId(),
        current.processCode(),
        current.processName(),
        current.processVersion(),
        current.sequenceNumber(),
        current.processCount(),
        List.of(planned),
        null);
  }
}
