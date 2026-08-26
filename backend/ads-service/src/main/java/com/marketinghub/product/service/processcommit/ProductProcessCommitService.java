package com.marketinghub.product.service.processcommit;

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
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar o registro auditável de commits por produto e processo. */
@Service
public class ProductProcessCommitService {
  private final ProductRepository productRepository;
  private final BusinessProcessDefinitionRepository processRepository;
  private final ProductProcessCommitRepository commitRepository;
  private final ProductValueChainPositionService positionService;
  private final Clock clock;

  /** Configura as fontes canônicas e o relógio usados no registro de commits. */
  @Autowired
  public ProductProcessCommitService(
      ProductRepository productRepository,
      BusinessProcessDefinitionRepository processRepository,
      ProductProcessCommitRepository commitRepository,
      ProductValueChainPositionService positionService) {
    this(
        productRepository, processRepository, commitRepository, positionService, Clock.systemUTC());
  }

  /** Permite validar os marcos de auditoria com um relógio determinístico. */
  ProductProcessCommitService(
      ProductRepository productRepository,
      BusinessProcessDefinitionRepository processRepository,
      ProductProcessCommitRepository commitRepository,
      ProductValueChainPositionService positionService,
      Clock clock) {
    this.productRepository = productRepository;
    this.processRepository = processRepository;
    this.commitRepository = commitRepository;
    this.positionService = positionService;
    this.clock = clock;
  }

  /** Lista somente os commits explicitamente vinculados ao produto solicitado. */
  @Transactional(readOnly = true)
  public List<ProductProcessCommitResponse> list(Long productId) {
    requireProduct(productId);
    return commitRepository.findByProductIdOrderByRecordedAtDescIdDesc(productId).stream()
        .map(this::response)
        .toList();
  }

  /** Retorna um vínculo específico sem permitir acesso por outro produto. */
  @Transactional(readOnly = true)
  public ProductProcessCommitResponse get(Long productId, Long commitId) {
    requireProduct(productId);
    return commitRepository
        .findByIdAndProductId(commitId, productId)
        .map(this::response)
        .orElseThrow(
            () -> new EntityNotFoundException("Commit de produto não encontrado: " + commitId));
  }

  /** Registra o vínculo ou devolve o registro existente quando a mesma evidência for repetida. */
  @Transactional
  public ProductProcessCommitRegistrationResult register(
      Long productId, RegisterProductProcessCommitRequest request) {
    Product product = requireProduct(productId);
    BusinessProcessDefinition process =
        processRepository
            .findById(request.processDefinitionId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Processo não encontrado: " + request.processDefinitionId()));
    ensureKnownProductProcess(product, process);

    commitRepository.lockProductForCommitRegistration(productId);
    String repositoryName = request.repositoryName().trim().toLowerCase(Locale.ROOT);
    String commitSha = request.commitSha().trim().toLowerCase(Locale.ROOT);
    var existing =
        commitRepository.findByProductIdAndProcessDefinitionIdAndRepositoryNameAndCommitSha(
            productId, process.getId(), repositoryName, commitSha);
    if (existing.isPresent()) {
      return new ProductProcessCommitRegistrationResult(response(existing.get()), false);
    }

    ProductProcessCommit commit = new ProductProcessCommit();
    commit.setProduct(product);
    commit.setProcessDefinition(process);
    commit.setRepositoryName(repositoryName);
    commit.setCommitSha(commitSha);
    commit.setCommitSummary(request.commitSummary().trim());
    commit.setCommitUrl(trimToNull(request.commitUrl()));
    commit.setRecordedBy(request.recordedBy().trim());
    commit.setRecordedAt(Instant.now(clock));
    return new ProductProcessCommitRegistrationResult(
        response(commitRepository.saveAndFlush(commit)), true);
  }

  /** Impede atribuir um commit a um processo ausente do histórico conhecido do produto. */
  private void ensureKnownProductProcess(Product product, BusinessProcessDefinition process) {
    ProductValueChainPositionResponse position = positionService.getPosition(product.getId());
    Set<Long> knownProcessIds = new HashSet<>();
    if (position.processDefinitionId() != null) {
      knownProcessIds.add(position.processDefinitionId());
    }
    position.processMeasurements().stream()
        .map(ProductStageMeasurementResponse::processDefinitionId)
        .forEach(knownProcessIds::add);
    if (position.subprocessPosition() != null) {
      if (position.subprocessPosition().currentSubprocessDefinitionId() != null) {
        knownProcessIds.add(position.subprocessPosition().currentSubprocessDefinitionId());
      }
      position.subprocessPosition().measurements().stream()
          .map(ProductStageMeasurementResponse::processDefinitionId)
          .forEach(knownProcessIds::add);
    }
    if (!knownProcessIds.contains(process.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O processo informado não pertence ao histórico conhecido deste produto.");
    }
  }

  /** Exige um produto persistido antes de consultar ou registrar evidências. */
  private Product requireProduct(Long productId) {
    return productRepository
        .findById(productId)
        .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado: " + productId));
  }

  /** Converte a entidade no contrato estável apresentado à tela. */
  private ProductProcessCommitResponse response(ProductProcessCommit commit) {
    BusinessProcessDefinition process = commit.getProcessDefinition();
    return new ProductProcessCommitResponse(
        commit.getId(),
        commit.getProduct().getId(),
        process.getId(),
        process.getProcessCode(),
        process.getName(),
        process.getVersionNumber(),
        commit.getRepositoryName(),
        commit.getCommitSha(),
        commit.getCommitSummary(),
        commit.getCommitUrl(),
        commit.getRecordedBy(),
        commit.getRecordedAt());
  }

  /** Normaliza campos opcionais vazios sem fabricar informação. */
  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
